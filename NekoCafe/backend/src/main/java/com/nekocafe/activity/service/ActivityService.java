package com.nekocafe.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.activity.entity.ActivityStore;
import com.nekocafe.activity.entity.PromotionActivity;
import com.nekocafe.activity.mapper.ActivityStoreMapper;
import com.nekocafe.activity.mapper.PromotionActivityMapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.user.entity.User;
import com.nekocafe.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityService {

    private final PromotionActivityMapper activityMapper;
    private final ActivityStoreMapper activityStoreMapper;
    private final StoreMapper storeMapper;
    private final UserMapper userMapper;

    public ActivityService(PromotionActivityMapper activityMapper,
                           ActivityStoreMapper activityStoreMapper,
                           StoreMapper storeMapper,
                           UserMapper userMapper) {
        this.activityMapper = activityMapper;
        this.activityStoreMapper = activityStoreMapper;
        this.storeMapper = storeMapper;
        this.userMapper = userMapper;
    }

    public List<ActivityRow> listActivities(String type, String status) {
        return activityMapper.selectAllActivities(
                        type != null && !type.isBlank() ? type : null,
                        status != null && !status.isBlank() ? status : null)
                .stream()
                .map(this::toActivityRow)
                .toList();
    }

    @Transactional
    public ActivityRow createActivity(CreateActivityRequest request) {
        PromotionActivity activity = new PromotionActivity();
        activity.setTitle(request.title());
        activity.setType(request.type() != null ? request.type() : "PROMOTION");
        activity.setDescription(request.description());
        activity.setCoverUrl(request.coverUrl());
        activity.setStartAt(request.startAt());
        activity.setEndAt(request.endAt());
        activity.setStatus("DRAFT");
        activity.setCreatedBy(request.createdBy());
        activityMapper.insert(activity);
        return toActivityRow(activity);
    }

    @Transactional
    public ActivityRow updateActivity(Long id, CreateActivityRequest request) {
        PromotionActivity activity = activityMapper.selectById(id);
        if (activity == null || activity.getDeleted() == 1) {
            throw new BizException(3001, "活动不存在");
        }
        if (request.title() != null) activity.setTitle(request.title());
        if (request.type() != null) activity.setType(request.type());
        if (request.description() != null) activity.setDescription(request.description());
        if (request.coverUrl() != null) activity.setCoverUrl(request.coverUrl());
        if (request.startAt() != null) activity.setStartAt(request.startAt());
        if (request.endAt() != null) activity.setEndAt(request.endAt());
        if (request.status() != null) activity.setStatus(request.status());
        activityMapper.updateById(activity);
        return toActivityRow(activity);
    }

    @Transactional
    public void deleteActivity(Long id) {
        PromotionActivity activity = activityMapper.selectById(id);
        if (activity == null || activity.getDeleted() == 1) {
            throw new BizException(3001, "活动不存在");
        }
        // Must use deleteById so MyBatis-Plus logic-delete interceptor handles the deleted column.
        // Direct setDeleted(1) + updateById() is silently ignored because the global
        // logic-delete-field config strips deleted from UPDATE statements.
        activityMapper.deleteById(id);

        // Also clean up associated activity_store records
        activityStoreMapper.delete(new LambdaQueryWrapper<ActivityStore>()
                .eq(ActivityStore::getActivityId, id));
    }

    @Transactional
    public void publishToStores(Long activityId, List<Long> storeIds) {
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getDeleted() == 1) {
            throw new BizException(3001, "活动不存在");
        }
        // Update activity status to PUBLISHED
        activity.setStatus("PUBLISHED");
        activityMapper.updateById(activity);

        // Remove activity_store rows for stores that are no longer selected
        if (storeIds != null && !storeIds.isEmpty()) {
            activityStoreMapper.delete(new LambdaQueryWrapper<ActivityStore>()
                    .eq(ActivityStore::getActivityId, activityId)
                    .notIn(ActivityStore::getStoreId, storeIds));
        } else {
            activityStoreMapper.delete(new LambdaQueryWrapper<ActivityStore>()
                    .eq(ActivityStore::getActivityId, activityId));
        }

        for (Long storeId : storeIds) {
            // Check if already exists
            ActivityStore existing = activityStoreMapper.selectOne(
                    new LambdaQueryWrapper<ActivityStore>()
                            .eq(ActivityStore::getActivityId, activityId)
                            .eq(ActivityStore::getStoreId, storeId));
            if (existing != null) {
                existing.setAcceptStatus("PENDING");
                existing.setHandledBy(null);
                existing.setHandledAt(null);
                existing.setHandleRemark(null);
                activityStoreMapper.updateById(existing);
            } else {
                ActivityStore as = new ActivityStore();
                as.setActivityId(activityId);
                as.setStoreId(storeId);
                as.setAcceptStatus("PENDING");
                activityStoreMapper.insert(as);
            }
        }
    }

    public List<StoreAcceptanceRow> getStoreAcceptance(Long activityId) {
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getDeleted() == 1) {
            throw new BizException(3001, "活动不存在");
        }
        List<ActivityStore> mappings = activityStoreMapper.selectList(
                new LambdaQueryWrapper<ActivityStore>()
                        .eq(ActivityStore::getActivityId, activityId));
        return mappings.stream().map(m -> {
            Store store = storeMapper.selectById(m.getStoreId());
            String storeName = store != null ? store.getName() : "未知门店";
            String handlerName = null;
            if (m.getHandledBy() != null) {
                User handler = userMapper.selectById(m.getHandledBy());
                handlerName = handler != null ? handler.getNickname() : null;
            }
            return new StoreAcceptanceRow(
                    m.getId(), m.getStoreId(), storeName, m.getAcceptStatus(),
                    m.getHandledBy(), handlerName, m.getHandledAt(), m.getHandleRemark());
        }).toList();
    }

    private ActivityRow toActivityRow(PromotionActivity a) {
        String displayStatus = Integer.valueOf(1).equals(a.getDeleted()) ? "DELETED" : a.getStatus();
        return new ActivityRow(
                a.getId(), a.getTitle(), a.getType(), a.getDescription(),
                a.getCoverUrl(), a.getStartAt(), a.getEndAt(), displayStatus,
                a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt());
    }

    // --- DTOs ---

    public record ActivityRow(Long id, String title, String type, String description,
                              String coverUrl, LocalDateTime startAt, LocalDateTime endAt,
                              String status, Long createdBy, LocalDateTime createdAt,
                              LocalDateTime updatedAt) {}

    public record CreateActivityRequest(String title, String type, String description,
                                        String coverUrl, LocalDateTime startAt,
                                        LocalDateTime endAt, String status, Long createdBy) {}

    public record StoreAcceptanceRow(Long id, Long storeId, String storeName,
                                     String acceptStatus, Long handledBy,
                                     String handlerName, LocalDateTime handledAt,
                                     String handleRemark) {}
}
