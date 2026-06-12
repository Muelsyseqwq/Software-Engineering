package com.nekocafe.reservation.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.queue.service.WaitingQueueService;
import com.nekocafe.queue.service.WaitingQueueService.ApplyQueueRequest;
import com.nekocafe.queue.service.WaitingQueueService.QueueStatusResponse;
import com.nekocafe.queue.service.WaitingQueueService.QueueTicketResponse;
import com.nekocafe.reservation.service.ReservationService;
import com.nekocafe.reservation.service.ReservationService.CreateReservationRequest;
import com.nekocafe.reservation.service.ReservationService.ReservationResponse;
import com.nekocafe.reservation.service.ReservationService.ReservationSlotResponse;
import com.nekocafe.security.AuthPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservation")
public class ReservationController {

    private final ReservationService reservationService;
    private final WaitingQueueService waitingQueueService;

    public ReservationController(ReservationService reservationService, WaitingQueueService waitingQueueService) {
        this.reservationService = reservationService;
        this.waitingQueueService = waitingQueueService;
    }

    @GetMapping("/status")
    public ApiResult<Map<String, String>> status() {
        return ApiResult.ok(Map.of("module", "reservation", "status", "ready"));
    }

    @GetMapping("/slots")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<List<ReservationSlotResponse>> slots(
        @RequestParam Long storeId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam Integer partySize
    ) {
        return ApiResult.ok(reservationService.slots(storeId, date, partySize));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<ReservationResponse> create(@AuthenticationPrincipal AuthPrincipal principal, @RequestBody CreateReservationRequest request) {
        return ApiResult.ok(reservationService.create(principal.userId(), request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<List<ReservationResponse>> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(reservationService.mine(principal.userId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<ReservationResponse> cancel(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        return ApiResult.ok(reservationService.cancel(principal.userId(), id));
    }

    @PostMapping("/queue")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<QueueTicketResponse> applyQueue(@AuthenticationPrincipal AuthPrincipal principal, @RequestBody ApplyQueueRequest request) {
        return ApiResult.ok(waitingQueueService.apply(principal.userId(), request));
    }

    @GetMapping("/queue/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<QueueStatusResponse> queueStatus(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam Long storeId,
        @RequestParam(required = false) Integer partySize
    ) {
        return ApiResult.ok(waitingQueueService.customerStatus(principal.userId(), storeId, partySize));
    }

    @PostMapping("/queue/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResult<QueueTicketResponse> cancelQueue(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        return ApiResult.ok(waitingQueueService.cancel(principal.userId(), id));
    }
}
