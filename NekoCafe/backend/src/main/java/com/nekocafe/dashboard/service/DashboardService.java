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
        // Count today's reservations (non-deleted)
        long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDeleted, 0)
                        .apply("DATE(created_at) = CURDATE()")
        );

        // Count today's orders (non-deleted)
        long orderCount = foodOrderMapper.selectCount(
                new LambdaQueryWrapper<FoodOrder>()
                        .eq(FoodOrder::getDeleted, 0)
                        .apply("DATE(created_at) = CURDATE()")
        );

        // Sum today's revenue from successful payment records
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

        // Count total users (non-deleted)
        long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)
        );

        // Count total stores (non-deleted)
        long storeCount = storeMapper.selectCount(
                new LambdaQueryWrapper<Store>().eq(Store::getDeleted, 0)
        );

        // Count cats (table may not exist yet, fall back to 0)
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

        // Query payment records for last 7 days grouped by date
        QueryWrapper<PaymentRecord> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(paid_at) AS stat_date, COALESCE(SUM(amount), 0) AS total")
                .eq("status", "SUCCESS")
                .apply("DATE(paid_at) BETWEEN {0} AND {1}", start.toString(), end.toString())
                .groupBy("DATE(paid_at)")
                .orderByAsc("stat_date");
        List<Map<String, Object>> maps = paymentRecordMapper.selectMaps(wrapper);

        // Build date -> value map
        BigDecimal[] values = new BigDecimal[7];
        for (int i = 0; i < 7; i++) {
            values[i] = BigDecimal.ZERO;
        }
        for (Map<String, Object> row : maps) {
            if (row.get("stat_date") instanceof java.sql.Date sqlDate) {
                LocalDate date = sqlDate.toLocalDate();
                int idx = (int) (date.toEpochDay() - start.toEpochDay());
                if (idx >= 0 && idx < 7) {
                    Object total = row.get("total");
                    if (total instanceof BigDecimal bd) {
                        values[idx] = bd;
                    } else if (total instanceof Number n) {
                        values[idx] = BigDecimal.valueOf(n.doubleValue());
                    }
                }
            }
        }

        // Build result with Chinese weekday labels
        List<DashboardTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            String label = dow.getDisplayName(TextStyle.SHORT, Locale.CHINESE);
            result.add(new DashboardTrendPoint(label, values[i].intValue()));
        }
        return result;
    }

    public List<DashboardTrendPoint> reservations() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);

        // Query reservations for last 7 days grouped by date
        QueryWrapper<Reservation> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(created_at) AS stat_date, COUNT(*) AS cnt")
                .eq("deleted", 0)
                .apply("DATE(created_at) BETWEEN {0} AND {1}", start.toString(), end.toString())
                .groupBy("DATE(created_at)")
                .orderByAsc("stat_date");
        List<Map<String, Object>> maps = reservationMapper.selectMaps(wrapper);

        // Build date -> value map
        int[] values = new int[7];
        for (Map<String, Object> row : maps) {
            if (row.get("stat_date") instanceof java.sql.Date sqlDate) {
                LocalDate date = sqlDate.toLocalDate();
                int idx = (int) (date.toEpochDay() - start.toEpochDay());
                if (idx >= 0 && idx < 7) {
                    Object cnt = row.get("cnt");
                    if (cnt instanceof Number n) {
                        values[idx] = n.intValue();
                    }
                }
            }
        }

        // Build result with Chinese weekday labels
        List<DashboardTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            String label = dow.getDisplayName(TextStyle.SHORT, Locale.CHINESE);
            result.add(new DashboardTrendPoint(label, values[i]));
        }
        return result;
    }

    public record DashboardSummary(int reservationCount, int orderCount, BigDecimal revenue, int userCount,
                                   int storeCount, int catCount) {
    }

    public record DashboardTrendPoint(String label, int value) {
    }
}
