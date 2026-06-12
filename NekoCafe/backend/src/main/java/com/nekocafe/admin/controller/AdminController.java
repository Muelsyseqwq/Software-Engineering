package com.nekocafe.admin.controller;

import com.nekocafe.admin.service.AdminService;
import com.nekocafe.admin.service.AdminService.AdminRoleRow;
import com.nekocafe.admin.service.AdminService.AdminUserRow;
import com.nekocafe.admin.service.AdminService.CreateStoreManagerRequest;
import com.nekocafe.admin.service.AdminService.StoreManagerRow;
import com.nekocafe.common.result.ApiResult;
import com.nekocafe.security.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('HQ_OPERATOR')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ApiResult<List<AdminUserRow>> users() {
        return ApiResult.ok(adminService.users());
    }

    @PutMapping("/users/{id}/status")
    public ApiResult<Void> updateUserStatus(@PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        adminService.updateUserStatus(id, body.get("status"));
        return ApiResult.ok(null);
    }

    @GetMapping("/roles")
    public ApiResult<List<AdminRoleRow>> roles() {
        return ApiResult.ok(adminService.roles());
    }

    @GetMapping("/store-managers")
    public ApiResult<List<StoreManagerRow>> storeManagers() {
        return ApiResult.ok(adminService.storeManagers());
    }

    @PostMapping("/store-managers")
    public ApiResult<Void> assignStoreManager(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long storeId = Long.valueOf(body.get("storeId").toString());
        Long createdBy = principal != null ? principal.userId() : null;
        adminService.assignStoreManager(userId, storeId, createdBy);
        return ApiResult.ok(null);
    }

    @DeleteMapping("/store-managers")
    public ApiResult<Void> removeStoreManager(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long storeId = Long.valueOf(body.get("storeId").toString());
        Long dismissedBy = principal != null ? principal.userId() : null;
        adminService.removeStoreManager(userId, storeId, dismissedBy);
        return ApiResult.ok(null);
    }

    /**
     * Create a new user and assign as store manager in a single request.
     */
    @PostMapping("/store-managers/with-user")
    public ApiResult<AdminUserRow> createUserAndAssignStoreManager(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody CreateStoreManagerRequest request) {
        Long createdBy = principal != null ? principal.userId() : null;
        return ApiResult.ok(adminService.createUserAndAssignStoreManager(request, createdBy));
    }
}
