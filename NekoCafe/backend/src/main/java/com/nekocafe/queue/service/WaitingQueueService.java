package com.nekocafe.queue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.queue.entity.WaitingQueueCounter;
import com.nekocafe.queue.entity.WaitingQueueTicket;
import com.nekocafe.queue.mapper.WaitingQueueCounterMapper;
import com.nekocafe.queue.mapper.WaitingQueueTicketMapper;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.DiningTableStatusLog;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.entity.UserStoreRole;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.DiningTableStatusLogMapper;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.store.mapper.UserStoreRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WaitingQueueService {

    private static final String OPEN = "OPEN";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String OCCUPIED = "OCCUPIED";
    private static final String STAFF = "STAFF";
    private static final String ACTIVE = "ACTIVE";

    private static final String WAITING = "WAITING";
    private static final String CALLED = "CALLED";
    private static final String SEATED = "SEATED";
    private static final String EXPIRED = "EXPIRED";
    private static final String CANCELLED = "CANCELLED";

    private final WaitingQueueCounterMapper counterMapper;
    private final WaitingQueueTicketMapper ticketMapper;
    private final StoreMapper storeMapper;
    private final DiningTableMapper diningTableMapper;
    private final DiningTableStatusLogMapper diningTableStatusLogMapper;
    private final UserStoreRoleMapper userStoreRoleMapper;

    public WaitingQueueService(WaitingQueueCounterMapper counterMapper,
                               WaitingQueueTicketMapper ticketMapper,
                               StoreMapper storeMapper,
                               DiningTableMapper diningTableMapper,
                               DiningTableStatusLogMapper diningTableStatusLogMapper,
                               UserStoreRoleMapper userStoreRoleMapper) {
        this.counterMapper = counterMapper;
        this.ticketMapper = ticketMapper;
        this.storeMapper = storeMapper;
        this.diningTableMapper = diningTableMapper;
        this.diningTableStatusLogMapper = diningTableStatusLogMapper;
        this.userStoreRoleMapper = userStoreRoleMapper;
    }

    @Transactional
    public QueueTicketResponse apply(Long userId, ApplyQueueRequest request) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        validateApplyRequest(request);
        Store store = ensureStoreOpen(request.storeId());
        if (hasSuitableAvailableTable(store.getId(), request.partySize())) {
            throw new BizException(6103, "当前仍有可用桌位，无需排队");
        }

        LocalDate today = LocalDate.now();
        WaitingQueueCounter counter = lockCounter(store.getId(), today);
        WaitingQueueTicket active = findActiveUserTicket(userId, store.getId(), today, counter.getResetVersion());
        if (active != null) {
            throw new BizException(6104, "你已有进行中的排队号码");
        }

        int nextNumber = counter.getNextNumber() == null ? 1 : counter.getNextNumber();
        WaitingQueueTicket ticket = new WaitingQueueTicket();
        ticket.setStoreId(store.getId());
        ticket.setUserId(userId);
        ticket.setQueueDate(today);
        ticket.setQueueNumber(nextNumber);
        ticket.setResetVersion(normalizeVersion(counter.getResetVersion()));
        ticket.setPartySize(request.partySize());
        ticket.setStatus(WAITING);
        ticket.setContactName(request.contactName().trim());
        ticket.setContactPhone(request.contactPhone().trim());
        ticketMapper.insert(ticket);

        counter.setNextNumber(nextNumber + 1);
        counterMapper.updateById(counter);
        return toTicketResponse(ticket);
    }

    public QueueStatusResponse customerStatus(Long userId, Long storeId, Integer partySize) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        if (storeId == null) {
            throw new BizException(6101, "请选择门店");
        }
        Store store = ensureStoreExists(storeId);
        LocalDate today = LocalDate.now();
        WaitingQueueCounter counter = findCounter(store.getId(), today);
        int resetVersion = counter == null ? 0 : normalizeVersion(counter.getResetVersion());
        WaitingQueueTicket myTicket = findActiveUserTicket(userId, store.getId(), today, resetVersion);
        if (myTicket == null) {
            myTicket = findLatestUserTicket(userId, store.getId(), today);
        }
        long waitingCount = countWaiting(store.getId(), today, resetVersion);
        int effectivePartySize = partySize != null && partySize > 0
            ? partySize
            : (myTicket == null || myTicket.getPartySize() == null ? 1 : myTicket.getPartySize());
        boolean canApply = !hasSuitableAvailableTable(store.getId(), effectivePartySize)
            && (myTicket == null || !isActive(myTicket.getStatus()));
        return new QueueStatusResponse(
            store.getId(),
            today,
            counter == null || counter.getCurrentNumber() == null ? 0 : counter.getCurrentNumber(),
            counter == null || counter.getNextNumber() == null ? 1 : counter.getNextNumber(),
            waitingCount,
            myTicket == null ? null : toTicketResponse(myTicket),
            canApply
        );
    }

    public StaffQueueStatusResponse staffStatus(Long staffId, Long storeId) {
        ensureStaffCanOperateStore(staffId, storeId);
        LocalDate today = LocalDate.now();
        WaitingQueueCounter counter = findCounter(storeId, today);
        int resetVersion = counter == null ? 0 : normalizeVersion(counter.getResetVersion());
        WaitingQueueTicket calledTicket = findCalledTicket(storeId, today, resetVersion);
        List<WaitingQueueTicket> tickets = activeTickets(storeId, today, resetVersion);
        return new StaffQueueStatusResponse(
            storeId,
            today,
            counter == null || counter.getCurrentNumber() == null ? 0 : counter.getCurrentNumber(),
            counter == null || counter.getNextNumber() == null ? 1 : counter.getNextNumber(),
            countWaiting(storeId, today, resetVersion),
            calledTicket == null ? null : toStaffTicket(calledTicket),
            tickets.stream().map(this::toStaffTicket).toList()
        );
    }

    @Transactional
    public StaffQueueStatusResponse callNext(Long staffId, Long storeId) {
        ensureStaffCanOperateStore(staffId, storeId);
        LocalDate today = LocalDate.now();
        WaitingQueueCounter counter = lockCounter(storeId, today);
        int resetVersion = normalizeVersion(counter.getResetVersion());
        LocalDateTime now = LocalDateTime.now();

        ticketMapper.update(null, new LambdaUpdateWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getStoreId, storeId)
            .eq(WaitingQueueTicket::getQueueDate, today)
            .eq(WaitingQueueTicket::getResetVersion, resetVersion)
            .eq(WaitingQueueTicket::getDeleted, 0)
            .eq(WaitingQueueTicket::getStatus, CALLED)
            .set(WaitingQueueTicket::getStatus, EXPIRED)
            .set(WaitingQueueTicket::getExpiredAt, now));

        WaitingQueueTicket next = ticketMapper.selectOne(new LambdaQueryWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getStoreId, storeId)
            .eq(WaitingQueueTicket::getQueueDate, today)
            .eq(WaitingQueueTicket::getResetVersion, resetVersion)
            .eq(WaitingQueueTicket::getDeleted, 0)
            .eq(WaitingQueueTicket::getStatus, WAITING)
            .orderByAsc(WaitingQueueTicket::getQueueNumber)
            .orderByAsc(WaitingQueueTicket::getId)
            .last("LIMIT 1"));
        if (next != null) {
            next.setStatus(CALLED);
            next.setCalledAt(now);
            ticketMapper.updateById(next);
            counter.setCurrentNumber(next.getQueueNumber());
            counterMapper.updateById(counter);
        }
        return staffStatus(staffId, storeId);
    }

    @Transactional
    public QueueTicketResponse markSeated(Long staffId, Long ticketId, MarkSeatedRequest request) {
        WaitingQueueTicket ticket = loadTicket(ticketId);
        ensureStaffCanOperateStore(staffId, ticket.getStoreId());
        if (!CALLED.equals(ticket.getStatus())) {
            throw new BizException(6107, "只有已叫号的顾客可以确认入座");
        }
        DiningTable table = validateSeatTable(ticket, request == null ? null : request.tableId());
        ticket.setTableId(table.getId());
        ticket.setStatus(SEATED);
        ticket.setSeatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);

        String oldStatus = table.getStatus();
        table.setStatus(OCCUPIED);
        diningTableMapper.updateById(table);

        DiningTableStatusLog log = new DiningTableStatusLog();
        log.setTableId(table.getId());
        log.setStoreId(table.getStoreId());
        log.setOldStatus(oldStatus);
        log.setNewStatus(OCCUPIED);
        log.setChangedBy(staffId);
        log.setReason("排队叫号，顾客入座");
        diningTableStatusLogMapper.insert(log);
        return toTicketResponse(ticket);
    }

    @Transactional
    public StaffQueueStatusResponse reset(Long staffId, Long storeId) {
        ensureStaffCanOperateStore(staffId, storeId);
        LocalDate today = LocalDate.now();
        WaitingQueueCounter counter = lockCounter(storeId, today);
        int resetVersion = normalizeVersion(counter.getResetVersion());
        LocalDateTime now = LocalDateTime.now();

        ticketMapper.update(null, new LambdaUpdateWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getStoreId, storeId)
            .eq(WaitingQueueTicket::getQueueDate, today)
            .eq(WaitingQueueTicket::getResetVersion, resetVersion)
            .eq(WaitingQueueTicket::getDeleted, 0)
            .in(WaitingQueueTicket::getStatus, List.of(WAITING, CALLED))
            .set(WaitingQueueTicket::getStatus, EXPIRED)
            .set(WaitingQueueTicket::getExpiredAt, now));

        counter.setCurrentNumber(0);
        counter.setNextNumber(1);
        counter.setResetVersion(resetVersion + 1);
        counter.setResetBy(staffId);
        counter.setResetAt(now);
        counterMapper.updateById(counter);
        return staffStatus(staffId, storeId);
    }

    @Transactional
    public QueueTicketResponse cancel(Long userId, Long ticketId) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        WaitingQueueTicket ticket = loadTicket(ticketId);
        if (!userId.equals(ticket.getUserId())) {
            throw new BizException(6106, "排队记录不存在");
        }
        if (!WAITING.equals(ticket.getStatus()) && !CALLED.equals(ticket.getStatus())) {
            throw new BizException(6107, "当前排队状态不可取消");
        }
        ticket.setStatus(CANCELLED);
        ticket.setCancelledAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        return toTicketResponse(ticket);
    }

    private WaitingQueueCounter lockCounter(Long storeId, LocalDate date) {
        counterMapper.ensureCounter(storeId, date);
        WaitingQueueCounter counter = counterMapper.selectForUpdate(storeId, date);
        if (counter == null) {
            throw new BizException(6109, "排队计数器初始化失败");
        }
        if (counter.getCurrentNumber() == null) counter.setCurrentNumber(0);
        if (counter.getNextNumber() == null) counter.setNextNumber(1);
        if (counter.getResetVersion() == null) counter.setResetVersion(0);
        return counter;
    }

    private WaitingQueueCounter findCounter(Long storeId, LocalDate date) {
        return counterMapper.selectOne(new LambdaQueryWrapper<WaitingQueueCounter>()
            .eq(WaitingQueueCounter::getStoreId, storeId)
            .eq(WaitingQueueCounter::getQueueDate, date)
            .eq(WaitingQueueCounter::getDeleted, 0)
            .last("LIMIT 1"));
    }

    private WaitingQueueTicket findActiveUserTicket(Long userId, Long storeId, LocalDate date, int resetVersion) {
        return ticketMapper.selectOne(new LambdaQueryWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getUserId, userId)
            .eq(WaitingQueueTicket::getStoreId, storeId)
            .eq(WaitingQueueTicket::getQueueDate, date)
            .eq(WaitingQueueTicket::getResetVersion, resetVersion)
            .eq(WaitingQueueTicket::getDeleted, 0)
            .in(WaitingQueueTicket::getStatus, List.of(WAITING, CALLED))
            .orderByDesc(WaitingQueueTicket::getId)
            .last("LIMIT 1"));
    }

    private WaitingQueueTicket findLatestUserTicket(Long userId, Long storeId, LocalDate date) {
        return ticketMapper.selectOne(new LambdaQueryWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getUserId, userId)
            .eq(WaitingQueueTicket::getStoreId, storeId)
            .eq(WaitingQueueTicket::getQueueDate, date)
            .eq(WaitingQueueTicket::getDeleted, 0)
            .orderByDesc(WaitingQueueTicket::getId)
            .last("LIMIT 1"));
    }

    private WaitingQueueTicket findCalledTicket(Long storeId, LocalDate date, int resetVersion) {
        return ticketMapper.selectOne(new LambdaQueryWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getStoreId, storeId)
            .eq(WaitingQueueTicket::getQueueDate, date)
            .eq(WaitingQueueTicket::getResetVersion, resetVersion)
            .eq(WaitingQueueTicket::getDeleted, 0)
            .eq(WaitingQueueTicket::getStatus, CALLED)
            .orderByAsc(WaitingQueueTicket::getQueueNumber)
            .last("LIMIT 1"));
    }

    private List<WaitingQueueTicket> activeTickets(Long storeId, LocalDate date, int resetVersion) {
        return ticketMapper.selectList(new LambdaQueryWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getStoreId, storeId)
            .eq(WaitingQueueTicket::getQueueDate, date)
            .eq(WaitingQueueTicket::getResetVersion, resetVersion)
            .eq(WaitingQueueTicket::getDeleted, 0)
            .in(WaitingQueueTicket::getStatus, List.of(CALLED, WAITING, SEATED))
            .orderByAsc(WaitingQueueTicket::getQueueNumber)
            .orderByAsc(WaitingQueueTicket::getId));
    }

    private long countWaiting(Long storeId, LocalDate date, int resetVersion) {
        return ticketMapper.selectCount(new LambdaQueryWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getStoreId, storeId)
            .eq(WaitingQueueTicket::getQueueDate, date)
            .eq(WaitingQueueTicket::getResetVersion, resetVersion)
            .eq(WaitingQueueTicket::getDeleted, 0)
            .eq(WaitingQueueTicket::getStatus, WAITING));
    }

    private WaitingQueueTicket loadTicket(Long ticketId) {
        if (ticketId == null) {
            throw new BizException(6106, "排队记录不存在");
        }
        WaitingQueueTicket ticket = ticketMapper.selectOne(new LambdaQueryWrapper<WaitingQueueTicket>()
            .eq(WaitingQueueTicket::getId, ticketId)
            .eq(WaitingQueueTicket::getDeleted, 0)
            .last("LIMIT 1"));
        if (ticket == null) {
            throw new BizException(6106, "排队记录不存在");
        }
        return ticket;
    }

    private Store ensureStoreOpen(Long storeId) {
        Store store = ensureStoreExists(storeId);
        if (!OPEN.equals(store.getStatus())) {
            throw new BizException(6102, "门店当前未营业，暂不能排队");
        }
        return store;
    }

    private Store ensureStoreExists(Long storeId) {
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
            .eq(Store::getId, storeId)
            .eq(Store::getDeleted, 0)
            .last("LIMIT 1"));
        if (store == null) {
            throw new BizException(6102, "门店不存在");
        }
        return store;
    }

    private DiningTable validateSeatTable(WaitingQueueTicket ticket, Long tableId) {
        if (tableId == null) {
            throw new BizException(6110, "请选择入座桌位");
        }
        DiningTable table = diningTableMapper.selectOne(new LambdaQueryWrapper<DiningTable>()
            .eq(DiningTable::getId, tableId)
            .eq(DiningTable::getStoreId, ticket.getStoreId())
            .eq(DiningTable::getDeleted, 0)
            .last("LIMIT 1"));
        if (table == null) {
            throw new BizException(6110, "桌位不存在或不属于当前门店");
        }
        if (!AVAILABLE.equals(table.getStatus())) {
            throw new BizException(6111, "请选择空闲桌位入座");
        }
        if (table.getCapacity() != null && ticket.getPartySize() != null && table.getCapacity() < ticket.getPartySize()) {
            throw new BizException(6112, "所选桌位容量不足");
        }
        return table;
    }

    private boolean hasSuitableAvailableTable(Long storeId, Integer partySize) {
        return diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
            .eq(DiningTable::getStoreId, storeId)
            .eq(DiningTable::getDeleted, 0)
            .eq(DiningTable::getStatus, AVAILABLE)
            .ge(DiningTable::getCapacity, partySize)) > 0;
    }

    private void ensureStaffCanOperateStore(Long staffId, Long storeId) {
        if (staffId == null) {
            throw new BizException(401, "请先登录");
        }
        if (storeId == null) {
            throw new BizException(6101, "请选择门店");
        }
        long count = userStoreRoleMapper.selectCount(new LambdaQueryWrapper<UserStoreRole>()
            .eq(UserStoreRole::getUserId, staffId)
            .eq(UserStoreRole::getStoreId, storeId)
            .eq(UserStoreRole::getRoleCode, STAFF)
            .eq(UserStoreRole::getStatus, ACTIVE));
        if (count == 0) {
            throw new BizException(6108, "无权操作该门店排队");
        }
    }

    private void validateApplyRequest(ApplyQueueRequest request) {
        if (request == null || request.storeId() == null) {
            throw new BizException(6101, "请选择门店");
        }
        if (request.partySize() == null || request.partySize() <= 0) {
            throw new BizException(6101, "请输入排队人数");
        }
        if (isBlank(request.contactName()) || isBlank(request.contactPhone())) {
            throw new BizException(6101, "联系人和手机号不能为空");
        }
    }

    private boolean isActive(String status) {
        return WAITING.equals(status) || CALLED.equals(status);
    }

    private int normalizeVersion(Integer version) {
        return version == null ? 0 : version;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private QueueTicketResponse toTicketResponse(WaitingQueueTicket ticket) {
        return new QueueTicketResponse(
            ticket.getId(),
            ticket.getStoreId(),
            ticket.getQueueDate(),
            ticket.getQueueNumber(),
            ticket.getPartySize(),
            ticket.getTableId(),
            tableNo(ticket.getTableId()),
            tableArea(ticket.getTableId()),
            ticket.getStatus(),
            ticket.getContactName(),
            ticket.getContactPhone(),
            ticket.getCalledAt(),
            ticket.getSeatedAt(),
            ticket.getCancelledAt(),
            ticket.getExpiredAt(),
            ticket.getCreatedAt()
        );
    }

    private StaffQueueTicketResponse toStaffTicket(WaitingQueueTicket ticket) {
        return new StaffQueueTicketResponse(
            ticket.getId(),
            ticket.getQueueNumber(),
            ticket.getPartySize(),
            ticket.getTableId(),
            tableNo(ticket.getTableId()),
            tableArea(ticket.getTableId()),
            ticket.getStatus(),
            ticket.getContactName(),
            ticket.getContactPhone(),
            ticket.getCalledAt(),
            ticket.getSeatedAt(),
            ticket.getExpiredAt(),
            ticket.getCreatedAt()
        );
    }

    private String tableNo(Long tableId) {
        DiningTable table = tableId == null ? null : diningTableMapper.selectById(tableId);
        return table == null ? null : table.getTableNo();
    }

    private String tableArea(Long tableId) {
        DiningTable table = tableId == null ? null : diningTableMapper.selectById(tableId);
        return table == null ? null : table.getArea();
    }

    public record ApplyQueueRequest(Long storeId, Integer partySize, String contactName, String contactPhone) {
    }

    public record QueueTicketResponse(
        Long id,
        Long storeId,
        LocalDate queueDate,
        Integer queueNumber,
        Integer partySize,
        Long tableId,
        String tableNo,
        String area,
        String status,
        String contactName,
        String contactPhone,
        LocalDateTime calledAt,
        LocalDateTime seatedAt,
        LocalDateTime cancelledAt,
        LocalDateTime expiredAt,
        LocalDateTime createdAt
    ) {
    }

    public record QueueStatusResponse(
        Long storeId,
        LocalDate queueDate,
        Integer currentNumber,
        Integer nextNumber,
        Long waitingCount,
        QueueTicketResponse myTicket,
        boolean canApply
    ) {
    }

    public record MarkSeatedRequest(Long tableId) {
    }

    public record StaffQueueTicketResponse(
        Long id,
        Integer queueNumber,
        Integer partySize,
        Long tableId,
        String tableNo,
        String area,
        String status,
        String contactName,
        String contactPhone,
        LocalDateTime calledAt,
        LocalDateTime seatedAt,
        LocalDateTime expiredAt,
        LocalDateTime createdAt
    ) {
    }

    public record StaffQueueStatusResponse(
        Long storeId,
        LocalDate queueDate,
        Integer currentNumber,
        Integer nextNumber,
        Long waitingCount,
        StaffQueueTicketResponse calledTicket,
        List<StaffQueueTicketResponse> tickets
    ) {
    }
}
