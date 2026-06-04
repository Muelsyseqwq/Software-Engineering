package com.nekocafe.dashboard.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.dashboard.service.DashboardService;
import com.nekocafe.dashboard.service.DashboardService.DashboardSummary;
import com.nekocafe.dashboard.service.DashboardService.DashboardTrendPoint;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('HQ_OPERATOR', 'ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

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
}
