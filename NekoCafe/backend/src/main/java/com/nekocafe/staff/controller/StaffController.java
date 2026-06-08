package com.nekocafe.staff.controller;

import com.nekocafe.cat.entity.Cat;
import com.nekocafe.common.result.ApiResult;
import com.nekocafe.security.AuthPrincipal;
import com.nekocafe.staff.dto.StaffOrderRow;
import com.nekocafe.staff.dto.StaffReservationRow;
import com.nekocafe.staff.service.StaffService;
import com.nekocafe.store.entity.DiningTable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResult<List<StaffReservationRow>> todayReservations(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(staffService.todayReservations(principal.userId()));
    }

    @PostMapping("/reservations/{id}/check-in")
    public ApiResult<Void> checkInReservation(@PathVariable Long id) {
        staffService.checkInReservation(id);
        return ApiResult.ok();
    }

    @GetMapping("/orders/pending")
    public ApiResult<List<StaffOrderRow>> pendingOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(staffService.pendingOrders(principal.userId()));
    }

    @PostMapping("/orders/{id}/start")
    public ApiResult<Void> startOrder(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        staffService.startOrder(id, principal.userId());
        return ApiResult.ok();
    }

    @PostMapping("/orders/{id}/complete")
    public ApiResult<Void> completeOrder(@PathVariable Long id) {
        staffService.completeOrder(id);
        return ApiResult.ok();
    }

    @GetMapping("/orders/handled")
    public ApiResult<List<StaffOrderRow>> handledOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(staffService.handledOrders(principal.userId()));
    }

    @GetMapping("/tables")
    public ApiResult<List<DiningTable>> listTables(@AuthenticationPrincipal AuthPrincipal principal,
                                                    @RequestParam(required = false) String status) {
        return ApiResult.ok(staffService.listTables(principal.userId(), status));
    }

    @GetMapping("/cats")
    public ApiResult<List<Cat>> listCats(@AuthenticationPrincipal AuthPrincipal principal,
                                          @RequestParam(required = false) String status) {
        return ApiResult.ok(staffService.listCats(principal.userId(), status));
    }

    @PutMapping("/tables/{id}/status")
    public ApiResult<Void> updateTableStatus(@AuthenticationPrincipal AuthPrincipal principal,
                                              @PathVariable Long id,
                                              @RequestParam String status,
                                              @RequestParam(required = false) String reason) {
        staffService.updateTableStatus(id, principal.userId(), status, reason);
        return ApiResult.ok();
    }
}
