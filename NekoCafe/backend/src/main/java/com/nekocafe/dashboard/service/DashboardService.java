package com.nekocafe.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nekocafe.cat.entity.Cat;
import com.nekocafe.cat.mapper.CatMapper;
import com.nekocafe.order.entity.FoodOrder;
import com.nekocafe.order.mapper.FoodOrderMapper;
import com.nekocafe.payment.entity.PaymentRecord;
import com.nekocafe.payment.mapper.PaymentRecordMapper;
import com.nekocafe.reservation.entity.Reservation;
import com.nekocafe.reservation.mapper.ReservationMapper;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.user.entity.User;
import com.nekocafe.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ReservationMapper reservationMapper;
    private final FoodOrderMapper foodOrderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final UserMapper userMapper;
    private final StoreMapper storeMapper;
    private final CatMapper catMapper;
    private final DiningTableMapper diningTableMapper;

    public DashboardService(ReservationMapper reservationMapper, FoodOrderMapper foodOrderMapper,
                            PaymentRecordMapper paymentRecordMapper, UserMapper userMapper,
                            StoreMapper storeMapper, CatMapper catMapper,
                            DiningTableMapper diningTableMapper) {
        this.reservationMapper = reservationMapper;
        this.foodOrderMapper = foodOrderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.userMapper = userMapper;
        this.storeMapper = storeMapper;
        this.catMapper = catMapper;
        this.diningTableMapper = diningTableMapper;
    }

    // ==================== existing methods ====================

    public DashboardSummary summary() {
        long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDeleted, 0)
                        .apply("DATE(created_at) = CURDATE()")
        );

        long orderCount = foodOrderMapper.selectCount(
                new LambdaQueryWrapper<FoodOrder>()
                        .eq(FoodOrder::getDeleted, 0)
                        .apply("DATE(created_at) = CURDATE()")
        );

        QueryWrapper<PaymentRecord> revenueWrapper = new QueryWrapper<>();
        revenueWrapper.select("COALESCE(SUM(amount), 0) AS total")
                .eq("status", "SUCCESS")
                .apply("DATE(paid_at) = CURDATE()");
        List<Map<String, Object>> revenueMaps = paymentRecordMapper.selectMaps(revenueWrapper);
        BigDecimal revenue = BigDecimal.ZERO;
        if (!revenueMaps.isEmpty() && revenueMaps.get(0) != null) {
            Object total = revenueMaps.get(0).get("total");
            if (total instanceof BigDecimal bd) {
                revenue = bd;
            } else if (total instanceof Number n) {
                revenue = BigDecimal.valueOf(n.doubleValue());
            }
        }

        long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)
        );

        long storeCount = storeMapper.selectCount(
                new LambdaQueryWrapper<Store>().eq(Store::getDeleted, 0)
        );

        long catCount = 0;
        try {
            catCount = catMapper.selectCount(
                    new LambdaQueryWrapper<Cat>().eq(Cat::getDeleted, 0)
            );
        } catch (Exception e) {
            log.warn("Failed to query cat count (table may not exist): {}", e.getMessage());
        }

        return new DashboardSummary(
                (int) reservationCount,
                (int) orderCount,
                revenue,
                (int) userCount,
                (int) storeCount,
                (int) catCount
        );
    }

    public List<DashboardTrendPoint> revenue() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);

        QueryWrapper<PaymentRecord> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(paid_at) AS stat_date, COALESCE(SUM(amount), 0) AS total")
                .eq("status", "SUCCESS")
                .apply("DATE(paid_at) BETWEEN {0} AND {1}", start.toString(), end.toString())
                .groupBy("DATE(paid_at)")
                .orderByAsc("stat_date");
        List<Map<String, Object>> maps = paymentRecordMapper.selectMaps(wrapper);

        return buildTrend7(start, maps, "stat_date", "total", true);
    }

    public List<DashboardTrendPoint> reservations() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);

        QueryWrapper<Reservation> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(created_at) AS stat_date, COUNT(*) AS cnt")
                .eq("deleted", 0)
                .apply("DATE(created_at) BETWEEN {0} AND {1}", start.toString(), end.toString())
                .groupBy("DATE(created_at)")
                .orderByAsc("stat_date");
        List<Map<String, Object>> maps = reservationMapper.selectMaps(wrapper);

        return buildTrend7(start, maps, "stat_date", "cnt", false);
    }

    public List<StoreSummaryRow> storeSummaries() {
        List<Map<String, Object>> revenueRows = paymentRecordMapper.selectStoreRevenueToday();

        QueryWrapper<FoodOrder> orderWrapper = new QueryWrapper<>();
        orderWrapper.select("store_id, COUNT(*) AS cnt")
                .eq("deleted", 0)
                .apply("DATE(created_at) = CURDATE()")
                .groupBy("store_id");
        List<Map<String, Object>> orderRows = foodOrderMapper.selectMaps(orderWrapper);

        QueryWrapper<Reservation> resvWrapper = new QueryWrapper<>();
        resvWrapper.select("store_id, COUNT(*) AS cnt")
                .eq("deleted", 0)
                .apply("DATE(created_at) = CURDATE()")
                .groupBy("store_id");
        List<Map<String, Object>> resvRows = reservationMapper.selectMaps(resvWrapper);

        Map<Long, BigDecimal> revMap = buildLongBigDecimalMap(revenueRows);
        Map<Long, Integer> orderCntMap = buildLongIntMap(orderRows);
        Map<Long, Integer> resvCntMap = buildLongIntMap(resvRows);

        List<Store> stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getDeleted, 0).orderByAsc(Store::getId));

        return stores.stream().map(s -> {
            long sid = s.getId();
            return new StoreSummaryRow(sid, s.getName(), s.getCity(), s.getStatus(),
                    revMap.getOrDefault(sid, BigDecimal.ZERO),
                    orderCntMap.getOrDefault(sid, 0),
                    resvCntMap.getOrDefault(sid, 0));
        }).toList();
    }

    public List<DashboardTrendPoint> storeRevenue(Long storeId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);

        List<Map<String, Object>> maps = paymentRecordMapper.selectStoreRevenueByDateRange(
                storeId, start.toString(), end.toString());

        return buildTrend7(start, maps, "stat_date", "total", true);
    }

    // ==================== new: period-aware KPI summary ====================

    public DashboardPeriodSummary periodSummary(String period, Long storeId) {
        PeriodRange range = resolvePeriod(period);
        LocalDateTime startAt = range.start.atStartOfDay();
        LocalDateTime endAt = range.end.plusDays(1).atStartOfDay();

        BigDecimal revenue = queryRevenue(startAt, endAt, storeId);

        // orders
        LambdaQueryWrapper<FoodOrder> orderW = new LambdaQueryWrapper<FoodOrder>()
                .eq(FoodOrder::getDeleted, 0)
                .ge(FoodOrder::getCreatedAt, startAt)
                .lt(FoodOrder::getCreatedAt, endAt);
        if (storeId != null) orderW.eq(FoodOrder::getStoreId, storeId);
        long orderCount = foodOrderMapper.selectCount(orderW);
        long paidOrderCount = foodOrderMapper.selectCount(orderW.clone().in(FoodOrder::getStatus,
                List.of("PAID", "PREPARING", "COMPLETED")));

        // reservations
        LambdaQueryWrapper<Reservation> resvW = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getDeleted, 0)
                .ge(Reservation::getCreatedAt, startAt)
                .lt(Reservation::getCreatedAt, endAt);
        if (storeId != null) resvW.eq(Reservation::getStoreId, storeId);
        long reservationCount = reservationMapper.selectCount(resvW);
        long checkedInCount = reservationMapper.selectCount(resvW.clone()
                .in(Reservation::getStatus, List.of("CHECKED_IN", "COMPLETED")));

        // tables & area
        long tableCount = storeId != null ? countTablesForStore(storeId) : countAllTables();
        BigDecimal area = getTotalArea(storeId);
        long days = Math.max(1, ChronoUnit.DAYS.between(range.start, range.end) + 1);

        BigDecimal revenuePerSqm = area.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : revenue.divide(area, 2, RoundingMode.HALF_UP);

        BigDecimal turnoverRate = tableCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(checkedInCount)
                .divide(BigDecimal.valueOf(tableCount * days), 2, RoundingMode.HALF_UP);

        BigDecimal repurchaseRate = calcRepurchaseRate(startAt, endAt, storeId);

        BigDecimal avgOrderValue = paidOrderCount == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(paidOrderCount), 2, RoundingMode.HALF_UP);

        return new DashboardPeriodSummary(range.start, range.end, revenue, orderCount, paidOrderCount,
                reservationCount, checkedInCount, tableCount, area, revenuePerSqm, turnoverRate,
                repurchaseRate, avgOrderValue);
    }

    // ==================== new: per-store metrics ====================

    public StoreMetrics storeMetrics(Long storeId, String period) {
        DashboardPeriodSummary summary = periodSummary(period, storeId);
        Store store = storeMapper.selectById(storeId);
        String storeName = store != null ? store.getName() : "未知门店";
        return new StoreMetrics(storeId, storeName, summary.revenue, summary.orderCount,
                summary.reservationCount, summary.area, summary.revenuePerSqm, summary.turnoverRate,
                summary.repurchaseRate, summary.start, summary.end);
    }

    // ==================== new: cross-store comparison ====================

    public List<CrossStoreRow> crossStore(String period) {
        PeriodRange range = resolvePeriod(period);
        LocalDateTime startAt = range.start.atStartOfDay();
        LocalDateTime endAt = range.end.plusDays(1).atStartOfDay();
        long days = Math.max(1, ChronoUnit.DAYS.between(range.start, range.end) + 1);

        List<Store> stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getDeleted, 0).orderByAsc(Store::getId));
        if (stores.isEmpty()) return List.of();

        return stores.stream().map(store -> {
            long sid = store.getId();
            BigDecimal revenue = queryRevenue(startAt, endAt, sid);

            long orderCount = foodOrderMapper.selectCount(new LambdaQueryWrapper<FoodOrder>()
                    .eq(FoodOrder::getDeleted, 0).eq(FoodOrder::getStoreId, sid)
                    .ge(FoodOrder::getCreatedAt, startAt).lt(FoodOrder::getCreatedAt, endAt));

            LambdaQueryWrapper<Reservation> baseRw = new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getDeleted, 0).eq(Reservation::getStoreId, sid)
                    .ge(Reservation::getCreatedAt, startAt).lt(Reservation::getCreatedAt, endAt);
            long reservationCount = reservationMapper.selectCount(baseRw);
            long checkedInCount = reservationMapper.selectCount(baseRw.clone()
                    .in(Reservation::getStatus, List.of("CHECKED_IN", "COMPLETED")));

            long tableCount = countTablesForStore(sid);
            BigDecimal area = store.getAreaSquareMeter() != null ? store.getAreaSquareMeter() : BigDecimal.ZERO;

            BigDecimal revenuePerSqm = area.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO
                    : revenue.divide(area, 2, RoundingMode.HALF_UP);

            BigDecimal turnoverRate = tableCount == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(checkedInCount)
                    .divide(BigDecimal.valueOf(tableCount * days), 2, RoundingMode.HALF_UP);

            BigDecimal repurchaseRate = calcRepurchaseRate(startAt, endAt, sid);

            return new CrossStoreRow(sid, store.getName(), store.getCity(), store.getStatus(),
                    revenue, orderCount, reservationCount, revenuePerSqm, turnoverRate,
                    repurchaseRate, area);
        }).toList();
    }

    // ==================== new: operator trend for charts ====================

    public List<DashboardTrendPoint> operatorTrend(String period, String metric, Long storeId) {
        PeriodRange range = resolvePeriod(period);
        boolean monthly = "YEAR".equals(period) || "ALL".equals(period);

        if (monthly) {
            return buildMonthlyTrend(range, storeId, metric);
        }
        return buildDailyTrend(range, storeId, metric);
    }

    // ==================== daily trend builders ====================

    private List<DashboardTrendPoint> buildDailyTrend(PeriodRange range, Long storeId, String metric) {
        LocalDate start = range.start;
        LocalDate end = range.end;
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay();

        return switch (metric) {
            case "REVENUE" -> {
                List<Map<String, Object>> maps = paymentRecordMapper.selectStoreRevenueByDateRangeAll(
                        storeId, start.toString(), end.toString());
                yield buildTrendN(start, end, maps, "stat_date", "total", null);
            }
            case "ORDERS" -> {
                LambdaQueryWrapper<FoodOrder> w = new LambdaQueryWrapper<FoodOrder>()
                        .eq(FoodOrder::getDeleted, 0)
                        .ge(FoodOrder::getCreatedAt, startAt).lt(FoodOrder::getCreatedAt, endAt);
                if (storeId != null) w.eq(FoodOrder::getStoreId, storeId);
                yield aggregateDailyCount(start, end, foodOrderMapper.selectList(w));
            }
            case "RESERVATIONS" -> {
                LambdaQueryWrapper<Reservation> w = new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDeleted, 0)
                        .ge(Reservation::getCreatedAt, startAt).lt(Reservation::getCreatedAt, endAt);
                if (storeId != null) w.eq(Reservation::getStoreId, storeId);
                yield aggregateDailyCount(start, end, reservationMapper.selectList(w));
            }
            case "REVENUE_PER_SQM" -> {
                BigDecimal area = getTotalArea(storeId);
                if (area.compareTo(BigDecimal.ZERO) <= 0) {
                    yield emptyDailySeries(start, end);
                }
                List<Map<String, Object>> maps = paymentRecordMapper.selectStoreRevenueByDateRangeAll(
                        storeId, start.toString(), end.toString());
                yield buildTrendN(start, end, maps, "stat_date", "total", area);
            }
            case "TURNOVER_RATE" -> buildDailyTurnoverTrend(start, end, storeId);
            case "REPURCHASE_RATE" -> buildDailyRepurchaseTrend(start, end, storeId);
            default -> List.of();
        };
    }

    private List<DashboardTrendPoint> buildMonthlyTrend(PeriodRange range, Long storeId, String metric) {
        List<DashboardTrendPoint> results = new ArrayList<>();
        YearMonth ym = YearMonth.from(range.start);
        YearMonth ymEnd = YearMonth.from(range.end);

        while (!ym.isAfter(ymEnd)) {
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();
            LocalDateTime ms = monthStart.atStartOfDay();
            LocalDateTime me = monthEnd.plusDays(1).atStartOfDay();

            String label = ym.getMonthValue() + "月";

            switch (metric) {
                case "REVENUE" -> {
                    BigDecimal v = queryRevenue(ms, me, storeId);
                    results.add(new DashboardTrendPoint(label, v.intValue()));
                }
                case "ORDERS" -> {
                    var w = new LambdaQueryWrapper<FoodOrder>()
                            .eq(FoodOrder::getDeleted, 0)
                            .ge(FoodOrder::getCreatedAt, ms).lt(FoodOrder::getCreatedAt, me);
                    if (storeId != null) w.eq(FoodOrder::getStoreId, storeId);
                    results.add(new DashboardTrendPoint(label, foodOrderMapper.selectCount(w).intValue()));
                }
                case "RESERVATIONS" -> {
                    var w = new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getDeleted, 0)
                            .ge(Reservation::getCreatedAt, ms).lt(Reservation::getCreatedAt, me);
                    if (storeId != null) w.eq(Reservation::getStoreId, storeId);
                    results.add(new DashboardTrendPoint(label, reservationMapper.selectCount(w).intValue()));
                }
                case "REVENUE_PER_SQM" -> {
                    BigDecimal rev = queryRevenue(ms, me, storeId);
                    BigDecimal area = getTotalArea(storeId);
                    BigDecimal v = area.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO
                            : rev.divide(area, 2, RoundingMode.HALF_UP);
                    results.add(new DashboardTrendPoint(label, v.intValue()));
                }
                case "TURNOVER_RATE" -> {
                    var rw = new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getDeleted, 0)
                            .in(Reservation::getStatus, List.of("CHECKED_IN", "COMPLETED"))
                            .ge(Reservation::getCreatedAt, ms).lt(Reservation::getCreatedAt, me);
                    if (storeId != null) rw.eq(Reservation::getStoreId, storeId);
                    long ci = reservationMapper.selectCount(rw);
                    long tables = storeId != null ? countTablesForStore(storeId) : countAllTables();
                    BigDecimal tr = tables == 0 ? BigDecimal.ZERO
                            : BigDecimal.valueOf(ci).divide(
                                    BigDecimal.valueOf(tables * ym.lengthOfMonth()), 2, RoundingMode.HALF_UP);
                    results.add(new DashboardTrendPoint(label, tr.multiply(BigDecimal.valueOf(100)).intValue()));
                }
                case "REPURCHASE_RATE" -> {
                    BigDecimal rr = calcRepurchaseRate(ms, me, storeId);
                    results.add(new DashboardTrendPoint(label, rr.multiply(BigDecimal.valueOf(100)).intValue()));
                }
            }
            ym = ym.plusMonths(1);
        }
        return results;
    }

    private List<DashboardTrendPoint> buildDailyTurnoverTrend(LocalDate start, LocalDate end, Long storeId) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        long tableCount = storeId != null ? countTablesForStore(storeId) : countAllTables();

        var rw = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getDeleted, 0)
                .in(Reservation::getStatus, List.of("CHECKED_IN", "COMPLETED"))
                .ge(Reservation::getCreatedAt, start.atStartOfDay())
                .lt(Reservation::getCreatedAt, end.plusDays(1).atStartOfDay());
        if (storeId != null) rw.eq(Reservation::getStoreId, storeId);
        List<Reservation> resvs = reservationMapper.selectList(rw);

        int[] counts = new int[(int) days];
        for (Reservation r : resvs) {
            if (r.getCreatedAt() != null) {
                int idx = (int) ChronoUnit.DAYS.between(start, r.getCreatedAt().toLocalDate());
                if (idx >= 0 && idx < days) counts[idx]++;
            }
        }

        List<DashboardTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            String label = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINESE);
            int value = tableCount == 0 ? 0
                    : BigDecimal.valueOf(counts[i])
                    .divide(BigDecimal.valueOf(tableCount), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
            result.add(new DashboardTrendPoint(label, value));
        }
        return result;
    }

    private List<DashboardTrendPoint> buildDailyRepurchaseTrend(LocalDate start, LocalDate end, Long storeId) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        List<DashboardTrendPoint> result = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();

            var ow = new LambdaQueryWrapper<FoodOrder>()
                    .eq(FoodOrder::getDeleted, 0)
                    .ge(FoodOrder::getCreatedAt, dayStart).lt(FoodOrder::getCreatedAt, dayEnd);
            if (storeId != null) ow.eq(FoodOrder::getStoreId, storeId);
            List<FoodOrder> dayOrders = foodOrderMapper.selectList(ow);

            Set<Long> userIds = dayOrders.stream()
                    .map(FoodOrder::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            String label = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINESE);

            if (userIds.isEmpty()) {
                result.add(new DashboardTrendPoint(label, 0));
                continue;
            }

            long repurchase = 0;
            for (Long uid : userIds) {
                var pw = new LambdaQueryWrapper<FoodOrder>()
                        .eq(FoodOrder::getUserId, uid)
                        .eq(FoodOrder::getDeleted, 0)
                        .lt(FoodOrder::getCreatedAt, dayStart);
                if (storeId != null) pw.eq(FoodOrder::getStoreId, storeId);
                if (foodOrderMapper.selectCount(pw) > 0) repurchase++;
            }
            int pct = BigDecimal.valueOf(repurchase)
                    .divide(BigDecimal.valueOf(userIds.size()), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
            result.add(new DashboardTrendPoint(label, pct));
        }
        return result;
    }

    // ==================== helpers ====================

    private PeriodRange resolvePeriod(String period) {
        LocalDate end = LocalDate.now();
        String p = period != null ? period.toUpperCase() : "WEEK";
        LocalDate start = switch (p) {
            case "MONTH" -> end.minusDays(29);
            case "YEAR" -> end.minusDays(364);
            case "ALL" -> findEarliestDate();
            default -> end.minusDays(6); // WEEK
        };
        return new PeriodRange(start, end);
    }

    private LocalDate findEarliestDate() {
        var orders = foodOrderMapper.selectList(new LambdaQueryWrapper<FoodOrder>()
                .eq(FoodOrder::getDeleted, 0)
                .orderByAsc(FoodOrder::getCreatedAt)
                .last("LIMIT 1"));
        if (!orders.isEmpty() && orders.get(0).getCreatedAt() != null) {
            return orders.get(0).getCreatedAt().toLocalDate();
        }
        return LocalDate.now().minusYears(1);
    }

    private BigDecimal queryRevenue(LocalDateTime startAt, LocalDateTime endAt, Long storeId) {
        String storeFilter = storeId != null ? " AND store_id = " + storeId : "";
        List<Map<String, Object>> maps = paymentRecordMapper.selectMaps(
                new QueryWrapper<PaymentRecord>()
                        .select("COALESCE(SUM(amount), 0) AS total")
                        .eq("status", "SUCCESS")
                        .ge("paid_at", startAt)
                        .lt("paid_at", endAt)
                        .apply("order_id IN (SELECT id FROM food_order WHERE deleted = 0" + storeFilter + ")")
        );
        if (!maps.isEmpty() && maps.get(0) != null) {
            Object total = maps.get(0).get("total");
            if (total instanceof BigDecimal bd) return bd;
            if (total instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calcRepurchaseRate(LocalDateTime startAt, LocalDateTime endAt, Long storeId) {
        var ow = new LambdaQueryWrapper<FoodOrder>()
                .eq(FoodOrder::getDeleted, 0)
                .ge(FoodOrder::getCreatedAt, startAt).lt(FoodOrder::getCreatedAt, endAt);
        if (storeId != null) ow.eq(FoodOrder::getStoreId, storeId);
        List<FoodOrder> periodOrders = foodOrderMapper.selectList(ow);

        Set<Long> activeUserIds = periodOrders.stream()
                .map(FoodOrder::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (activeUserIds.isEmpty()) return BigDecimal.ZERO;

        long repurchase = 0;
        for (Long uid : activeUserIds) {
            var pw = new LambdaQueryWrapper<FoodOrder>()
                    .eq(FoodOrder::getUserId, uid)
                    .eq(FoodOrder::getDeleted, 0)
                    .lt(FoodOrder::getCreatedAt, startAt);
            if (storeId != null) pw.eq(FoodOrder::getStoreId, storeId);
            if (foodOrderMapper.selectCount(pw) > 0) repurchase++;
        }

        return BigDecimal.valueOf(repurchase)
                .divide(BigDecimal.valueOf(activeUserIds.size()), 2, RoundingMode.HALF_UP);
    }

    private long countTablesForStore(Long storeId) {
        return diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getStoreId, storeId)
                .eq(DiningTable::getDeleted, 0));
    }

    private long countAllTables() {
        return diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getDeleted, 0));
    }

    private BigDecimal getTotalArea(Long storeId) {
        if (storeId != null) {
            Store s = storeMapper.selectById(storeId);
            return s != null && s.getAreaSquareMeter() != null ? s.getAreaSquareMeter() : BigDecimal.ZERO;
        }
        List<Store> stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getDeleted, 0));
        return stores.stream()
                .map(s -> s.getAreaSquareMeter() != null ? s.getAreaSquareMeter() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---- generic trend builders ----

    /**
     * Build 7-day fixed trend (used by legacy endpoints).
     */
    private List<DashboardTrendPoint> buildTrend7(LocalDate start, List<Map<String, Object>> maps,
                                                   String dateKey, String valueKey, boolean asInt) {
        List<DashboardTrendPoint> result = new ArrayList<>();
        BigDecimal[] valuesArr = new BigDecimal[7];
        int[] intArr = new int[7];
        for (Map<String, Object> row : maps) {
            LocalDate date = extractDate(row, dateKey);
            if (date == null) continue;
            int idx = (int) (date.toEpochDay() - start.toEpochDay());
            if (idx >= 0 && idx < 7) {
                Object val = row.get(valueKey);
                if (asInt) {
                    if (val instanceof BigDecimal bd) valuesArr[idx] = bd;
                    else if (val instanceof Number n)
                        valuesArr[idx] = BigDecimal.valueOf(n.doubleValue());
                    if (valuesArr[idx] == null) valuesArr[idx] = BigDecimal.ZERO;
                } else {
                    if (val instanceof Number n) intArr[idx] = n.intValue();
                }
            }
        }
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            String label = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINESE);
            if (asInt) {
                BigDecimal bd = valuesArr[i] != null ? valuesArr[i] : BigDecimal.ZERO;
                result.add(new DashboardTrendPoint(label, bd.intValue()));
            } else {
                result.add(new DashboardTrendPoint(label, intArr[i]));
            }
        }
        return result;
    }

    /**
     * Build N-day trend with optional divisor (for per-sqm calculations).
     */
    private List<DashboardTrendPoint> buildTrendN(LocalDate start, LocalDate end,
                                                   List<Map<String, Object>> maps,
                                                   String dateKey, String valueKey,
                                                   BigDecimal divisor) {
        int days = (int) ChronoUnit.DAYS.between(start, end) + 1;
        BigDecimal[] values = new BigDecimal[days];
        for (int i = 0; i < days; i++) values[i] = BigDecimal.ZERO;

        for (Map<String, Object> row : maps) {
            LocalDate date = extractDate(row, dateKey);
            if (date == null) continue;
            int idx = (int) ChronoUnit.DAYS.between(start, date);
            if (idx >= 0 && idx < days) {
                Object val = row.get(valueKey);
                if (val instanceof BigDecimal bd) values[idx] = bd;
                else if (val instanceof Number n)
                    values[idx] = BigDecimal.valueOf(n.doubleValue());
            }
        }

        List<DashboardTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            String label = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINESE);
            if (divisor != null && divisor.compareTo(BigDecimal.ZERO) > 0) {
                values[i] = values[i].divide(divisor, 2, RoundingMode.HALF_UP);
            }
            result.add(new DashboardTrendPoint(label, values[i].intValue()));
        }
        return result;
    }

    private List<DashboardTrendPoint> aggregateDailyCount(LocalDate start, LocalDate end, List<?> records) {
        int days = (int) ChronoUnit.DAYS.between(start, end) + 1;
        int[] counts = new int[days];

        for (Object rec : records) {
            LocalDateTime dt = null;
            if (rec instanceof FoodOrder o) dt = o.getCreatedAt();
            else if (rec instanceof Reservation r) dt = r.getCreatedAt();
            if (dt != null) {
                int idx = (int) ChronoUnit.DAYS.between(start, dt.toLocalDate());
                if (idx >= 0 && idx < days) counts[idx]++;
            }
        }

        List<DashboardTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            String label = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINESE);
            result.add(new DashboardTrendPoint(label, counts[i]));
        }
        return result;
    }

    private List<DashboardTrendPoint> emptyDailySeries(LocalDate start, LocalDate end) {
        int days = (int) ChronoUnit.DAYS.between(start, end) + 1;
        List<DashboardTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            String label = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINESE);
            result.add(new DashboardTrendPoint(label, 0));
        }
        return result;
    }

    private LocalDate extractDate(Map<String, Object> row, String key) {
        if (row.get(key) instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return null;
    }

    private Map<Long, BigDecimal> buildLongBigDecimalMap(List<Map<String, Object>> rows) {
        return rows.stream()
                .filter(row -> row.get("store_id") instanceof Number)
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("store_id")).longValue(),
                        row -> {
                            Object total = row.get("total");
                            if (total instanceof BigDecimal bd) return bd;
                            if (total instanceof Number n)
                                return BigDecimal.valueOf(n.doubleValue());
                            return BigDecimal.ZERO;
                        },
                        (a, b) -> a));
    }

    private Map<Long, Integer> buildLongIntMap(List<Map<String, Object>> rows) {
        return rows.stream()
                .filter(row -> row.get("store_id") instanceof Number)
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("store_id")).longValue(),
                        row -> row.get("cnt") instanceof Number n ? n.intValue() : 0,
                        (a, b) -> a));
    }

    // ==================== DTOs ====================

    public record DashboardSummary(int reservationCount, int orderCount, BigDecimal revenue,
                                   int userCount, int storeCount, int catCount) {}

    public record DashboardTrendPoint(String label, int value) {}

    public record StoreSummaryRow(Long storeId, String storeName, String city, String status,
                                  BigDecimal revenue, int orderCount, int reservationCount) {}

    public record DashboardPeriodSummary(
            LocalDate start, LocalDate end,
            BigDecimal revenue, Long orderCount, Long paidOrderCount,
            Long reservationCount, Long checkedInReservationCount,
            Long tableCount, BigDecimal area,
            BigDecimal revenuePerSqm, BigDecimal turnoverRate,
            BigDecimal repurchaseRate, BigDecimal averageOrderValue) {}

    public record StoreMetrics(
            Long storeId, String storeName,
            BigDecimal revenue, Long orderCount, Long reservationCount,
            BigDecimal areaSquareMeter, BigDecimal revenuePerSqm,
            BigDecimal turnoverRate, BigDecimal repurchaseRate,
            LocalDate periodStart, LocalDate periodEnd) {}

    public record CrossStoreRow(
            Long storeId, String storeName, String city, String status,
            BigDecimal revenue, Long orderCount, Long reservationCount,
            BigDecimal revenuePerSqm, BigDecimal turnoverRate,
            BigDecimal repurchaseRate, BigDecimal areaSquareMeter) {}

    private record PeriodRange(LocalDate start, LocalDate end) {}
}
