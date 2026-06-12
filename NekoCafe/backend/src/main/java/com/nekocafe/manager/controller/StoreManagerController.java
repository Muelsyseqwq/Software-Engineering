package com.nekocafe.manager.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.manager.dto.*;
import com.nekocafe.manager.service.StoreManagerService;
import com.nekocafe.security.AuthPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasRole('STORE_MANAGER')")
public class StoreManagerController {

    private final StoreManagerService storeManagerService;

    public StoreManagerController(StoreManagerService storeManagerService) {
        this.storeManagerService = storeManagerService;
    }

    @GetMapping("/store")
    public ApiResult<ManagerStoreInfo> store(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(storeManagerService.store(principal.userId()));
    }

    @PutMapping("/store")
    public ApiResult<ManagerStoreInfo> updateStore(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody UpdateManagerStoreRequest request
    ) {
        return ApiResult.ok(storeManagerService.updateStore(principal.userId(), request));
    }

    @PutMapping("/store/status")
    public ApiResult<Void> updateStoreStatus(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody UpdateStoreStatusRequest request
    ) {
        storeManagerService.updateStoreStatus(principal.userId(), request.status());
        return ApiResult.ok();
    }

    @GetMapping("/metrics")
    public ApiResult<ManagerMetricsSummary> metrics(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResult.ok(storeManagerService.metrics(principal.userId(), from, to));
    }

    @GetMapping("/tables")
    public ApiResult<List<ManagerTableRow>> tables(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(storeManagerService.tables(principal.userId()));
    }

    @PostMapping("/tables")
    public ApiResult<ManagerTableRow> createTable(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody ManagerTableRow payload
    ) {
        return ApiResult.ok(storeManagerService.createTable(principal.userId(), payload));
    }

    @PutMapping("/tables/{id}")
    public ApiResult<ManagerTableRow> updateTable(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long id,
        @RequestBody ManagerTableRow payload
    ) {
        return ApiResult.ok(storeManagerService.updateTable(principal.userId(), id, payload));
    }

    @GetMapping("/reservations")
    public ApiResult<List<ManagerReservationRow>> reservations(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResult.ok(storeManagerService.reservations(principal.userId(), status, date));
    }

    @PutMapping("/reservations/{id}/status")
    public ApiResult<ManagerReservationRow> updateReservationStatus(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long id,
        @RequestBody UpdateReservationStatusRequest request
    ) {
        return ApiResult.ok(storeManagerService.updateReservationStatus(principal.userId(), id, request.status()));
    }

    @GetMapping("/orders")
    public ApiResult<List<ManagerOrderRow>> orders(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResult.ok(storeManagerService.orders(principal.userId(), status, from, to));
    }

    @PutMapping("/orders/{id}/refund")
    public ApiResult<ManagerOrderRow> decideRefund(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long id,
        @RequestBody ManagerRefundDecisionRequest request
    ) {
        return ApiResult.ok(storeManagerService.decideRefund(principal.userId(), id, request));
    }

    @GetMapping("/orders/{id}")
    public ApiResult<ManagerOrderDetail> orderDetail(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long id
    ) {
        return ApiResult.ok(storeManagerService.orderDetail(principal.userId(), id));
    }

    @GetMapping("/cats")
    public ApiResult<List<ManagerCatStatusRow>> cats(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) String status
    ) {
        return ApiResult.ok(storeManagerService.cats(principal.userId(), status));
    }

    @GetMapping("/staff")
    public ApiResult<List<ManagerStaffRow>> staff(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String roleCode
    ) {
        return ApiResult.ok(storeManagerService.staff(principal.userId(), status, roleCode));
    }

    @PostMapping("/staff")
    public ApiResult<ManagerStaffRow> hireStaff(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody HireStaffRequest request
    ) {
        return ApiResult.ok(storeManagerService.hireStaff(principal.userId(), request));
    }

    @PutMapping("/staff/{userStoreRoleId}/dismiss")
    public ApiResult<Void> dismissStaff(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long userStoreRoleId,
        @RequestBody(required = false) DismissStaffRequest request
    ) {
        storeManagerService.dismissStaff(principal.userId(), userStoreRoleId, request);
        return ApiResult.ok();
    }

    @PostMapping("/staff/{userId}/leave")
    public ApiResult<Void> grantLeave(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long userId,
        @RequestBody GrantLeaveRequest request
    ) {
        storeManagerService.grantLeave(principal.userId(), userId, request);
        return ApiResult.ok();
    }

    @GetMapping("/shifts")
    public ApiResult<List<ManagerShiftRow>> shifts(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long userId
    ) {
        return ApiResult.ok(storeManagerService.shifts(principal.userId(), from, to, userId));
    }

    @PostMapping("/shifts")
    public ApiResult<ManagerShiftRow> createShift(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody ManagerShiftRequest request
    ) {
        return ApiResult.ok(storeManagerService.createShift(principal.userId(), request));
    }

    @PutMapping("/shifts/{id}")
    public ApiResult<ManagerShiftRow> updateShift(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long id,
        @RequestBody ManagerShiftRequest request
    ) {
        return ApiResult.ok(storeManagerService.updateShift(principal.userId(), id, request));
    }

    @GetMapping("/activities")
    public ApiResult<List<ManagerActivityRow>> activities(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) String status
    ) {
        return ApiResult.ok(storeManagerService.activities(principal.userId(), status));
    }

    @PutMapping("/activities/{activityStoreId}/decision")
    public ApiResult<ManagerActivityRow> decideActivity(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long activityStoreId,
        @RequestBody ActivityDecisionRequest request
    ) {
        return ApiResult.ok(storeManagerService.decideActivity(principal.userId(), activityStoreId, request));
    }

    @GetMapping("/dishes")
    public ApiResult<List<ManagerDishRow>> dishes(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String status
    ) {
        return ApiResult.ok(storeManagerService.dishes(principal.userId(), categoryId, status));
    }

    @PutMapping("/dishes/{id}/price")
    public ApiResult<ManagerDishRow> updateDishPrice(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long id,
        @RequestBody UpdateDishPriceRequest request
    ) {
        return ApiResult.ok(storeManagerService.updateDishPrice(principal.userId(), id, request));
    }

    @GetMapping("/dishes/{id}/price-history")
    public ApiResult<List<DishPriceHistoryRow>> dishPriceHistory(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long id
    ) {
        return ApiResult.ok(storeManagerService.dishPriceHistory(principal.userId(), id));
    }
}
