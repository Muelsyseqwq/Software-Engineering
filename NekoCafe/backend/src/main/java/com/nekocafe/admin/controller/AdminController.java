package com.nekocafe.admin.controller;

import com.nekocafe.admin.service.AdminService;
import com.nekocafe.admin.service.AdminService.AdminRoleRow;
import com.nekocafe.admin.service.AdminService.AdminStoreRow;
import com.nekocafe.admin.service.AdminService.AdminUserRow;
import com.nekocafe.admin.service.AdminService.StoreManagerRow;
import com.nekocafe.common.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('HQ_OPERATOR', 'ADMIN')")
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

    @GetMapping("/stores")
    public ApiResult<List<AdminStoreRow>> stores() {
        return ApiResult.ok(adminService.stores());
    }

    @GetMapping("/store-managers")
    public ApiResult<List<StoreManagerRow>> storeManagers() {
        return ApiResult.ok(adminService.storeManagers());
    }

    @PostMapping("/store-managers")
    public ApiResult<Void> assignStoreManager(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long storeId = Long.valueOf(body.get("storeId").toString());
        Long createdBy = body.containsKey("createdBy") && body.get("createdBy") != null
                ? Long.valueOf(body.get("createdBy").toString()) : null;
        adminService.assignStoreManager(userId, storeId, createdBy);
        return ApiResult.ok(null);
    }

    @DeleteMapping("/store-managers")
    public ApiResult<Void> removeStoreManager(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long storeId = Long.valueOf(body.get("storeId").toString());
        Long dismissedBy = body.containsKey("dismissedBy") && body.get("dismissedBy") != null
                ? Long.valueOf(body.get("dismissedBy").toString()) : null;
        adminService.removeStoreManager(userId, storeId, dismissedBy);
        return ApiResult.ok(null);
    }
}
