package com.nekocafe.staff.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.staff.service.StaffService;
import com.nekocafe.staff.service.StaffService.StaffOrderRow;
import com.nekocafe.staff.service.StaffService.StaffReservationRow;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasRole('STAFF')")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/reservations/today")
    public ApiResult<List<StaffReservationRow>> todayReservations() {
        return ApiResult.ok(staffService.todayReservations());
    }

    @PostMapping("/reservations/{id}/check-in")
    public ApiResult<Void> checkInReservation(@PathVariable Long id) {
        staffService.checkInReservation(id);
        return ApiResult.ok();
    }

    @GetMapping("/orders/pending")
    public ApiResult<List<StaffOrderRow>> pendingOrders() {
        return ApiResult.ok(staffService.pendingOrders());
    }

    @PostMapping("/orders/{id}/start")
    public ApiResult<Void> startOrder(@PathVariable Long id) {
        staffService.startOrder(id);
        return ApiResult.ok();
    }

    @PostMapping("/orders/{id}/complete")
    public ApiResult<Void> completeOrder(@PathVariable Long id) {
        staffService.completeOrder(id);
        return ApiResult.ok();
    }
}
