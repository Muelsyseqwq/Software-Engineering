package com.nekocafe.staff.controller;

import com.nekocafe.cat.entity.Cat;
import com.nekocafe.common.result.ApiResult;
import com.nekocafe.queue.service.WaitingQueueService;
import com.nekocafe.queue.service.WaitingQueueService.QueueTicketResponse;
import com.nekocafe.queue.service.WaitingQueueService.StaffQueueStatusResponse;
import com.nekocafe.security.AuthPrincipal;
import com.nekocafe.staff.dto.StaffOrderRow;
import com.nekocafe.staff.dto.StaffReservationRow;
import com.nekocafe.staff.dto.StaffReviewRow;
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
    private final WaitingQueueService waitingQueueService;

    public StaffController(StaffService staffService, WaitingQueueService waitingQueueService) {
        this.staffService = staffService;
        this.waitingQueueService = waitingQueueService;
    }

    @GetMapping("/reservations/today")
    public ApiResult<List<StaffReservationRow>> todayReservations(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(staffService.todayReservations(principal.userId()));
    }

    @PostMapping("/reservations/{id}/check-in")
    public ApiResult<Void> checkInReservation(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        staffService.checkInReservation(id, principal.userId());
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
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Integer capacity) {
        return ApiResult.ok(staffService.listTables(principal.userId(), status, capacity));
    }

    @GetMapping("/cats")
    public ApiResult<List<Cat>> listCats(@AuthenticationPrincipal AuthPrincipal principal,
                                          @RequestParam(required = false) String status) {
        return ApiResult.ok(staffService.listCats(principal.userId(), status));
    }

    @GetMapping("/orders/{id}/review")
    public ApiResult<StaffReviewRow> getOrderReview(@PathVariable Long id) {
        StaffReviewRow review = staffService.getOrderReview(id);
        return ApiResult.ok(review);
    }

    @PutMapping("/tables/{id}/status")
    public ApiResult<Void> updateTableStatus(@AuthenticationPrincipal AuthPrincipal principal,
                                              @PathVariable Long id,
                                              @RequestParam String status,
                                              @RequestParam(required = false) String reason) {
        staffService.updateTableStatus(id, principal.userId(), status, reason);
        return ApiResult.ok();
    }

    @GetMapping("/queues/status")
    public ApiResult<StaffQueueStatusResponse> queueStatus(@AuthenticationPrincipal AuthPrincipal principal,
                                                            @RequestParam Long storeId) {
        return ApiResult.ok(waitingQueueService.staffStatus(principal.userId(), storeId));
    }

    @PostMapping("/queues/{storeId}/next")
    public ApiResult<StaffQueueStatusResponse> callNextQueueNumber(@AuthenticationPrincipal AuthPrincipal principal,
                                                                    @PathVariable Long storeId) {
        return ApiResult.ok(waitingQueueService.callNext(principal.userId(), storeId));
    }

    @PostMapping("/queues/tickets/{ticketId}/seat")
    public ApiResult<QueueTicketResponse> markQueueTicketSeated(@AuthenticationPrincipal AuthPrincipal principal,
                                                                 @PathVariable Long ticketId) {
        return ApiResult.ok(waitingQueueService.markSeated(principal.userId(), ticketId));
    }

    @PostMapping("/queues/{storeId}/reset")
    public ApiResult<StaffQueueStatusResponse> resetQueue(@AuthenticationPrincipal AuthPrincipal principal,
                                                           @PathVariable Long storeId) {
        return ApiResult.ok(waitingQueueService.reset(principal.userId(), storeId));
    }
}
