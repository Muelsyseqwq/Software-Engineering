package com.nekocafe.dashboard.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardService {

    public DashboardSummary summary() {
        return new DashboardSummary(18, 32, new BigDecimal("2680.00"), 126, 2, 8);
    }

    public List<DashboardTrendPoint> revenue() {
        return List.of(
            new DashboardTrendPoint("周一", 680),
            new DashboardTrendPoint("周二", 820),
            new DashboardTrendPoint("周三", 760),
            new DashboardTrendPoint("周四", 980),
            new DashboardTrendPoint("周五", 1260),
            new DashboardTrendPoint("周六", 1880),
            new DashboardTrendPoint("周日", 1680)
        );
    }

    public List<DashboardTrendPoint> reservations() {
        return List.of(
            new DashboardTrendPoint("周一", 6),
            new DashboardTrendPoint("周二", 8),
            new DashboardTrendPoint("周三", 7),
            new DashboardTrendPoint("周四", 10),
            new DashboardTrendPoint("周五", 14),
            new DashboardTrendPoint("周六", 22),
            new DashboardTrendPoint("周日", 18)
        );
    }

    public record DashboardSummary(int reservationCount, int orderCount, BigDecimal revenue, int userCount, int storeCount, int catCount) {
    }

    public record DashboardTrendPoint(String label, int value) {
    }
}
