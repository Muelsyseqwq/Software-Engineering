package com.nekocafe.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.reservation.entity.Reservation;
import com.nekocafe.reservation.entity.ReservationSlot;
import com.nekocafe.reservation.mapper.ReservationMapper;
import com.nekocafe.reservation.mapper.ReservationSlotMapper;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final String OPEN = "OPEN";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String RESERVED = "RESERVED";
    private static final String CANCELLED = "CANCELLED";

    private final ReservationSlotMapper slotMapper;
    private final ReservationMapper reservationMapper;
    private final StoreMapper storeMapper;
    private final DiningTableMapper diningTableMapper;

    public ReservationService(
        ReservationSlotMapper slotMapper,
        ReservationMapper reservationMapper,
        StoreMapper storeMapper,
        DiningTableMapper diningTableMapper
    ) {
        this.slotMapper = slotMapper;
        this.reservationMapper = reservationMapper;
        this.storeMapper = storeMapper;
        this.diningTableMapper = diningTableMapper;
    }

    public List<ReservationSlotResponse> slots(Long storeId, LocalDate date, Integer partySize) {
        if (storeId == null || date == null || partySize == null || partySize <= 0) {
            throw new BizException(2101, "请选择门店、日期和预约人数");
        }
        ensureStoreOpen(storeId);
        Map<Long, DiningTable> tableMap = diningTableMapper.selectList(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getStoreId, storeId)
                .eq(DiningTable::getDeleted, 0)
                .eq(DiningTable::getStatus, AVAILABLE)
                .ge(DiningTable::getCapacity, partySize))
            .stream()
            .collect(Collectors.toMap(DiningTable::getId, Function.identity()));
        if (tableMap.isEmpty()) {
            return List.of();
        }
        return slotMapper.selectList(new LambdaQueryWrapper<ReservationSlot>()
                .eq(ReservationSlot::getStoreId, storeId)
                .eq(ReservationSlot::getSlotDate, date)
                .in(ReservationSlot::getTableId, tableMap.keySet())
                .eq(ReservationSlot::getStatus, AVAILABLE)
                .gt(ReservationSlot::getAvailableCount, 0)
                .orderByAsc(ReservationSlot::getStartTime)
                .orderByAsc(ReservationSlot::getTableId))
            .stream()
            .map(slot -> toSlot(slot, tableMap.get(slot.getTableId())))
            .toList();
    }

    @Transactional
    public ReservationResponse create(Long userId, CreateReservationRequest request) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        validateCreateRequest(request);
        Store store = ensureStoreOpen(request.storeId());
        DiningTable table = diningTableMapper.selectOne(new LambdaQueryWrapper<DiningTable>()
            .eq(DiningTable::getId, request.tableId())
            .eq(DiningTable::getStoreId, request.storeId())
            .eq(DiningTable::getDeleted, 0)
            .last("LIMIT 1"));
        if (table == null || !AVAILABLE.equals(table.getStatus())) {
            throw new BizException(2102, "所选桌位不可预约");
        }
        if (request.partySize() > table.getCapacity()) {
            throw new BizException(2103, "预约人数超过桌位容量");
        }
        ReservationSlot slot = slotMapper.selectOne(new LambdaQueryWrapper<ReservationSlot>()
            .eq(ReservationSlot::getId, request.slotId())
            .eq(ReservationSlot::getStoreId, request.storeId())
            .eq(ReservationSlot::getTableId, request.tableId())
            .eq(ReservationSlot::getStatus, AVAILABLE)
            .last("LIMIT 1"));
        if (slot == null) {
            throw new BizException(2104, "预约时段不存在");
        }
        if (slot.getAvailableCount() == null || slot.getAvailableCount() <= 0) {
            throw new BizException(2105, "该时段已约满");
        }

        int updated = slotMapper.update(null, new LambdaUpdateWrapper<ReservationSlot>()
            .eq(ReservationSlot::getId, slot.getId())
            .gt(ReservationSlot::getAvailableCount, 0)
            .setSql("reserved_count = reserved_count + 1")
            .setSql("available_count = available_count - 1"));
        if (updated == 0) {
            throw new BizException(2105, "该时段已约满");
        }

        Reservation reservation = new Reservation();
        reservation.setReservationNo(generateReservationNo(userId));
        reservation.setUserId(userId);
        reservation.setStoreId(request.storeId());
        reservation.setTableId(request.tableId());
        reservation.setSlotId(request.slotId());
        reservation.setPartySize(request.partySize());
        reservation.setStatus(RESERVED);
        reservation.setContactName(request.contactName().trim());
        reservation.setContactPhone(request.contactPhone().trim());
        reservation.setRemark(normalizeOptional(request.remark()));
        reservationMapper.insert(reservation);

        slot.setAvailableCount(slot.getAvailableCount() - 1);
        slot.setReservedCount(slot.getReservedCount() + 1);
        return toReservation(reservation, store, table, slot);
    }

    public List<ReservationResponse> mine(Long userId) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
            .eq(Reservation::getUserId, userId)
            .eq(Reservation::getDeleted, 0)
            .orderByDesc(Reservation::getCreatedAt)
            .orderByDesc(Reservation::getId));
        return reservations.stream().map(this::toReservation).toList();
    }

    @Transactional
    public ReservationResponse cancel(Long userId, Long id) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        Reservation reservation = reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
            .eq(Reservation::getId, id)
            .eq(Reservation::getUserId, userId)
            .eq(Reservation::getDeleted, 0)
            .last("LIMIT 1"));
        if (reservation == null) {
            throw new BizException(2106, "预约不存在");
        }
        if (CANCELLED.equals(reservation.getStatus()) || "CHECKED_IN".equals(reservation.getStatus()) || "COMPLETED".equals(reservation.getStatus())) {
            throw new BizException(2107, "当前预约状态不可取消");
        }
        reservation.setStatus(CANCELLED);
        reservationMapper.updateById(reservation);
        slotMapper.update(null, new LambdaUpdateWrapper<ReservationSlot>()
            .eq(ReservationSlot::getId, reservation.getSlotId())
            .setSql("reserved_count = GREATEST(reserved_count - 1, 0)")
            .setSql("available_count = available_count + 1"));
        return toReservation(reservation);
    }

    private ReservationResponse toReservation(Reservation reservation) {
        Store store = storeMapper.selectById(reservation.getStoreId());
        DiningTable table = diningTableMapper.selectById(reservation.getTableId());
        ReservationSlot slot = slotMapper.selectById(reservation.getSlotId());
        return toReservation(reservation, store, table, slot);
    }

    private ReservationResponse toReservation(Reservation reservation, Store store, DiningTable table, ReservationSlot slot) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getReservationNo(),
            reservation.getStoreId(),
            store == null ? "未知门店" : store.getName(),
            reservation.getTableId(),
            table == null ? "未知桌位" : table.getTableNo(),
            table == null ? null : table.getArea(),
            reservation.getSlotId(),
            reservation.getPartySize(),
            slot == null ? null : slot.getSlotDate(),
            slot == null ? null : slot.getStartTime(),
            slot == null ? null : slot.getEndTime(),
            reservation.getStatus(),
            reservation.getContactName(),
            reservation.getContactPhone(),
            reservation.getRemark(),
            reservation.getCreatedAt()
        );
    }

    private ReservationSlotResponse toSlot(ReservationSlot slot, DiningTable table) {
        return new ReservationSlotResponse(
            slot.getId(),
            slot.getStoreId(),
            slot.getTableId(),
            table == null ? "未知桌位" : table.getTableNo(),
            table == null ? null : table.getCapacity(),
            table == null ? null : table.getArea(),
            slot.getSlotDate(),
            slot.getStartTime(),
            slot.getEndTime(),
            slot.getAvailableCount(),
            slot.getStatus()
        );
    }

    private Store ensureStoreOpen(Long storeId) {
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
            .eq(Store::getId, storeId)
            .eq(Store::getDeleted, 0)
            .last("LIMIT 1"));
        if (store == null) {
            throw new BizException(2001, "门店不存在");
        }
        if (!OPEN.equals(store.getStatus())) {
            throw new BizException(2108, "门店当前未营业");
        }
        return store;
    }

    private void validateCreateRequest(CreateReservationRequest request) {
        if (request == null || request.storeId() == null || request.tableId() == null || request.slotId() == null) {
            throw new BizException(2101, "请选择门店、桌位和预约时段");
        }
        if (request.partySize() == null || request.partySize() <= 0) {
            throw new BizException(2109, "预约人数必须大于 0");
        }
        if (isBlank(request.contactName()) || isBlank(request.contactPhone())) {
            throw new BizException(2110, "联系人和手机号不能为空");
        }
    }

    private String generateReservationNo(Long userId) {
        return "R" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId;
    }

    private String normalizeOptional(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateReservationRequest(Long storeId, Long tableId, Long slotId, Integer partySize, String contactName, String contactPhone, String remark) {
    }

    public record ReservationSlotResponse(
        Long id,
        Long storeId,
        Long tableId,
        String tableNo,
        Integer capacity,
        String area,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer availableCount,
        String status
    ) {
    }

    public record ReservationResponse(
        Long id,
        String reservationNo,
        Long storeId,
        String storeName,
        Long tableId,
        String tableNo,
        String area,
        Long slotId,
        Integer partySize,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String contactName,
        String contactPhone,
        String remark,
        LocalDateTime createdAt
    ) {
    }
}
