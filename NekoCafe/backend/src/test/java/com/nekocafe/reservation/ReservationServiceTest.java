package com.nekocafe.reservation;

import com.nekocafe.common.exception.BizException;
import com.nekocafe.order.service.OrderService;
import com.nekocafe.reservation.entity.Reservation;
import com.nekocafe.reservation.entity.ReservationSlot;
import com.nekocafe.reservation.mapper.ReservationMapper;
import com.nekocafe.reservation.mapper.ReservationSlotMapper;
import com.nekocafe.reservation.service.ReservationService;
import com.nekocafe.reservation.service.ReservationSlotGenerator;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceTest {

    @Test
    void createReservationSuccess() {
        ReservationSlotMapper slotMapper = mock(ReservationSlotMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationSlotGenerator slotGenerator = mock(ReservationSlotGenerator.class);
        OrderService orderService = mock(OrderService.class);
        ReservationService reservationService = new ReservationService(
            slotMapper, reservationMapper, storeMapper, diningTableMapper, slotGenerator, orderService);

        Store store = store(1L, "测试猫咖", "OPEN");
        DiningTable table = diningTable(1L, 1L, "A1", 4, "AVAILABLE");
        ReservationSlot slot = reservationSlot(1L, 1L, 1L, LocalDate.now(),
            LocalTime.of(10, 0), LocalTime.of(12, 0), 2, "AVAILABLE");

        when(storeMapper.selectOne(any())).thenReturn(store);
        when(diningTableMapper.selectOne(any())).thenReturn(table);
        when(slotMapper.selectOne(any())).thenReturn(slot);
        when(slotMapper.update(any(), any())).thenReturn(1);

        ReservationService.CreateReservationRequest request = new ReservationService.CreateReservationRequest(
            1L, 1L, 1L, 2, "Test", "13800000000", null);

        ReservationService.ReservationResponse response = reservationService.create(1L, request);

        assertThat(response.status()).isEqualTo("RESERVED");
    }

    @Test
    void slotFullThrowsWhenAvailableCountIsZero() {
        ReservationSlotMapper slotMapper = mock(ReservationSlotMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationSlotGenerator slotGenerator = mock(ReservationSlotGenerator.class);
        OrderService orderService = mock(OrderService.class);
        ReservationService reservationService = new ReservationService(
            slotMapper, reservationMapper, storeMapper, diningTableMapper, slotGenerator, orderService);

        Store store = store(1L, "测试猫咖", "OPEN");
        DiningTable table = diningTable(1L, 1L, "A1", 4, "AVAILABLE");
        ReservationSlot slot = reservationSlot(1L, 1L, 1L, LocalDate.now(),
            LocalTime.of(10, 0), LocalTime.of(12, 0), 0, "AVAILABLE");

        when(storeMapper.selectOne(any())).thenReturn(store);
        when(diningTableMapper.selectOne(any())).thenReturn(table);
        when(slotMapper.selectOne(any())).thenReturn(slot);

        ReservationService.CreateReservationRequest request = new ReservationService.CreateReservationRequest(
            1L, 1L, 1L, 2, "Test", "13800000000", null);

        assertThatThrownBy(() -> reservationService.create(1L, request))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("已约满");
    }

    @Test
    void cancelOwnReservationSuccess() {
        ReservationSlotMapper slotMapper = mock(ReservationSlotMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationSlotGenerator slotGenerator = mock(ReservationSlotGenerator.class);
        OrderService orderService = mock(OrderService.class);
        ReservationService reservationService = new ReservationService(
            slotMapper, reservationMapper, storeMapper, diningTableMapper, slotGenerator, orderService);

        Reservation reservation = reservation(1L, 1L, 1L, 1L, 1L, "RESERVED");
        Store store = store(1L, "测试猫咖", "OPEN");
        DiningTable table = diningTable(1L, 1L, "A1", 4, "AVAILABLE");
        ReservationSlot slot = reservationSlot(1L, 1L, 1L, LocalDate.now(),
            LocalTime.of(10, 0), LocalTime.of(12, 0), 2, "AVAILABLE");

        when(reservationMapper.selectOne(any())).thenReturn(reservation);
        when(orderService.hasPaidOrActiveOrdersForReservation(1L, 1L)).thenReturn(false);
        when(storeMapper.selectById(1L)).thenReturn(store);
        when(diningTableMapper.selectById(1L)).thenReturn(table);
        when(slotMapper.selectById(1L)).thenReturn(slot);

        ReservationService.ReservationResponse response = reservationService.cancel(1L, 1L);

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(orderService).cancelCreatedOrdersForReservation(1L, 1L);
    }

    @Test
    void cancelOthersReservationFails() {
        ReservationSlotMapper slotMapper = mock(ReservationSlotMapper.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        ReservationSlotGenerator slotGenerator = mock(ReservationSlotGenerator.class);
        OrderService orderService = mock(OrderService.class);
        ReservationService reservationService = new ReservationService(
            slotMapper, reservationMapper, storeMapper, diningTableMapper, slotGenerator, orderService);

        when(reservationMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> reservationService.cancel(1L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("预约不存在");
    }

    // --- helpers ---

    private Store store(Long id, String name, String status) {
        Store store = new Store();
        store.setId(id);
        store.setName(name);
        store.setStatus(status);
        return store;
    }

    private DiningTable diningTable(Long id, Long storeId, String tableNo, Integer capacity, String status) {
        DiningTable table = new DiningTable();
        table.setId(id);
        table.setStoreId(storeId);
        table.setTableNo(tableNo);
        table.setCapacity(capacity);
        table.setStatus(status);
        return table;
    }

    private ReservationSlot reservationSlot(Long id, Long storeId, Long tableId,
                                            LocalDate date, LocalTime start, LocalTime end,
                                            Integer availableCount, String status) {
        ReservationSlot slot = new ReservationSlot();
        slot.setId(id);
        slot.setStoreId(storeId);
        slot.setTableId(tableId);
        slot.setSlotDate(date);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setAvailableCount(availableCount);
        slot.setReservedCount(0);
        slot.setStatus(status);
        return slot;
    }

    private Reservation reservation(Long id, Long userId, Long storeId, Long tableId, Long slotId, String status) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setUserId(userId);
        r.setStoreId(storeId);
        r.setTableId(tableId);
        r.setSlotId(slotId);
        r.setStatus(status);
        return r;
    }
}
