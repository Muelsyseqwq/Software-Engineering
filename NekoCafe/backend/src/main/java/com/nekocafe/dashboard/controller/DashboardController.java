package com.nekocafe.dashboard.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.dashboard.service.DashboardService;
import com.nekocafe.dashboard.service.DashboardService.CrossStoreRow;
import com.nekocafe.dashboard.service.DashboardService.DashboardPeriodSummary;
import com.nekocafe.dashboard.service.DashboardService.DashboardSummary;
import com.nekocafe.dashboard.service.DashboardService.DashboardTrendPoint;
import com.nekocafe.dashboard.service.DashboardService.StoreMetrics;
import com.nekocafe.dashboard.service.DashboardService.StoreSummaryRow;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('HQ_OPERATOR')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ---- legacy endpoints (unchanged) ----

    @GetMapping("/summary")
    public ApiResult<DashboardSummary> summary() {
        return ApiResult.ok(dashboardService.summary());
    }

    @GetMapping("/revenue")
    public ApiResult<List<DashboardTrendPoint>> revenue() {
        return ApiResult.ok(dashboardService.revenue());
    }

    @GetMapping("/reservations")
    public ApiResult<List<DashboardTrendPoint>> reservations() {
        return ApiResult.ok(dashboardService.reservations());
    }

    @GetMapping("/store-summary")
    public ApiResult<List<StoreSummaryRow>> storeSummary() {
        return ApiResult.ok(dashboardService.storeSummaries());
    }

    @GetMapping("/store/{storeId}/revenue")
    public ApiResult<List<DashboardTrendPoint>> storeRevenue(@PathVariable Long storeId) {
        return ApiResult.ok(dashboardService.storeRevenue(storeId));
    }

    // ---- new: period-aware KPI overview ----

    @GetMapping("/overview")
    public ApiResult<DashboardPeriodSummary> overview(
            @RequestParam(defaultValue = "WEEK") String period,
            @RequestParam(required = false) Long storeId) {
        return ApiResult.ok(dashboardService.periodSummary(period, storeId));
    }

    // ---- new: per-store metrics (坪效/翻台率/复购率) ----

    @GetMapping("/store/{storeId}/metrics")
    public ApiResult<StoreMetrics> storeMetrics(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "WEEK") String period) {
        return ApiResult.ok(dashboardService.storeMetrics(storeId, period));
    }

    // ---- new: cross-store comparison ----

    @GetMapping("/cross-store")
    public ApiResult<List<CrossStoreRow>> crossStore(
            @RequestParam(defaultValue = "WEEK") String period) {
        return ApiResult.ok(dashboardService.crossStore(period));
    }

    // ---- new: trend data for operator charts ----

    @GetMapping("/trend")
    public ApiResult<List<DashboardTrendPoint>> trend(
            @RequestParam(defaultValue = "WEEK") String period,
            @RequestParam(defaultValue = "REVENUE") String metric,
            @RequestParam(required = false) Long storeId) {
        return ApiResult.ok(dashboardService.operatorTrend(period, metric, storeId));
    }
}
