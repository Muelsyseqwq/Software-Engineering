package com.nekocafe.manager.controller;

import com.nekocafe.common.result.ApiResult;
import com.nekocafe.manager.service.StoreManagerService;
import com.nekocafe.manager.service.StoreManagerService.ManagerReservationRow;
import com.nekocafe.manager.service.StoreManagerService.ManagerStoreInfo;
import com.nekocafe.manager.service.StoreManagerService.ManagerTableRow;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasRole('STORE_MANAGER')")
public class StoreManagerController {

    private final StoreManagerService storeManagerService;

    public StoreManagerController(StoreManagerService storeManagerService) {
        this.storeManagerService = storeManagerService;
    }

    @GetMapping("/store")
    public ApiResult<ManagerStoreInfo> store() {
        return ApiResult.ok(storeManagerService.store());
    }

    @PutMapping("/store/status")
    public ApiResult<Void> updateStoreStatus(@RequestBody Map<String, String> payload) {
        storeManagerService.updateStoreStatus(payload.getOrDefault("status", "OPEN"));
        return ApiResult.ok();
    }

    @GetMapping("/tables")
    public ApiResult<List<ManagerTableRow>> tables() {
        return ApiResult.ok(storeManagerService.tables());
    }

    @PostMapping("/tables")
    public ApiResult<ManagerTableRow> createTable(@RequestBody ManagerTableRow payload) {
        return ApiResult.ok(storeManagerService.createTable(payload));
    }

    @PutMapping("/tables/{id}")
    public ApiResult<ManagerTableRow> updateTable(@PathVariable Long id, @RequestBody ManagerTableRow payload) {
        return ApiResult.ok(storeManagerService.updateTable(id, payload));
    }

    @GetMapping("/reservations")
    public ApiResult<List<ManagerReservationRow>> reservations() {
        return ApiResult.ok(storeManagerService.reservations());
    }
}
