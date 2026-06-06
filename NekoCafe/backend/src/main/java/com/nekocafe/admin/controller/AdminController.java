package com.nekocafe.admin.controller;

import com.nekocafe.admin.service.AdminService;
import com.nekocafe.admin.service.AdminService.AdminRoleRow;
import com.nekocafe.admin.service.AdminService.AdminStoreRow;
import com.nekocafe.admin.service.AdminService.AdminUserRow;
import com.nekocafe.common.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/roles")
    public ApiResult<List<AdminRoleRow>> roles() {
        return ApiResult.ok(adminService.roles());
    }

    @GetMapping("/stores")
    public ApiResult<List<AdminStoreRow>> stores() {
        return ApiResult.ok(adminService.stores());
    }
}
