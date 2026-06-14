package com.nekocafe.customer.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.customer.service.CustomerService;
import com.nekocafe.customer.service.CustomerService.CustomerActivityResponse;
import com.nekocafe.customer.service.CustomerService.HomeResponse;
import com.nekocafe.customer.service.CustomerService.PointsSummaryResponse;
import com.nekocafe.customer.service.CustomerService.PreferenceRequest;
import com.nekocafe.customer.service.CustomerService.PreferenceResponse;
import com.nekocafe.customer.service.CustomerService.RedeemRewardResponse;
import com.nekocafe.customer.service.CustomerService.RefundRequestPayload;
import com.nekocafe.customer.service.CustomerService.RefundResponse;
import com.nekocafe.customer.service.CustomerService.ReviewRequest;
import com.nekocafe.customer.service.CustomerService.ReviewResponse;
import com.nekocafe.customer.service.CustomerService.RewardCatalogResponse;
import com.nekocafe.customer.service.CustomerService.RewardRedemptionResponse;
import com.nekocafe.security.AuthPrincipal;
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

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/home")
    public ApiResult<HomeResponse> home(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(customerService.home(principal.userId()));
    }

    @GetMapping("/activities")
    public ApiResult<List<CustomerActivityResponse>> activities(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) Long storeId
    ) {
        return ApiResult.ok(customerService.activitiesForUser(principal.userId(), type, storeId));
    }

    @PostMapping("/activities/{activityId}/claim")
    public ApiResult<RewardRedemptionResponse> claimActivityReward(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long activityId
    ) {
        return ApiResult.ok(customerService.claimActivityReward(principal.userId(), activityId));
    }

    @GetMapping("/points")
    public ApiResult<PointsSummaryResponse> points(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(customerService.points(principal.userId()));
    }

    @GetMapping("/rewards")
    public ApiResult<List<RewardCatalogResponse>> rewards(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(customerService.rewards(principal.userId()));
    }

    @PostMapping("/rewards/{rewardId}/redeem")
    public ApiResult<RedeemRewardResponse> redeemReward(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long rewardId
    ) {
        return ApiResult.ok(customerService.redeemReward(principal.userId(), rewardId));
    }

    @GetMapping("/redemptions/me")
    public ApiResult<List<RewardRedemptionResponse>> myRedemptions(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(customerService.myRedemptions(principal.userId()));
    }

    @GetMapping("/preferences")
    public ApiResult<List<PreferenceResponse>> preferences(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(customerService.preferences(principal.userId()));
    }

    @PutMapping("/preferences")
    public ApiResult<List<PreferenceResponse>> savePreferences(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody List<PreferenceRequest> request
    ) {
        return ApiResult.ok(customerService.savePreferences(principal.userId(), request));
    }

    @PostMapping("/orders/{orderId}/reviews")
    public ApiResult<ReviewResponse> createReview(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long orderId,
        @RequestBody ReviewRequest request
    ) {
        return ApiResult.ok(customerService.createReview(principal.userId(), orderId, request));
    }

    @GetMapping("/reviews/me")
    public ApiResult<List<ReviewResponse>> myReviews(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(customerService.myReviews(principal.userId()));
    }

    @PostMapping("/orders/{orderId}/refunds")
    public ApiResult<RefundResponse> applyRefund(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long orderId,
        @RequestBody(required = false) RefundRequestPayload request
    ) {
        return ApiResult.ok(customerService.applyRefund(principal.userId(), orderId, request));
    }

    @GetMapping("/refunds/me")
    public ApiResult<List<RefundResponse>> myRefunds(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResult.ok(customerService.myRefunds(principal.userId()));
    }
}
