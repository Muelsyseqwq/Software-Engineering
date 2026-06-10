package com.nekocafe.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nekocafe.cat.entity.Cat;
import com.nekocafe.cat.mapper.CatMapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.manager.dto.*;
import com.nekocafe.manager.entity.*;
import com.nekocafe.manager.mapper.*;
import com.nekocafe.menu.entity.Dish;
import com.nekocafe.menu.mapper.DishMapper;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.entity.FoodOrderItem;
import com.nekocafe.order.mapper.FoodOrderItemMapper;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.reservation.entity.Reservation;
import com.nekocafe.reservation.entity.ReservationSlot;
import com.nekocafe.reservation.mapper.ReservationMapper;
import com.nekocafe.reservation.mapper.ReservationSlotMapper;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.user.entity.User;
import com.nekocafe.user.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StoreManagerService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISMISSED = "DISMISSED";
    private static final String STORE_MANAGER = "STORE_MANAGER";
    private static final String STAFF = "STAFF";
    private static final String OPEN = "OPEN";
    private static final String CLOSED = "CLOSED";
    private static final String PREPARING = "PREPARING";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String DISABLED = "DISABLED";
    private static final String UNAVAILABLE = "UNAVAILABLE";
    private static final String RESERVED = "RESERVED";
    private static final String CHECKED_IN = "CHECKED_IN";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";
    private static final String SCHEDULED = "SCHEDULED";
    private static final String ON_LEAVE = "ON_LEAVE";
    private static final String APPROVED = "APPROVED";
    private static final Set<String> STORE_STATUSES = Set.of(OPEN, CLOSED, PREPARING);
    private static final Set<String> TABLE_STATUSES = Set.of(AVAILABLE, "OCCUPIED", RESERVED, "CLEANING", DISABLED, UNAVAILABLE);
    private static final Set<String> RESERVATION_ACTION_STATUSES = Set.of(CHECKED_IN, COMPLETED, CANCELLED);
    private static final Set<String> REVENUE_ORDER_STATUSES = Set.of("PAID", PREPARING, COMPLETED);
    private static final Set<String> SHIFT_STATUSES = Set.of(SCHEDULED, ON_LEAVE, CANCELLED, COMPLETED);
    private static final Set<String> ACTIVITY_DECISIONS = Set.of("ACCEPTED", "REJECTED");
    private static final DateTimeFormatter SLOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ManagerUserStoreRoleMapper userStoreRoleMapper;
    private final StoreMapper storeMapper;
    private final DiningTableMapper diningTableMapper;
    private final ReservationMapper reservationMapper;
    private final ReservationSlotMapper slotMapper;
    private final FoodOrderMapper foodOrderMapper;
    private final FoodOrderItemMapper foodOrderItemMapper;
    private final UserMapper userMapper;
    private final CatMapper catMapper;
    private final StaffShiftMapper staffShiftMapper;
    private final StaffLeaveRequestMapper staffLeaveRequestMapper;
    private final ManagerPromotionActivityMapper promotionActivityMapper;
    private final ManagerActivityStoreMapper activityStoreMapper;
    private final DishMapper dishMapper;
    private final DishPriceHistoryMapper dishPriceHistoryMapper;

    public StoreManagerService(
        ManagerUserStoreRoleMapper userStoreRoleMapper,
        StoreMapper storeMapper,
        DiningTableMapper diningTableMapper,
        ReservationMapper reservationMapper,
        ReservationSlotMapper slotMapper,
        FoodOrderMapper foodOrderMapper,
        FoodOrderItemMapper foodOrderItemMapper,
        UserMapper userMapper,
        CatMapper catMapper,
        StaffShiftMapper staffShiftMapper,
        StaffLeaveRequestMapper staffLeaveRequestMapper,
        ManagerPromotionActivityMapper promotionActivityMapper,
        ManagerActivityStoreMapper activityStoreMapper,
        DishMapper dishMapper,
        DishPriceHistoryMapper dishPriceHistoryMapper
    ) {
        this.userStoreRoleMapper = userStoreRoleMapper;
        this.storeMapper = storeMapper;
        this.diningTableMapper = diningTableMapper;
        this.reservationMapper = reservationMapper;
        this.slotMapper = slotMapper;
        this.foodOrderMapper = foodOrderMapper;
        this.foodOrderItemMapper = foodOrderItemMapper;
        this.userMapper = userMapper;
        this.catMapper = catMapper;
        this.staffShiftMapper = staffShiftMapper;
        this.staffLeaveRequestMapper = staffLeaveRequestMapper;
        this.promotionActivityMapper = promotionActivityMapper;
        this.activityStoreMapper = activityStoreMapper;
        this.dishMapper = dishMapper;
        this.dishPriceHistoryMapper = dishPriceHistoryMapper;
    }

    public ManagerStoreInfo store(Long managerUserId) {
        return toStoreInfo(loadManagedStore(managerUserId));
    }

    @Transactional
    public ManagerStoreInfo updateStore(Long managerUserId, UpdateManagerStoreRequest request) {
        Store store = loadManagedStore(managerUserId);
        validateStoreRequest(request);
        store.setName(request.name().trim());
        store.setCity(request.city().trim());
        store.setAddress(request.address().trim());
        store.setPhone(normalizeOptional(request.phone()));
        store.setOpeningTime(request.openingTime());
        store.setClosingTime(request.closingTime());
        store.setDescription(normalizeOptional(request.description()));
        storeMapper.updateById(store);
        return toStoreInfo(store);
    }

    @Transactional
    public void updateStoreStatus(Long managerUserId, String status) {
        Store store = loadManagedStore(managerUserId);
        String normalized = normalizeRequired(status, "门店状态不能为空").toUpperCase();
        if (!STORE_STATUSES.contains(normalized)) {
            throw new BizException(2203, "门店状态只能为营业、歇业或筹备");
        }
        storeMapper.update(null, new LambdaUpdateWrapper<Store>()
            .eq(Store::getId, store.getId())
            .eq(Store::getDeleted, 0)
            .set(Store::getStatus, normalized));
    }

    public List<ManagerTableRow> tables(Long managerUserId) {
        Long storeId = resolveManagedStoreId(managerUserId);
        return diningTableMapper.selectList(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getStoreId, storeId)
                .eq(DiningTable::getDeleted, 0)
                .orderByAsc(DiningTable::getArea)
                .orderByAsc(DiningTable::getTableNo)
                .orderByAsc(DiningTable::getId))
            .stream()
            .map(this::toTableRow)
            .toList();
    }

    @Transactional
    public ManagerTableRow createTable(Long managerUserId, ManagerTableRow payload) {
        Long storeId = resolveManagedStoreId(managerUserId);
        String tableStatus = validateTablePayload(payload);
        ensureTableNoAvailable(storeId, payload.tableNo(), null);

        DiningTable table = new DiningTable();
        table.setStoreId(storeId);
        table.setTableNo(payload.tableNo().trim());
        table.setCapacity(payload.capacity());
        table.setArea(normalizeOptional(payload.area()));
        table.setStatus(tableStatus);
        try {
            diningTableMapper.insert(table);
        } catch (DuplicateKeyException exception) {
            throw new BizException(2210, "桌号已存在");
        }
        return toTableRow(table);
    }

    @Transactional
    public ManagerTableRow updateTable(Long managerUserId, Long id, ManagerTableRow payload) {
        Long storeId = resolveManagedStoreId(managerUserId);
        if (id == null) {
            throw new BizException(2211, "请选择要编辑的桌位");
        }
        String tableStatus = validateTablePayload(payload);
        DiningTable table = diningTableMapper.selectOne(new LambdaQueryWrapper<DiningTable>()
            .eq(DiningTable::getId, id)
            .eq(DiningTable::getStoreId, storeId)
            .eq(DiningTable::getDeleted, 0)
            .last("LIMIT 1"));
        if (table == null) {
            throw new BizException(2212, "桌位不存在或不属于当前门店");
        }
        ensureTableNoAvailable(storeId, payload.tableNo(), id);

        table.setTableNo(payload.tableNo().trim());
        table.setCapacity(payload.capacity());
        table.setArea(normalizeOptional(payload.area()));
        table.setStatus(tableStatus);
        try {
            diningTableMapper.updateById(table);
        } catch (DuplicateKeyException exception) {
            throw new BizException(2210, "桌号已存在");
        }
        return toTableRow(table);
    }

    public List<ManagerReservationRow> reservations(Long managerUserId, String status, LocalDate date) {
        Long storeId = resolveManagedStoreId(managerUserId);
        String normalizedStatus = normalizeOptional(status);
        List<Long> slotIds = null;
        if (date != null) {
            slotIds = slotMapper.selectList(new LambdaQueryWrapper<ReservationSlot>()
                    .eq(ReservationSlot::getStoreId, storeId)
                    .eq(ReservationSlot::getSlotDate, date))
                .stream()
                .map(ReservationSlot::getId)
                .toList();
            if (slotIds.isEmpty()) {
                return List.of();
            }
        }

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
            .eq(Reservation::getStoreId, storeId)
            .eq(Reservation::getDeleted, 0)
            .orderByDesc(Reservation::getCreatedAt)
            .orderByDesc(Reservation::getId);
        if (normalizedStatus != null) {
            wrapper.eq(Reservation::getStatus, normalizedStatus);
        }
        if (slotIds != null) {
            wrapper.in(Reservation::getSlotId, slotIds);
        }
        return toReservationRows(reservationMapper.selectList(wrapper));
    }

    @Transactional
    public ManagerReservationRow updateReservationStatus(Long managerUserId, Long reservationId, String status) {
        Long storeId = resolveManagedStoreId(managerUserId);
        if (reservationId == null) {
            throw new BizException(2220, "请选择要处理的预约");
        }
        String targetStatus = normalizeRequired(status, "预约状态不能为空").toUpperCase();
        if (!RESERVATION_ACTION_STATUSES.contains(targetStatus)) {
            throw new BizException(2221, "不支持的预约状态操作");
        }
        Reservation reservation = reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
            .eq(Reservation::getId, reservationId)
            .eq(Reservation::getStoreId, storeId)
            .eq(Reservation::getDeleted, 0)
            .last("LIMIT 1"));
        if (reservation == null) {
            throw new BizException(2222, "预约不存在或不属于当前门店");
        }
        String currentStatus = reservation.getStatus();
        if (CANCELLED.equals(currentStatus) || COMPLETED.equals(currentStatus)) {
            throw new BizException(2223, "当前预约状态不可继续变更");
        }

        if (CHECKED_IN.equals(targetStatus)) {
            if (!RESERVED.equals(currentStatus)) {
                throw new BizException(2224, "只有待到店预约可以办理到店");
            }
            reservation.setStatus(CHECKED_IN);
            reservation.setCheckedInAt(LocalDateTime.now());
            reservationMapper.updateById(reservation);
        } else if (COMPLETED.equals(targetStatus)) {
            if (!CHECKED_IN.equals(currentStatus)) {
                throw new BizException(2225, "只有已到店预约可以完成");
            }
            reservation.setStatus(COMPLETED);
            reservationMapper.updateById(reservation);
        } else if (CANCELLED.equals(targetStatus)) {
            if (!RESERVED.equals(currentStatus)) {
                throw new BizException(2226, "只有待到店预约可以取消");
            }
            reservation.setStatus(CANCELLED);
            reservationMapper.updateById(reservation);
            slotMapper.update(null, new LambdaUpdateWrapper<ReservationSlot>()
                .eq(ReservationSlot::getId, reservation.getSlotId())
                .setSql("reserved_count = GREATEST(reserved_count - 1, 0)")
                .setSql("available_count = available_count + 1"));
        }
        return toReservationRows(List.of(reservation)).get(0);
    }

    public ManagerMetricsSummary metrics(Long managerUserId, LocalDate from, LocalDate to) {
        Long storeId = resolveManagedStoreId(managerUserId);
        Store store = loadManagedStore(managerUserId);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.isAfter(end)) {
            throw new BizException(2230, "开始日期不能晚于结束日期");
        }
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay();

        List<FoodOrder> orders = foodOrderMapper.selectList(new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getStoreId, storeId)
            .eq(FoodOrder::getDeleted, 0)
            .ge(FoodOrder::getCreatedAt, startAt)
            .lt(FoodOrder::getCreatedAt, endAt));
        List<Reservation> reservationList = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
            .eq(Reservation::getStoreId, storeId)
            .eq(Reservation::getDeleted, 0)
            .ge(Reservation::getCreatedAt, startAt)
            .lt(Reservation::getCreatedAt, endAt));
        long tableCount = diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
            .eq(DiningTable::getStoreId, storeId)
            .eq(DiningTable::getDeleted, 0));
        BigDecimal revenue = orders.stream()
            .filter(order -> REVENUE_ORDER_STATUSES.contains(order.getStatus()))
            .map(FoodOrder::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidOrderCount = orders.stream().filter(order -> REVENUE_ORDER_STATUSES.contains(order.getStatus())).count();
        long completedOrderCount = orders.stream().filter(order -> COMPLETED.equals(order.getStatus())).count();
        long checkedInReservationCount = reservationList.stream()
            .filter(reservation -> Set.of(CHECKED_IN, COMPLETED).contains(reservation.getStatus()))
            .count();
        long days = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
        BigDecimal tableTurnoverRate = tableCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(checkedInReservationCount)
            .divide(BigDecimal.valueOf(tableCount * days), 2, RoundingMode.HALF_UP);
        BigDecimal area = store.getAreaSquareMeter();
        BigDecimal revenuePerSquareMeter = area == null || area.compareTo(BigDecimal.ZERO) <= 0
            ? BigDecimal.ZERO
            : revenue.divide(area, 2, RoundingMode.HALF_UP);
        BigDecimal averageOrderValue = paidOrderCount == 0
            ? BigDecimal.ZERO
            : revenue.divide(BigDecimal.valueOf(paidOrderCount), 2, RoundingMode.HALF_UP);
        return new ManagerMetricsSummary(storeId, start, end, revenue, paidOrderCount, completedOrderCount,
            (long) reservationList.size(), checkedInReservationCount, tableCount, tableTurnoverRate, area,
            revenuePerSquareMeter, averageOrderValue);
    }

    public List<ManagerOrderRow> orders(Long managerUserId, String status, LocalDate from, LocalDate to) {
        Long storeId = resolveManagedStoreId(managerUserId);
        LambdaQueryWrapper<FoodOrder> wrapper = orderQuery(storeId, status, from, to)
            .orderByDesc(FoodOrder::getCreatedAt)
            .orderByDesc(FoodOrder::getId);
        return toOrderRows(foodOrderMapper.selectList(wrapper));
    }

    public ManagerOrderDetail orderDetail(Long managerUserId, Long id) {
        Long storeId = resolveManagedStoreId(managerUserId);
        FoodOrder order = foodOrderMapper.selectOne(new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getId, id)
            .eq(FoodOrder::getStoreId, storeId)
            .eq(FoodOrder::getDeleted, 0)
            .last("LIMIT 1"));
        if (order == null) {
            throw new BizException(2231, "订单不存在或不属于当前门店");
        }
        ManagerOrderRow row = toOrderRows(List.of(order)).get(0);
        List<ManagerOrderItemRow> items = foodOrderItemMapper.selectList(new LambdaQueryWrapper<FoodOrderItem>()
                .eq(FoodOrderItem::getOrderId, order.getId())
                .orderByAsc(FoodOrderItem::getId))
            .stream()
            .map(item -> new ManagerOrderItemRow(item.getId(), item.getDishId(), item.getDishName(), item.getUnitPrice(), item.getQuantity(), item.getSubtotal()))
            .toList();
        return new ManagerOrderDetail(row.id(), row.orderNo(), row.customerName(), row.tableNo(), row.reservationNo(),
            row.totalAmount(), row.status(), row.refundStatus(), order.getRemark(), row.paidAt(), row.completedAt(), row.createdAt(), items);
    }

    public List<ManagerCatStatusRow> cats(Long managerUserId, String status) {
        Long storeId = resolveManagedStoreId(managerUserId);
        LambdaQueryWrapper<Cat> wrapper = new LambdaQueryWrapper<Cat>()
            .eq(Cat::getStoreId, storeId)
            .eq(Cat::getDeleted, 0)
            .orderByAsc(Cat::getName)
            .orderByAsc(Cat::getId);
        String normalizedStatus = normalizeOptional(status);
        if (normalizedStatus != null) {
            wrapper.eq(Cat::getStatus, normalizedStatus);
        }
        return catMapper.selectList(wrapper).stream()
            .map(cat -> new ManagerCatStatusRow(cat.getId(), cat.getName(), cat.getBreed(), cat.getAge(), cat.getGender(),
                cat.getHealthStatus(), cat.getStatus(), cat.getPhotoUrl(), cat.getDescription()))
            .toList();
    }

    public List<ManagerStaffRow> staff(Long managerUserId, String status, String roleCode) {
        Long storeId = resolveManagedStoreId(managerUserId);
        LambdaQueryWrapper<UserStoreRole> wrapper = new LambdaQueryWrapper<UserStoreRole>()
            .eq(UserStoreRole::getStoreId, storeId)
            .ne(UserStoreRole::getRoleCode, STORE_MANAGER)
            .orderByAsc(UserStoreRole::getRoleCode)
            .orderByAsc(UserStoreRole::getUserId);
        String normalizedStatus = normalizeOptional(status);
        String normalizedRole = normalizeOptional(roleCode);
        if (normalizedStatus != null) wrapper.eq(UserStoreRole::getStatus, normalizedStatus);
        if (normalizedRole != null) wrapper.eq(UserStoreRole::getRoleCode, normalizedRole);
        List<UserStoreRole> roles = userStoreRoleMapper.selectList(wrapper);
        Map<Long, User> userMap = selectByIds(roles.stream().map(UserStoreRole::getUserId).collect(Collectors.toSet()), userMapper::selectBatchIds, User::getId);
        LocalDate today = LocalDate.now();
        List<StaffShift> todayShifts = staffShiftMapper.selectList(new LambdaQueryWrapper<StaffShift>()
            .eq(StaffShift::getStoreId, storeId)
            .eq(StaffShift::getShiftDate, today));
        Map<Long, StaffShift> shiftMap = todayShifts.stream().collect(Collectors.toMap(StaffShift::getUserId, Function.identity(), (first, ignored) -> first));
        List<StaffLeaveRequest> leaves = staffLeaveRequestMapper.selectList(new LambdaQueryWrapper<StaffLeaveRequest>()
            .eq(StaffLeaveRequest::getStoreId, storeId)
            .eq(StaffLeaveRequest::getStatus, APPROVED)
            .le(StaffLeaveRequest::getStartDate, today)
            .ge(StaffLeaveRequest::getEndDate, today));
        Map<Long, StaffLeaveRequest> leaveMap = leaves.stream().collect(Collectors.toMap(StaffLeaveRequest::getUserId, Function.identity(), (first, ignored) -> first));
        return roles.stream().map(role -> {
            User user = userMap.get(role.getUserId());
            StaffShift shift = shiftMap.get(role.getUserId());
            StaffLeaveRequest leave = leaveMap.get(role.getUserId());
            return new ManagerStaffRow(role.getId(), role.getUserId(), user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(), user == null ? null : user.getPhone(), user == null ? null : user.getEmail(),
                role.getRoleCode(), role.getStatus(), shift == null ? null : shift.getShiftDate(), shift == null ? null : shift.getStartTime(),
                shift == null ? null : shift.getEndTime(), shift == null ? null : shift.getStatus(), leave == null ? null : leave.getLeaveType());
        }).toList();
    }

    @Transactional
    public void dismissStaff(Long managerUserId, Long userStoreRoleId, DismissStaffRequest request) {
        Long storeId = resolveManagedStoreId(managerUserId);
        UserStoreRole role = loadStoreRole(storeId, userStoreRoleId);
        if (STORE_MANAGER.equals(role.getRoleCode())) {
            throw new BizException(2241, "不能通过此接口调整店长身份");
        }
        role.setStatus(DISMISSED);
        role.setDismissedBy(managerUserId);
        role.setDismissedAt(LocalDateTime.now());
        role.setDismissReason(normalizeOptional(request == null ? null : request.reason()));
        userStoreRoleMapper.updateById(role);
    }

    @Transactional
    public void grantLeave(Long managerUserId, Long userId, GrantLeaveRequest request) {
        Long storeId = resolveManagedStoreId(managerUserId);
        UserStoreRole role = loadActiveStaffRole(storeId, userId);
        validateLeaveRequest(request);
        StaffLeaveRequest leave = new StaffLeaveRequest();
        leave.setStoreId(storeId);
        leave.setUserId(userId);
        leave.setLeaveType(normalizeRequired(request.leaveType(), "请假类型不能为空").toUpperCase());
        leave.setStartDate(request.startDate());
        leave.setEndDate(request.endDate());
        leave.setReason(normalizeOptional(request.reason()));
        leave.setStatus(APPROVED);
        leave.setApprovedBy(managerUserId);
        leave.setApprovedAt(LocalDateTime.now());
        staffLeaveRequestMapper.insert(leave);
        staffShiftMapper.update(null, new LambdaUpdateWrapper<StaffShift>()
            .eq(StaffShift::getStoreId, storeId)
            .eq(StaffShift::getUserId, role.getUserId())
            .eq(StaffShift::getStatus, SCHEDULED)
            .ge(StaffShift::getShiftDate, request.startDate())
            .le(StaffShift::getShiftDate, request.endDate())
            .set(StaffShift::getStatus, ON_LEAVE));
    }

    public List<ManagerShiftRow> shifts(Long managerUserId, LocalDate from, LocalDate to, Long userId) {
        Long storeId = resolveManagedStoreId(managerUserId);
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start.plusDays(6) : to;
        if (start.isAfter(end)) {
            throw new BizException(2242, "开始日期不能晚于结束日期");
        }
        LambdaQueryWrapper<StaffShift> wrapper = new LambdaQueryWrapper<StaffShift>()
            .eq(StaffShift::getStoreId, storeId)
            .ge(StaffShift::getShiftDate, start)
            .le(StaffShift::getShiftDate, end)
            .orderByAsc(StaffShift::getShiftDate)
            .orderByAsc(StaffShift::getStartTime)
            .orderByAsc(StaffShift::getUserId);
        if (userId != null) wrapper.eq(StaffShift::getUserId, userId);
        return toShiftRows(staffShiftMapper.selectList(wrapper));
    }

    @Transactional
    public ManagerShiftRow createShift(Long managerUserId, ManagerShiftRequest request) {
        Long storeId = resolveManagedStoreId(managerUserId);
        validateShiftRequest(request);
        UserStoreRole role = loadActiveStaffRole(storeId, request.userId());
        StaffShift shift = new StaffShift();
        shift.setStoreId(storeId);
        shift.setUserId(role.getUserId());
        shift.setRoleCode(normalizeOptional(request.roleCode()) == null ? role.getRoleCode() : request.roleCode().trim().toUpperCase());
        shift.setShiftDate(request.shiftDate());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setStatus(normalizeRequired(request.status(), "排班状态不能为空").toUpperCase());
        shift.setRemark(normalizeOptional(request.remark()));
        shift.setCreatedBy(managerUserId);
        try {
            staffShiftMapper.insert(shift);
        } catch (DuplicateKeyException exception) {
            throw new BizException(2243, "该员工在此时段已有排班");
        }
        return toShiftRows(List.of(shift)).get(0);
    }

    @Transactional
    public ManagerShiftRow updateShift(Long managerUserId, Long id, ManagerShiftRequest request) {
        Long storeId = resolveManagedStoreId(managerUserId);
        validateShiftRequest(request);
        StaffShift shift = staffShiftMapper.selectOne(new LambdaQueryWrapper<StaffShift>()
            .eq(StaffShift::getId, id)
            .eq(StaffShift::getStoreId, storeId)
            .last("LIMIT 1"));
        if (shift == null) {
            throw new BizException(2244, "排班不存在或不属于当前门店");
        }
        UserStoreRole role = loadActiveStaffRole(storeId, request.userId());
        shift.setUserId(role.getUserId());
        shift.setRoleCode(normalizeOptional(request.roleCode()) == null ? role.getRoleCode() : request.roleCode().trim().toUpperCase());
        shift.setShiftDate(request.shiftDate());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setStatus(normalizeRequired(request.status(), "排班状态不能为空").toUpperCase());
        shift.setRemark(normalizeOptional(request.remark()));
        try {
            staffShiftMapper.updateById(shift);
        } catch (DuplicateKeyException exception) {
            throw new BizException(2243, "该员工在此时段已有排班");
        }
        return toShiftRows(List.of(shift)).get(0);
    }

    public List<ManagerActivityRow> activities(Long managerUserId, String status) {
        Long storeId = resolveManagedStoreId(managerUserId);
        LambdaQueryWrapper<ActivityStore> wrapper = new LambdaQueryWrapper<ActivityStore>()
            .eq(ActivityStore::getStoreId, storeId)
            .orderByDesc(ActivityStore::getCreatedAt)
            .orderByDesc(ActivityStore::getId);
        String normalizedStatus = normalizeOptional(status);
        if (normalizedStatus != null) wrapper.eq(ActivityStore::getAcceptStatus, normalizedStatus);
        List<ActivityStore> rows = activityStoreMapper.selectList(wrapper);
        Map<Long, PromotionActivity> activityMap = selectByIds(rows.stream().map(ActivityStore::getActivityId).collect(Collectors.toSet()), promotionActivityMapper::selectBatchIds, PromotionActivity::getId);
        return rows.stream().map(row -> {
            PromotionActivity activity = activityMap.get(row.getActivityId());
            if (activity == null || Integer.valueOf(1).equals(activity.getDeleted())) return null;
            return new ManagerActivityRow(row.getId(), row.getActivityId(), activity.getTitle(), activity.getType(), activity.getDescription(),
                activity.getCoverUrl(), activity.getStartAt(), activity.getEndAt(), activity.getStatus(), row.getAcceptStatus(), row.getHandledAt(), row.getHandleRemark());
        }).filter(Objects::nonNull).toList();
    }

    @Transactional
    public ManagerActivityRow decideActivity(Long managerUserId, Long activityStoreId, ActivityDecisionRequest request) {
        Long storeId = resolveManagedStoreId(managerUserId);
        String decision = normalizeRequired(request == null ? null : request.acceptStatus(), "请选择活动处理结果").toUpperCase();
        if (!ACTIVITY_DECISIONS.contains(decision)) {
            throw new BizException(2250, "活动处理结果只能为接受或拒绝");
        }
        ActivityStore row = activityStoreMapper.selectOne(new LambdaQueryWrapper<ActivityStore>()
            .eq(ActivityStore::getId, activityStoreId)
            .eq(ActivityStore::getStoreId, storeId)
            .last("LIMIT 1"));
        if (row == null) {
            throw new BizException(2251, "活动不存在或不属于当前门店");
        }
        row.setAcceptStatus(decision);
        row.setHandledBy(managerUserId);
        row.setHandledAt(LocalDateTime.now());
        row.setHandleRemark(normalizeOptional(request.remark()));
        activityStoreMapper.updateById(row);
        return activities(managerUserId, null).stream()
            .filter(activity -> activity.activityStoreId().equals(row.getId()))
            .findFirst()
            .orElseThrow(() -> new BizException(2251, "活动不存在或不属于当前门店"));
    }

    public List<ManagerDishRow> dishes(Long managerUserId, Long categoryId, String status) {
        Long storeId = resolveManagedStoreId(managerUserId);
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
            .eq(Dish::getStoreId, storeId)
            .eq(Dish::getDeleted, 0)
            .orderByAsc(Dish::getCategoryId)
            .orderByAsc(Dish::getId);
        if (categoryId != null) wrapper.eq(Dish::getCategoryId, categoryId);
        String normalizedStatus = normalizeOptional(status);
        if (normalizedStatus != null) wrapper.eq(Dish::getStatus, normalizedStatus);
        return dishMapper.selectList(wrapper).stream()
            .map(dish -> new ManagerDishRow(dish.getId(), dish.getCategoryId(), dish.getName(), dish.getPrice(), dish.getStock(), dish.getStatus(), dish.getDescription(), dish.getImageUrl()))
            .toList();
    }

    @Transactional
    public ManagerDishRow updateDishPrice(Long managerUserId, Long dishId, UpdateDishPriceRequest request) {
        Long storeId = resolveManagedStoreId(managerUserId);
        if (request == null || request.newPrice() == null || request.newPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(2260, "新价格必须大于 0");
        }
        Dish dish = dishMapper.selectOne(new LambdaQueryWrapper<Dish>()
            .eq(Dish::getId, dishId)
            .eq(Dish::getStoreId, storeId)
            .eq(Dish::getDeleted, 0)
            .last("LIMIT 1"));
        if (dish == null) {
            throw new BizException(2261, "菜品不存在或不属于当前门店");
        }
        BigDecimal oldPrice = dish.getPrice();
        dish.setPrice(request.newPrice());
        dishMapper.updateById(dish);
        DishPriceHistory history = new DishPriceHistory();
        history.setDishId(dish.getId());
        history.setStoreId(storeId);
        history.setOldPrice(oldPrice == null ? BigDecimal.ZERO : oldPrice);
        history.setNewPrice(request.newPrice());
        history.setChangedBy(managerUserId);
        history.setReason(normalizeOptional(request.reason()));
        dishPriceHistoryMapper.insert(history);
        return new ManagerDishRow(dish.getId(), dish.getCategoryId(), dish.getName(), dish.getPrice(), dish.getStock(), dish.getStatus(), dish.getDescription(), dish.getImageUrl());
    }

    public List<DishPriceHistoryRow> dishPriceHistory(Long managerUserId, Long dishId) {
        Long storeId = resolveManagedStoreId(managerUserId);
        Dish dish = dishMapper.selectOne(new LambdaQueryWrapper<Dish>()
            .eq(Dish::getId, dishId)
            .eq(Dish::getStoreId, storeId)
            .eq(Dish::getDeleted, 0)
            .last("LIMIT 1"));
        if (dish == null) {
            throw new BizException(2261, "菜品不存在或不属于当前门店");
        }
        return dishPriceHistoryMapper.selectList(new LambdaQueryWrapper<DishPriceHistory>()
                .eq(DishPriceHistory::getDishId, dishId)
                .eq(DishPriceHistory::getStoreId, storeId)
                .orderByDesc(DishPriceHistory::getCreatedAt)
                .orderByDesc(DishPriceHistory::getId))
            .stream()
            .map(history -> new DishPriceHistoryRow(history.getId(), history.getDishId(), history.getOldPrice(), history.getNewPrice(), history.getChangedBy(), history.getReason(), history.getCreatedAt()))
            .toList();
    }

    private Store loadManagedStore(Long managerUserId) {
        Long storeId = resolveManagedStoreId(managerUserId);
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
            .eq(Store::getId, storeId)
            .eq(Store::getDeleted, 0)
            .last("LIMIT 1"));
        if (store == null) {
            throw new BizException(2202, "绑定门店不存在或已停用");
        }
        return store;
    }

    private Long resolveManagedStoreId(Long managerUserId) {
        if (managerUserId == null) {
            throw new BizException(401, "请先登录");
        }
        List<UserStoreRole> roles = userStoreRoleMapper.selectList(new LambdaQueryWrapper<UserStoreRole>()
            .eq(UserStoreRole::getUserId, managerUserId)
            .eq(UserStoreRole::getRoleCode, STORE_MANAGER)
            .eq(UserStoreRole::getStatus, ACTIVE)
            .orderByAsc(UserStoreRole::getId));
        if (roles.isEmpty()) {
            throw new BizException(2201, "当前店长未绑定门店，请联系管理员");
        }
        if (roles.size() > 1) {
            throw new BizException(2207, "当前店长绑定了多个门店，请联系管理员保留一个有效绑定");
        }
        return roles.get(0).getStoreId();
    }

    private LambdaQueryWrapper<FoodOrder> orderQuery(Long storeId, String status, LocalDate from, LocalDate to) {
        LambdaQueryWrapper<FoodOrder> wrapper = new LambdaQueryWrapper<FoodOrder>()
            .eq(FoodOrder::getStoreId, storeId)
            .eq(FoodOrder::getDeleted, 0);
        String normalizedStatus = normalizeOptional(status);
        if (normalizedStatus != null) wrapper.eq(FoodOrder::getStatus, normalizedStatus);
        if (from != null) wrapper.ge(FoodOrder::getCreatedAt, from.atStartOfDay());
        if (to != null) wrapper.lt(FoodOrder::getCreatedAt, to.plusDays(1).atStartOfDay());
        return wrapper;
    }

    private List<ManagerReservationRow> toReservationRows(List<Reservation> reservations) {
        if (reservations.isEmpty()) return List.of();
        Map<Long, DiningTable> tableMap = selectByIds(reservations.stream().map(Reservation::getTableId).collect(Collectors.toSet()), diningTableMapper::selectBatchIds, DiningTable::getId);
        Map<Long, ReservationSlot> slotMap = selectByIds(reservations.stream().map(Reservation::getSlotId).collect(Collectors.toSet()), slotMapper::selectBatchIds, ReservationSlot::getId);
        return reservations.stream().map(reservation -> toReservationRow(reservation, tableMap.get(reservation.getTableId()), slotMap.get(reservation.getSlotId()))).toList();
    }

    private List<ManagerOrderRow> toOrderRows(List<FoodOrder> orders) {
        if (orders.isEmpty()) return List.of();
        Map<Long, User> userMap = selectByIds(orders.stream().map(FoodOrder::getUserId).collect(Collectors.toSet()), userMapper::selectBatchIds, User::getId);
        Map<Long, DiningTable> tableMap = selectByIds(orders.stream().map(FoodOrder::getTableId).collect(Collectors.toSet()), diningTableMapper::selectBatchIds, DiningTable::getId);
        Map<Long, Reservation> reservationMap = selectByIds(orders.stream().map(FoodOrder::getReservationId).collect(Collectors.toSet()), reservationMapper::selectBatchIds, Reservation::getId);
        List<FoodOrderItem> items = foodOrderItemMapper.selectList(new LambdaQueryWrapper<FoodOrderItem>()
            .in(FoodOrderItem::getOrderId, orders.stream().map(FoodOrder::getId).toList())
            .orderByAsc(FoodOrderItem::getId));
        Map<Long, String> summaryMap = items.stream().collect(Collectors.groupingBy(FoodOrderItem::getOrderId,
            Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                .sorted(Comparator.comparing(FoodOrderItem::getId))
                .limit(3)
                .map(item -> item.getDishName() + "×" + item.getQuantity())
                .collect(Collectors.joining("、")))));
        return orders.stream().map(order -> {
            User user = userMap.get(order.getUserId());
            DiningTable table = tableMap.get(order.getTableId());
            Reservation reservation = reservationMap.get(order.getReservationId());
            return new ManagerOrderRow(order.getId(), order.getOrderNo(), user == null ? "散客" : user.getNickname(),
                table == null ? null : table.getTableNo(), reservation == null ? null : reservation.getReservationNo(),
                order.getTotalAmount(), order.getStatus(), order.getRefundStatus(), order.getPaidAt(), order.getCompletedAt(),
                order.getCreatedAt(), summaryMap.getOrDefault(order.getId(), ""));
        }).toList();
    }

    private List<ManagerShiftRow> toShiftRows(List<StaffShift> shifts) {
        if (shifts.isEmpty()) return List.of();
        Map<Long, User> userMap = selectByIds(shifts.stream().map(StaffShift::getUserId).collect(Collectors.toSet()), userMapper::selectBatchIds, User::getId);
        return shifts.stream().map(shift -> {
            User user = userMap.get(shift.getUserId());
            return new ManagerShiftRow(shift.getId(), shift.getUserId(), user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(), shift.getRoleCode(), shift.getShiftDate(), shift.getStartTime(),
                shift.getEndTime(), shift.getStatus(), shift.getRemark());
        }).toList();
    }

    private <T> Map<Long, T> selectByIds(Collection<Long> ids, Function<Collection<Long>, List<T>> selector, Function<T, Long> idGetter) {
        List<Long> normalizedIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedIds.isEmpty()) return Map.of();
        return selector.apply(normalizedIds).stream().collect(Collectors.toMap(idGetter, Function.identity(), (first, ignored) -> first));
    }

    private ManagerStoreInfo toStoreInfo(Store store) {
        return new ManagerStoreInfo(store.getId(), store.getName(), store.getCity(), store.getAddress(), store.getPhone(),
            store.getOpeningTime(), store.getClosingTime(), store.getStatus(), store.getDescription());
    }

    private ManagerTableRow toTableRow(DiningTable table) {
        return new ManagerTableRow(table.getId(), table.getTableNo(), table.getCapacity(), table.getArea(), table.getStatus());
    }

    private ManagerReservationRow toReservationRow(Reservation reservation, DiningTable table, ReservationSlot slot) {
        return new ManagerReservationRow(reservation.getId(), reservation.getReservationNo(), reservation.getContactName(),
            reservation.getContactPhone(), reservation.getPartySize(), table == null ? "未知桌位" : table.getTableNo(),
            formatSlotTime(slot), reservation.getStatus(), reservation.getRemark(), reservation.getCreatedAt());
    }

    private String formatSlotTime(ReservationSlot slot) {
        if (slot == null || slot.getSlotDate() == null || slot.getStartTime() == null) return "未知时段";
        return SLOT_TIME_FORMATTER.format(slot.getSlotDate().atTime(slot.getStartTime()));
    }

    private UserStoreRole loadStoreRole(Long storeId, Long userStoreRoleId) {
        UserStoreRole role = userStoreRoleMapper.selectOne(new LambdaQueryWrapper<UserStoreRole>()
            .eq(UserStoreRole::getId, userStoreRoleId)
            .eq(UserStoreRole::getStoreId, storeId)
            .last("LIMIT 1"));
        if (role == null) throw new BizException(2240, "员工关系不存在或不属于当前门店");
        return role;
    }

    private UserStoreRole loadActiveStaffRole(Long storeId, Long userId) {
        if (userId == null) throw new BizException(400, "请选择员工");
        UserStoreRole role = userStoreRoleMapper.selectOne(new LambdaQueryWrapper<UserStoreRole>()
            .eq(UserStoreRole::getStoreId, storeId)
            .eq(UserStoreRole::getUserId, userId)
            .ne(UserStoreRole::getRoleCode, STORE_MANAGER)
            .ne(UserStoreRole::getStatus, DISMISSED)
            .last("LIMIT 1"));
        if (role == null) throw new BizException(2240, "员工不存在、已离职或不属于当前门店");
        return role;
    }

    private void validateStoreRequest(UpdateManagerStoreRequest request) {
        if (request == null) throw new BizException(2204, "门店信息不能为空");
        validateLength(normalizeRequired(request.name(), "门店名称不能为空"), 128, "门店名称不能超过 128 个字符");
        validateLength(normalizeRequired(request.city(), "城市不能为空"), 64, "城市不能超过 64 个字符");
        validateLength(normalizeRequired(request.address(), "地址不能为空"), 255, "地址不能超过 255 个字符");
        validateOptionalLength(request.phone(), 32, "电话不能超过 32 个字符");
        validateOptionalLength(request.description(), 1000, "门店介绍不能超过 1000 个字符");
        if (request.openingTime() == null || request.closingTime() == null) throw new BizException(2205, "营业时间不能为空");
        if (!request.openingTime().isBefore(request.closingTime())) throw new BizException(2206, "开始营业时间必须早于结束营业时间");
    }

    private String validateTablePayload(ManagerTableRow payload) {
        if (payload == null) throw new BizException(2213, "桌位信息不能为空");
        validateLength(normalizeRequired(payload.tableNo(), "桌号不能为空"), 32, "桌号不能超过 32 个字符");
        if (payload.capacity() == null || payload.capacity() <= 0) throw new BizException(2214, "容量必须大于 0");
        if (payload.capacity() > 20) throw new BizException(2215, "容量不能超过 20 人");
        validateOptionalLength(payload.area(), 64, "区域不能超过 64 个字符");
        String status = normalizeRequired(payload.status(), "桌位状态不能为空").toUpperCase();
        if (!TABLE_STATUSES.contains(status)) throw new BizException(2216, "不支持的桌位状态");
        return UNAVAILABLE.equals(status) ? DISABLED : status;
    }

    private void validateLeaveRequest(GrantLeaveRequest request) {
        if (request == null) throw new BizException(2245, "请假信息不能为空");
        normalizeRequired(request.leaveType(), "请假类型不能为空");
        if (request.startDate() == null || request.endDate() == null) throw new BizException(2246, "请假日期不能为空");
        if (request.startDate().isAfter(request.endDate())) throw new BizException(2247, "请假开始日期不能晚于结束日期");
        validateOptionalLength(request.reason(), 500, "请假原因不能超过 500 个字符");
    }

    private void validateShiftRequest(ManagerShiftRequest request) {
        if (request == null) throw new BizException(2248, "排班信息不能为空");
        if (request.userId() == null) throw new BizException(400, "请选择员工");
        if (request.shiftDate() == null || request.startTime() == null || request.endTime() == null) throw new BizException(2249, "排班日期和时间不能为空");
        if (!request.startTime().isBefore(request.endTime())) throw new BizException(2249, "排班开始时间必须早于结束时间");
        String status = normalizeRequired(request.status(), "排班状态不能为空").toUpperCase();
        if (!SHIFT_STATUSES.contains(status)) throw new BizException(2249, "不支持的排班状态");
        validateOptionalLength(request.remark(), 255, "排班备注不能超过 255 个字符");
    }

    private void ensureTableNoAvailable(Long storeId, String tableNo, Long ignoredId) {
        DiningTable existing = diningTableMapper.selectOne(new LambdaQueryWrapper<DiningTable>()
            .eq(DiningTable::getStoreId, storeId)
            .eq(DiningTable::getTableNo, tableNo.trim())
            .eq(DiningTable::getDeleted, 0)
            .last("LIMIT 1"));
        if (existing != null && !existing.getId().equals(ignoredId)) throw new BizException(2210, "桌号已存在");
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new BizException(400, message);
        return value.trim();
    }

    private String normalizeOptional(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateLength(String value, int maxLength, String message) {
        if (value.length() > maxLength) throw new BizException(400, message);
    }

    private void validateOptionalLength(String value, int maxLength, String message) {
        String normalized = normalizeOptional(value);
        if (normalized != null && normalized.length() > maxLength) throw new BizException(400, message);
    }
}
