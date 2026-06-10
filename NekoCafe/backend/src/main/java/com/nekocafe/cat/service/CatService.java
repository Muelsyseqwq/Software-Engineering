package com.nekocafe.cat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.cat.dto.CatRequest;
import com.nekocafe.cat.dto.CatResponse;
import com.nekocafe.cat.entity.Cat;
import com.nekocafe.cat.mapper.CatMapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.user.entity.UserStoreRole;
import com.nekocafe.user.mapper.UserStoreRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class CatService {

    private static final String HEALTHY = "HEALTHY";
    private static final String ACTIVE = "ACTIVE";
    private static final Set<String> HEALTH_STATUSES = Set.of("HEALTHY", "OBSERVING", "TREATMENT", "RECOVERING");
    private static final Set<String> CAT_STATUSES = Set.of("ACTIVE", "INACTIVE", "ADOPTED");

    private final CatMapper catMapper;
    private final UserStoreRoleMapper userStoreRoleMapper;

    public CatService(CatMapper catMapper, UserStoreRoleMapper userStoreRoleMapper) {
        this.catMapper = catMapper;
        this.userStoreRoleMapper = userStoreRoleMapper;
    }

    public List<CatResponse> listCats(Long caretakerId) {
        List<Long> storeIds = resolveStoreIds(caretakerId);
        if (storeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return catMapper.selectList(new LambdaQueryWrapper<Cat>()
                .eq(Cat::getDeleted, 0)
                .in(Cat::getStoreId, storeIds)
                .orderByAsc(Cat::getStatus)
                .orderByDesc(Cat::getId))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public CatResponse getCat(Long caretakerId, Long id) {
        return toResponse(getExistingCat(caretakerId, id));
    }

    public CatResponse createCat(Long caretakerId, CatRequest request) {
        List<Long> storeIds = resolveStoreIds(caretakerId);
        if (storeIds.isEmpty()) {
            throw new BizException(4105, "当前猫咪管家未绑定门店");
        }
        Cat cat = new Cat();
        cat.setStoreId(storeIds.get(0));
        applyRequest(cat, request);
        cat.setHealthStatus(normalizeHealthStatus(request.healthStatus()));
        cat.setStatus(normalizeCatStatus(request.status()));
        cat.setDeleted(0);
        catMapper.insert(cat);
        return toResponse(cat);
    }

    public CatResponse updateCat(Long caretakerId, Long id, CatRequest request) {
        Cat cat = getExistingCat(caretakerId, id);
        applyRequest(cat, request);
        cat.setHealthStatus(normalizeHealthStatus(request.healthStatus()));
        cat.setStatus(normalizeCatStatus(request.status()));
        catMapper.updateById(cat);
        return toResponse(cat);
    }

    public CatResponse updateHealthStatus(Long caretakerId, Long id, String healthStatus) {
        Cat cat = getExistingCat(caretakerId, id);
        cat.setHealthStatus(normalizeHealthStatus(healthStatus));
        catMapper.updateById(cat);
        return toResponse(cat);
    }

    public CatResponse updateStatus(Long caretakerId, Long id, String status) {
        Cat cat = getExistingCat(caretakerId, id);
        cat.setStatus(normalizeCatStatus(status));
        catMapper.updateById(cat);
        return toResponse(cat);
    }

    public void deleteCat(Long caretakerId, Long id) {
        getExistingCat(caretakerId, id);
        catMapper.deleteById(id);
    }

    private Cat getExistingCat(Long caretakerId, Long id) {
        List<Long> storeIds = resolveStoreIds(caretakerId);
        if (storeIds.isEmpty()) {
            throw new BizException(4101, "猫咪档案不存在");
        }
        Cat cat = catMapper.selectOne(new LambdaQueryWrapper<Cat>()
            .eq(Cat::getId, id)
            .eq(Cat::getDeleted, 0)
            .in(Cat::getStoreId, storeIds)
            .last("LIMIT 1"));
        if (cat == null) {
            throw new BizException(4101, "猫咪档案不存在");
        }
        return cat;
    }

    private void applyRequest(Cat cat, CatRequest request) {
        cat.setName(requiredName(request.name()));
        cat.setBreed(trimToNull(request.breed()));
        cat.setAge(request.age());
        cat.setWeight(request.weight());
        cat.setGender(trimToNull(request.gender()));
        cat.setPersonality(trimToNull(request.personality()));
        cat.setInteract(trimToNull(request.interact()));
        cat.setVaccinium(trimToNull(request.vaccinium()));
        cat.setPhotoUrl(trimToNull(request.photoUrl()));
        cat.setDescription(trimToNull(request.description()));
    }

    private List<Long> resolveStoreIds(Long caretakerId) {
        LambdaQueryWrapper<UserStoreRole> wrapper = new LambdaQueryWrapper<UserStoreRole>()
            .eq(UserStoreRole::getUserId, caretakerId)
            .eq(UserStoreRole::getStatus, "ACTIVE")
            .eq(UserStoreRole::getRoleCode, "CAT_CARETAKER");
        List<UserStoreRole> roles = userStoreRoleMapper.selectList(wrapper);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream().map(UserStoreRole::getStoreId).distinct().toList();
    }

    private String requiredName(String name) {
        String trimmed = trimToNull(name);
        if (trimmed == null) {
            throw new BizException(4102, "猫咪名字不能为空");
        }
        return trimmed;
    }

    private String normalizeHealthStatus(String healthStatus) {
        String normalized = normalizeCode(healthStatus, HEALTHY);
        normalized = switch (normalized) {
            case "健康" -> "HEALTHY";
            case "观察中" -> "OBSERVING";
            case "治疗中" -> "TREATMENT";
            case "恢复中" -> "RECOVERING";
            default -> normalized;
        };
        if (!HEALTH_STATUSES.contains(normalized)) {
            throw new BizException(4103, "健康状态不正确");
        }
        return normalized;
    }

    private String normalizeCatStatus(String status) {
        String normalized = normalizeCode(status, ACTIVE);
        normalized = switch (normalized) {
            case "AVAILABLE" -> "ACTIVE";
            case "RESTING", "OFFLINE" -> "INACTIVE";
            case "ADOPTED" -> "ADOPTED";
            default -> normalized;
        };
        if (!CAT_STATUSES.contains(normalized)) {
            throw new BizException(4104, "档案状态不正确");
        }
        return normalized;
    }

    private String normalizeCode(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed.toUpperCase();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private CatResponse toResponse(Cat cat) {
        return new CatResponse(
            cat.getId(),
            cat.getName(),
            cat.getBreed(),
            cat.getAge(),
            cat.getWeight(),
            cat.getGender(),
            cat.getPersonality(),
            cat.getInteract(),
            cat.getHealthStatus(),
            cat.getVaccinium(),
            cat.getPhotoUrl(),
            cat.getDescription(),
            cat.getStatus()
        );
    }
}
