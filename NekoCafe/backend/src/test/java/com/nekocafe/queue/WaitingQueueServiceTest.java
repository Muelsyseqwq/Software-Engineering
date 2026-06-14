package com.nekocafe.queue;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.queue.entity.WaitingQueueCounter;
import com.nekocafe.queue.entity.WaitingQueueTicket;
import com.nekocafe.queue.mapper.WaitingQueueCounterMapper;
import com.nekocafe.queue.mapper.WaitingQueueTicketMapper;
import com.nekocafe.queue.service.WaitingQueueService;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.entity.UserStoreRole;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.store.mapper.UserStoreRoleMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WaitingQueueServiceTest {

    private final WaitingQueueCounterMapper counterMapper = mock(WaitingQueueCounterMapper.class);
    private final WaitingQueueTicketMapper ticketMapper = mock(WaitingQueueTicketMapper.class);
    private final StoreMapper storeMapper = mock(StoreMapper.class);
    private final DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
    private final UserStoreRoleMapper userStoreRoleMapper = mock(UserStoreRoleMapper.class);
    private final WaitingQueueService service = new WaitingQueueService(
        counterMapper, ticketMapper, storeMapper, diningTableMapper, userStoreRoleMapper);

    /**
     * Pure-Mockito unit tests run without a Spring context, so MyBatis-Plus does not
     * auto-initialize its lambda column cache.  That cache is needed whenever the
     * service creates a {@code LambdaUpdateWrapper} with method references like
     * {@code WaitingQueueTicket::getStatus}.  We manually seed a minimal MyBatis
     * {@link Configuration} so that {@link TableInfoHelper} can build the required
     * {@code TableInfo} for the entity class.
     */
    @BeforeAll
    static void initMybatisLambdaCache() {
        Configuration config = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, WaitingQueueTicket.class);
    }

    // ---- applyQueue ----

    @Test
    void applyQueueSuccess() {
        // Given: store is OPEN, no available tables (must queue), no existing active ticket
        Store store = store(1L, "OPEN");
        WaitingQueueCounter counter = counter(1L, LocalDate.now(), 0, 1, 0);

        when(storeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(store);
        when(diningTableMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doNothing().when(counterMapper).ensureCounter(anyLong(), any(LocalDate.class));
        when(counterMapper.selectForUpdate(anyLong(), any(LocalDate.class))).thenReturn(counter);
        // findActiveUserTicket returns null → no existing active ticket
        when(ticketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(ticketMapper.insert(any(WaitingQueueTicket.class))).thenReturn(1);
        when(counterMapper.updateById(any(WaitingQueueCounter.class))).thenReturn(1);

        // When
        var req = new WaitingQueueService.ApplyQueueRequest(1L, 2, "张三", "13800000001");
        WaitingQueueService.QueueTicketResponse resp = service.apply(1L, req);

        // Then: ticket is WAITING with the next counter number (1)
        assertThat(resp.status()).isEqualTo("WAITING");
        assertThat(resp.queueNumber()).isEqualTo(1);
        verify(ticketMapper).insert(any(WaitingQueueTicket.class));
    }

    @Test
    void applyQueueDuplicateFails() {
        // Given: user already has an active WAITING ticket
        Store store = store(1L, "OPEN");
        WaitingQueueCounter counter = counter(1L, LocalDate.now(), 0, 1, 0);
        WaitingQueueTicket active = ticket(1L, 1L, 1L, 1, "WAITING");

        when(storeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(store);
        when(diningTableMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doNothing().when(counterMapper).ensureCounter(anyLong(), any(LocalDate.class));
        when(counterMapper.selectForUpdate(anyLong(), any(LocalDate.class))).thenReturn(counter);
        // findActiveUserTicket returns an existing active ticket
        when(ticketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(active);

        // When / Then: duplicate application throws BizException
        var req = new WaitingQueueService.ApplyQueueRequest(1L, 2, "张三", "13800000001");
        assertThatThrownBy(() -> service.apply(1L, req))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("已有进行中的排队号码");
    }

    // ---- callNext ----

    @Test
    void callNextRequiresStaffStorePermission() {
        // Given: staff has no valid store assignment → selectCount returns 0
        WaitingQueueCounter counter = counter(1L, LocalDate.now(), 0, 5, 0);

        when(userStoreRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doNothing().when(counterMapper).ensureCounter(anyLong(), any(LocalDate.class));
        when(counterMapper.selectForUpdate(anyLong(), any(LocalDate.class))).thenReturn(counter);

        // When / Then: permission check fails before reaching LambdaUpdateWrapper
        assertThatThrownBy(() -> service.callNext(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("无权操作");
    }

    @Test
    void callNextMovesToCalled() {
        // Given: staff can operate store, counter exists, next WAITING ticket exists
        WaitingQueueCounter counter = counter(1L, LocalDate.now(), 0, 5, 0);
        WaitingQueueTicket next = ticket(10L, 1L, 1L, 5, "WAITING");

        // ensureStaffCanOperateStore passes (called twice: in callNext and staffStatus)
        when(userStoreRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        // lockCounter
        doNothing().when(counterMapper).ensureCounter(anyLong(), any(LocalDate.class));
        when(counterMapper.selectForUpdate(anyLong(), any(LocalDate.class))).thenReturn(counter);
        // expire any existing CALLED tickets (uses LambdaUpdateWrapper internally)
        when(ticketMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        // find the next WAITING ticket
        when(ticketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(next);
        when(ticketMapper.updateById(any(WaitingQueueTicket.class))).thenReturn(1);
        when(counterMapper.updateById(any(WaitingQueueCounter.class))).thenReturn(1);
        // staffStatus after callNext: findCounter, findCalledTicket, activeTickets, countWaiting
        when(counterMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(counter);
        when(ticketMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(next));
        when(ticketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        WaitingQueueService.StaffQueueStatusResponse resp = service.callNext(1L, 1L);

        // Then: the next ticket was updated and calledTicket is present
        verify(ticketMapper).updateById(any(WaitingQueueTicket.class));
        assertThat(resp.calledTicket()).isNotNull();
    }

    // ---- markSeated ----

    @Test
    void markSeatedSuccess() {
        // Given: a CALLED ticket and staff who can operate the store
        WaitingQueueTicket ticket = ticket(1L, 1L, 1L, 3, "CALLED");
        when(ticketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ticket);
        when(userStoreRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(ticketMapper.updateById(any(WaitingQueueTicket.class))).thenReturn(1);

        // When
        WaitingQueueService.QueueTicketResponse resp = service.markSeated(1L, 1L);

        // Then: status transitions to SEATED
        assertThat(resp.status()).isEqualTo("SEATED");
    }

    // ---- cancel ----

    @Test
    void cancelQueueSuccess() {
        // Given: a WAITING ticket belonging to the calling user
        WaitingQueueTicket ticket = ticket(1L, 1L, 1L, 3, "WAITING");
        when(ticketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ticket);
        when(ticketMapper.updateById(any(WaitingQueueTicket.class))).thenReturn(1);

        // When
        WaitingQueueService.QueueTicketResponse resp = service.cancel(1L, 1L);

        // Then: status transitions to CANCELLED
        assertThat(resp.status()).isEqualTo("CANCELLED");
    }

    // ---- helper factory methods ----

    private static Store store(Long id, String status) {
        Store s = new Store();
        s.setId(id);
        s.setName("测试猫咖");
        s.setStatus(status);
        s.setOpeningTime(LocalTime.of(10, 0));
        s.setClosingTime(LocalTime.of(22, 0));
        s.setDeleted(0);
        return s;
    }

    private static WaitingQueueCounter counter(Long storeId, LocalDate date,
                                                int currentNumber, int nextNumber, int resetVersion) {
        WaitingQueueCounter c = new WaitingQueueCounter();
        c.setId(1L);
        c.setStoreId(storeId);
        c.setQueueDate(date);
        c.setCurrentNumber(currentNumber);
        c.setNextNumber(nextNumber);
        c.setResetVersion(resetVersion);
        c.setDeleted(0);
        return c;
    }

    private static WaitingQueueTicket ticket(Long id, Long storeId, Long userId,
                                              int queueNumber, String status) {
        WaitingQueueTicket t = new WaitingQueueTicket();
        t.setId(id);
        t.setStoreId(storeId);
        t.setUserId(userId);
        t.setQueueDate(LocalDate.now());
        t.setQueueNumber(queueNumber);
        t.setResetVersion(0);
        t.setPartySize(2);
        t.setStatus(status);
        t.setContactName("张三");
        t.setContactPhone("13800000001");
        t.setDeleted(0);
        return t;
    }
}
