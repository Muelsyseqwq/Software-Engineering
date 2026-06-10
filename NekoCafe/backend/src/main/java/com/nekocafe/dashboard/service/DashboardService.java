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
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.user.entity.User;
import com.nekocafe.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ReservationMapper reservationMapper;
    private final FoodOrderMapper foodOrderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final UserMapper userMapper;
    private final StoreMapper storeMapper;
    private final CatMapper catMapper;

    public DashboardService(ReservationMapper reservationMapper, FoodOrderMapper foodOrderMapper,
                            PaymentRecordMapper paymentRecordMapper, UserMapper userMapper,
                            StoreMapper storeMapper, CatMapper catMapper) {
        this.reservationMapper = reservationMapper;
        this.foodOrderMapper = foodOrderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.userMapper = userMapper;
        this.storeMapper = storeMapper;
        this.catMapper = catMapper;
    }

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

        return buildTrend(start, maps, "stat_date", "total", true);
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

        return buildTrend(start, maps, "stat_date", "cnt", false);
    }

    // ---- store-level summaries ----

    public List<StoreSummaryRow> storeSummaries() {
        // Per-store revenue via JOIN payment_record + food_order
        List<Map<String, Object>> revenueRows = paymentRecordMapper.selectStoreRevenueToday();

        // Per-store order count today
        QueryWrapper<FoodOrder> orderWrapper = new QueryWrapper<>();
        orderWrapper.select("store_id, COUNT(*) AS cnt")
                .eq("deleted", 0)
                .apply("DATE(created_at) = CURDATE()")
                .groupBy("store_id");
        List<Map<String, Object>> orderRows = foodOrderMapper.selectMaps(orderWrapper);

        // Per-store reservation count today
        QueryWrapper<Reservation> resvWrapper = new QueryWrapper<>();
        resvWrapper.select("store_id, COUNT(*) AS cnt")
                .eq("deleted", 0)
                .apply("DATE(created_at) = CURDATE()")
                .groupBy("store_id");
        List<Map<String, Object>> resvRows = reservationMapper.selectMaps(resvWrapper);

        // Build maps
        Map<Long, BigDecimal> revMap = buildLongBigDecimalMap(revenueRows);
        Map<Long, Integer> orderCntMap = buildLongIntMap(orderRows);
        Map<Long, Integer> resvCntMap = buildLongIntMap(resvRows);

        // All non-deleted stores
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

        return buildTrend(start, maps, "stat_date", "total", true);
    }

    // ---- helpers ----

    private List<DashboardTrendPoint> buildTrend(LocalDate start,
                                                  List<Map<String, Object>> maps,
                                                  String dateKey, String valueKey,
                                                  boolean asInt) {
        List<DashboardTrendPoint> result = new ArrayList<>();
        java.math.BigDecimal[] valuesArr = new java.math.BigDecimal[7];
        int[] intArr = new int[7];
        for (Map<String, Object> row : maps) {
            LocalDate date = null;
            if (row.get(dateKey) instanceof java.sql.Date sqlDate) {
                date = sqlDate.toLocalDate();
            }
            if (date == null) continue;
            int idx = (int) (date.toEpochDay() - start.toEpochDay());
            if (idx >= 0 && idx < 7) {
                Object val = row.get(valueKey);
                if (asInt) {
                    if (val instanceof java.math.BigDecimal bd) valuesArr[idx] = bd;
                    else if (val instanceof Number n)
                        valuesArr[idx] = java.math.BigDecimal.valueOf(n.doubleValue());
                    if (valuesArr[idx] == null) valuesArr[idx] = java.math.BigDecimal.ZERO;
                } else {
                    if (val instanceof Number n) intArr[idx] = n.intValue();
                }
            }
        }
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            String label = dow.getDisplayName(TextStyle.SHORT, Locale.CHINESE);
            if (asInt) {
                BigDecimal bd = valuesArr[i] != null ? valuesArr[i] : BigDecimal.ZERO;
                result.add(new DashboardTrendPoint(label, bd.intValue()));
            } else {
                result.add(new DashboardTrendPoint(label, intArr[i]));
            }
        }
        return result;
    }

    private Map<Long, BigDecimal> buildLongBigDecimalMap(List<Map<String, Object>> rows) {
        return rows.stream()
                .filter(row -> row.get("store_id") instanceof Number)
                .collect(java.util.stream.Collectors.toMap(
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
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((Number) row.get("store_id")).longValue(),
                        row -> row.get("cnt") instanceof Number n ? n.intValue() : 0,
                        (a, b) -> a));
    }

    // ---- DTOs ----

    public record DashboardSummary(int reservationCount, int orderCount, BigDecimal revenue,
                                   int userCount, int storeCount, int catCount) {}

    public record DashboardTrendPoint(String label, int value) {}

    public record StoreSummaryRow(Long storeId, String storeName, String city, String status,
                                  BigDecimal revenue, int orderCount, int reservationCount) {}
}
