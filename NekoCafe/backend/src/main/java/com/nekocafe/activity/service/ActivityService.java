package com.nekocafe.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.activity.entity.ActivityStore;
import com.nekocafe.activity.entity.PromotionActivity;
import com.nekocafe.activity.mapper.ActivityStoreMapper;
import com.nekocafe.activity.mapper.PromotionActivityMapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.customer.entity.RewardCatalog;
import com.nekocafe.customer.mapper.RewardCatalogMapper;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.user.entity.User;
import com.nekocafe.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    private final PromotionActivityMapper activityMapper;
    private final ActivityStoreMapper activityStoreMapper;
    private final StoreMapper storeMapper;
    private final UserMapper userMapper;
    private final RewardCatalogMapper rewardCatalogMapper;

    public ActivityService(PromotionActivityMapper activityMapper,
                           ActivityStoreMapper activityStoreMapper,
                           StoreMapper storeMapper,
                           UserMapper userMapper,
                           RewardCatalogMapper rewardCatalogMapper) {
        this.activityMapper = activityMapper;
        this.activityStoreMapper = activityStoreMapper;
        this.storeMapper = storeMapper;
        this.userMapper = userMapper;
        this.rewardCatalogMapper = rewardCatalogMapper;
    }

    public List<ActivityRow> listActivities(String type, String status) {
        List<PromotionActivity> activities = activityMapper.selectAllActivities(
                type != null && !type.isBlank() ? type : null,
                status != null && !status.isBlank() ? status : null);
        Map<Long, String> rewardNameMap = buildRewardNameMap(activities);
        return activities.stream()
                .map(a -> toActivityRow(a, rewardNameMap))
                .toList();
    }

    @Transactional
    public ActivityRow createActivity(CreateActivityRequest request) {
        PromotionActivity activity = new PromotionActivity();
        activity.setTitle(request.title());
        activity.setType(request.type() != null ? request.type() : "PROMOTION");
        activity.setDescription(request.description());
        activity.setRewardId(request.rewardId());
        activity.setCoverUrl(request.coverUrl());
        activity.setStartAt(request.startAt());
        activity.setEndAt(request.endAt());
        activity.setStatus("DRAFT");
        activity.setCreatedBy(request.createdBy());
        activityMapper.insert(activity);
        String rewardName = resolveRewardName(request.rewardId());
        return toActivityRow(activity, rewardName);
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
        if (request.rewardId() != null) activity.setRewardId(request.rewardId());
        if (request.coverUrl() != null) activity.setCoverUrl(request.coverUrl());
        if (request.startAt() != null) activity.setStartAt(request.startAt());
        if (request.endAt() != null) activity.setEndAt(request.endAt());
        if (request.status() != null) activity.setStatus(request.status());
        activityMapper.updateById(activity);
        String rewardName = resolveRewardName(activity.getRewardId());
        return toActivityRow(activity, rewardName);
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

    private ActivityRow toActivityRow(PromotionActivity a, Map<Long, String> rewardNameMap) {
        String displayStatus = Integer.valueOf(1).equals(a.getDeleted()) ? "DELETED" : a.getStatus();
        String rewardName = a.getRewardId() != null ? rewardNameMap.get(a.getRewardId()) : null;
        return new ActivityRow(
                a.getId(), a.getTitle(), a.getType(), a.getDescription(),
                a.getCoverUrl(), a.getStartAt(), a.getEndAt(), displayStatus,
                a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt(),
                a.getRewardId(), rewardName);
    }

    private ActivityRow toActivityRow(PromotionActivity a, String rewardName) {
        String displayStatus = Integer.valueOf(1).equals(a.getDeleted()) ? "DELETED" : a.getStatus();
        return new ActivityRow(
                a.getId(), a.getTitle(), a.getType(), a.getDescription(),
                a.getCoverUrl(), a.getStartAt(), a.getEndAt(), displayStatus,
                a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt(),
                a.getRewardId(), rewardName);
    }

    private Map<Long, String> buildRewardNameMap(List<PromotionActivity> activities) {
        Set<Long> rewardIds = activities.stream()
                .map(PromotionActivity::getRewardId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (rewardIds.isEmpty()) return new java.util.HashMap<>();
        return rewardCatalogMapper.selectBatchIds(rewardIds).stream()
                .collect(Collectors.toMap(RewardCatalog::getId, RewardCatalog::getName, (a, b) -> a));
    }

    private String resolveRewardName(Long rewardId) {
        if (rewardId == null) return null;
        RewardCatalog reward = rewardCatalogMapper.selectById(rewardId);
        return reward != null ? reward.getName() : null;
    }

    // --- DTOs ---

    public record ActivityRow(Long id, String title, String type, String description,
                              String coverUrl, LocalDateTime startAt, LocalDateTime endAt,
                              String status, Long createdBy, LocalDateTime createdAt,
                              LocalDateTime updatedAt, Long rewardId, String rewardName) {}

    public record CreateActivityRequest(String title, String type, String description,
                                        String coverUrl, LocalDateTime startAt,
                                        LocalDateTime endAt, String status, Long createdBy,
                                        Long rewardId) {}

    public record RewardOption(Long id, String name, String rewardType) {}

    public List<RewardOption> listRewardOptions() {
        return rewardCatalogMapper.selectList(new LambdaQueryWrapper<RewardCatalog>()
                        .eq(RewardCatalog::getDeleted, 0)
                        .eq(RewardCatalog::getStatus, "ACTIVE")
                        .orderByAsc(RewardCatalog::getName))
                .stream()
                .map(r -> new RewardOption(r.getId(), r.getName(), r.getRewardType()))
                .toList();
    }

    public record StoreAcceptanceRow(Long id, Long storeId, String storeName,
                                     String acceptStatus, Long handledBy,
                                     String handlerName, LocalDateTime handledAt,
                                     String handleRemark) {}
}
