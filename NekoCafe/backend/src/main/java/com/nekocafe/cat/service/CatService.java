package com.nekocafe.cat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.cat.dto.CatHealthRecordRequest;
import com.nekocafe.cat.dto.CatHealthRecordResponse;
import com.nekocafe.cat.dto.CatRequest;
import com.nekocafe.cat.dto.CatResponse;
import com.nekocafe.cat.dto.CatWeightTrendPoint;
import com.nekocafe.cat.entity.Cat;
import com.nekocafe.cat.entity.CatHealthRecord;
import com.nekocafe.cat.mapper.CatHealthRecordMapper;
import com.nekocafe.cat.mapper.CatMapper;
import com.nekocafe.common.exception.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class CatService {

    private static final String HEALTHY = "健康";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String RESTING = "RESTING";
    private static final String DISABLED = "DISABLED";
    private static final Set<String> HEALTH_STATUSES = Set.of("健康", "观察中", "治疗中", "恢复中");
    private static final Set<String> CAT_STATUSES = Set.of("AVAILABLE", "RESTING", "DISABLED");

    private final CatMapper catMapper;
    private final CatHealthRecordMapper catHealthRecordMapper;
    private final JdbcTemplate jdbcTemplate;

    public CatService(CatMapper catMapper, CatHealthRecordMapper catHealthRecordMapper, JdbcTemplate jdbcTemplate) {
        this.catMapper = catMapper;
        this.catHealthRecordMapper = catHealthRecordMapper;
        this.jdbcTemplate = jdbcTemplate;
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

    @Transactional
    public CatResponse createCat(Long caretakerId, CatRequest request) {
        List<Long> storeIds = resolveStoreIds(caretakerId);
        if (storeIds.isEmpty()) {
            throw new BizException(4105, "当前猫咪管家未绑定门店");
        }
        Cat cat = new Cat();
        cat.setStoreId(storeIds.get(0));
        applyRequest(cat, request);
        cat.setHealthStatus(normalizeHealthStatus(request.healthStatus()));
        syncStatusWithHealth(cat);
        cat.setDeleted(0);
        catMapper.insert(cat);
        insertHealthRecordFromCat(cat, caretakerId, "创建猫咪档案");
        return toResponse(cat);
    }

    @Transactional
    public CatResponse updateCat(Long caretakerId, Long id, CatRequest request) {
        Cat cat = getExistingCat(caretakerId, id);
        BigDecimal oldWeight = cat.getWeight();
        String oldVaccinium = cat.getVaccinium();
        String oldInteract = cat.getInteract();
        applyRequest(cat, request);
        cat.setHealthStatus(normalizeHealthStatus(request.healthStatus()));
        syncStatusWithHealth(cat);
        catMapper.updateById(cat);
        if (healthSnapshotChanged(oldWeight, oldVaccinium, oldInteract, cat)) {
            insertHealthRecordFromCat(cat, caretakerId, "更新猫咪档案");
        }
        return toResponse(cat);
    }

    public CatResponse updateHealthStatus(Long caretakerId, Long id, String healthStatus) {
        Cat cat = getExistingCat(caretakerId, id);
        cat.setHealthStatus(normalizeHealthStatus(healthStatus));
        syncStatusWithHealth(cat);
        catMapper.updateById(cat);
        return toResponse(cat);
    }

    public CatResponse updateStatus(Long caretakerId, Long id, String status) {
        Cat cat = getExistingCat(caretakerId, id);
        String normalizedStatus = normalizeCatStatus(status);
        if (DISABLED.equals(normalizedStatus)) {
            cat.setStatus(DISABLED);
        } else {
            syncStatusWithHealth(cat);
        }
        catMapper.updateById(cat);
        return toResponse(cat);
    }

    public List<CatHealthRecordResponse> listHealthRecords(Long caretakerId, Long catId) {
        getExistingCat(caretakerId, catId);
        return catHealthRecordMapper.selectList(new LambdaQueryWrapper<CatHealthRecord>()
                .eq(CatHealthRecord::getCatId, catId)
                .eq(CatHealthRecord::getDeleted, 0)
                .orderByDesc(CatHealthRecord::getRecordDate)
                .orderByDesc(CatHealthRecord::getId))
            .stream()
            .map(this::toHealthRecordResponse)
            .toList();
    }

    @Transactional
    public CatHealthRecordResponse createHealthRecord(Long caretakerId, Long catId, CatHealthRecordRequest request) {
        Cat cat = getExistingCat(caretakerId, catId);
        validateHealthRecordContent(request);
        CatHealthRecord record = new CatHealthRecord();
        record.setCatId(cat.getId());
        record.setStoreId(cat.getStoreId());
        record.setRecordDate(request.recordDate() == null ? LocalDate.now() : request.recordDate());
        record.setWeight(request.weight());
        record.setVaccinium(trimToNull(request.vaccinium()));
        record.setInteract(trimToNull(request.interact()));
        record.setNote(trimToNull(request.note()));
        record.setRecordedBy(caretakerId);
        record.setDeleted(0);
        catHealthRecordMapper.insert(record);

        boolean shouldUpdateCat = false;
        if (record.getWeight() != null) {
            cat.setWeight(record.getWeight());
            shouldUpdateCat = true;
        }
        if (StringUtils.hasText(record.getVaccinium())) {
            cat.setVaccinium(record.getVaccinium());
            shouldUpdateCat = true;
        }
        if (StringUtils.hasText(record.getInteract())) {
            cat.setInteract(record.getInteract());
            shouldUpdateCat = true;
        }
        if (shouldUpdateCat) {
            catMapper.updateById(cat);
        }
        return toHealthRecordResponse(record);
    }

    public List<CatWeightTrendPoint> weightTrend(Long caretakerId, Long catId) {
        getExistingCat(caretakerId, catId);
        return catHealthRecordMapper.selectList(new LambdaQueryWrapper<CatHealthRecord>()
                .eq(CatHealthRecord::getCatId, catId)
                .eq(CatHealthRecord::getDeleted, 0)
                .isNotNull(CatHealthRecord::getWeight)
                .orderByAsc(CatHealthRecord::getRecordDate)
                .orderByAsc(CatHealthRecord::getId))
            .stream()
            .map(record -> new CatWeightTrendPoint(record.getRecordDate().toString(), record.getWeight()))
            .toList();
    }

    @Transactional
    public void deleteCat(Long caretakerId, Long id) {
        getExistingCat(caretakerId, id);
        jdbcTemplate.update("DELETE FROM cat_schedule WHERE cat_id = ?", id);
        jdbcTemplate.update("DELETE FROM reservation_cat WHERE cat_id = ?", id);
        jdbcTemplate.update("DELETE FROM cat_health_record WHERE cat_id = ?", id);
        jdbcTemplate.update("DELETE FROM cat WHERE id = ?", id);
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

    private void insertHealthRecordFromCat(Cat cat, Long caretakerId, String note) {
        if (cat.getWeight() == null && !StringUtils.hasText(cat.getVaccinium()) && !StringUtils.hasText(cat.getInteract())) {
            return;
        }
        CatHealthRecord record = new CatHealthRecord();
        record.setCatId(cat.getId());
        record.setStoreId(cat.getStoreId());
        record.setRecordDate(LocalDate.now());
        record.setWeight(cat.getWeight());
        record.setVaccinium(cat.getVaccinium());
        record.setInteract(cat.getInteract());
        record.setNote(note);
        record.setRecordedBy(caretakerId);
        record.setDeleted(0);
        catHealthRecordMapper.insert(record);
    }

    private boolean healthSnapshotChanged(BigDecimal oldWeight, String oldVaccinium, String oldInteract, Cat cat) {
        return !sameDecimal(oldWeight, cat.getWeight())
            || !Objects.equals(oldVaccinium, cat.getVaccinium())
            || !Objects.equals(oldInteract, cat.getInteract());
    }

    private boolean sameDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private void validateHealthRecordContent(CatHealthRecordRequest request) {
        if (request.weight() == null
            && !StringUtils.hasText(request.vaccinium())
            && !StringUtils.hasText(request.interact())
            && !StringUtils.hasText(request.note())) {
            throw new BizException(4106, "请至少填写体重、疫苗、互动记录或备注");
        }
    }

    private List<Long> resolveStoreIds(Long caretakerId) {
        if (caretakerId == null) {
            return Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT store_id FROM user_store_role WHERE user_id = ? AND role_code = ? AND status = ?",
            Long.class,
            caretakerId,
            "CAT_CARETAKER",
            "ACTIVE"
        );
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
            case "HEALTHY" -> "健康";
            case "OBSERVING" -> "观察中";
            case "TREATMENT" -> "治疗中";
            case "RECOVERING" -> "恢复中";
            default -> normalized;
        };
        if (!HEALTH_STATUSES.contains(normalized)) {
            throw new BizException(4103, "健康状态不正确");
        }
        return normalized;
    }

    private String normalizeCatStatus(String status) {
        String normalized = normalizeCode(status, AVAILABLE);
        normalized = switch (normalized) {
            case "ACTIVE" -> AVAILABLE;
            case "INACTIVE", "OFFLINE", "休息中", "不可互动" -> RESTING;
            case "停用", "已停用" -> DISABLED;
            default -> normalized;
        };
        if (!CAT_STATUSES.contains(normalized)) {
            throw new BizException(4104, "档案状态不正确");
        }
        return normalized;
    }

    private void syncStatusWithHealth(Cat cat) {
        if (DISABLED.equals(cat.getStatus())) {
            return;
        }
        cat.setStatus(statusForHealth(cat.getHealthStatus()));
    }

    private String statusForHealth(String healthStatus) {
        return HEALTHY.equals(normalizeHealthStatus(healthStatus)) ? AVAILABLE : RESTING;
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

    private CatHealthRecordResponse toHealthRecordResponse(CatHealthRecord record) {
        return new CatHealthRecordResponse(
            record.getId(),
            record.getCatId(),
            record.getRecordDate(),
            record.getWeight(),
            record.getVaccinium(),
            record.getInteract(),
            record.getNote(),
            record.getRecordedBy(),
            record.getCreatedAt()
        );
    }
}
