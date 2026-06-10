package com.nekocafe.activity.controller;

import com.nekocafe.activity.service.ActivityService;
import com.nekocafe.activity.service.ActivityService.ActivityRow;
import com.nekocafe.activity.service.ActivityService.CreateActivityRequest;
import com.nekocafe.activity.service.ActivityService.StoreAcceptanceRow;
import com.nekocafe.common.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
@PreAuthorize("hasAnyRole('HQ_OPERATOR', 'ADMIN')")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ApiResult<List<ActivityRow>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(activityService.listActivities(type, status));
    }

    @PostMapping
    public ApiResult<ActivityRow> create(@RequestBody CreateActivityRequest request) {
        return ApiResult.ok(activityService.createActivity(request));
    }

    @PutMapping("/{id}")
    public ApiResult<ActivityRow> update(@PathVariable Long id,
                                         @RequestBody CreateActivityRequest request) {
        return ApiResult.ok(activityService.updateActivity(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ApiResult.ok(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id,
                                   @RequestBody Map<String, List<Long>> body) {
        List<Long> storeIds = body.get("storeIds");
        activityService.publishToStores(id, storeIds);
        return ApiResult.ok(null);
    }

    @GetMapping("/{id}/stores")
    public ApiResult<List<StoreAcceptanceRow>> storeAcceptance(@PathVariable Long id) {
        return ApiResult.ok(activityService.getStoreAcceptance(id));
    }
}
