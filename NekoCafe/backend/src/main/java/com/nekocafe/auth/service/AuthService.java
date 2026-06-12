package com.nekocafe.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.auth.dto.AuthResponse;
import com.nekocafe.auth.dto.AuthUserResponse;
import com.nekocafe.auth.dto.AuthUserResponse.AuthAssignedStore;
import com.nekocafe.auth.dto.LoginRequest;
import com.nekocafe.auth.dto.RegisterRequest;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.customer.entity.UserPreference;
import com.nekocafe.customer.mapper.UserPreferenceMapper;
import com.nekocafe.security.AuthPrincipal;
import com.nekocafe.security.JwtService;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.entity.UserStoreRole;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.store.mapper.UserStoreRoleMapper;
import com.nekocafe.user.entity.MemberAccount;
import com.nekocafe.user.entity.Role;
import com.nekocafe.user.entity.User;
import com.nekocafe.user.entity.UserRole;
import com.nekocafe.user.mapper.MemberAccountMapper;
import com.nekocafe.user.mapper.RoleMapper;
import com.nekocafe.user.mapper.UserMapper;
import com.nekocafe.user.mapper.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AuthService {

    private static final String ACTIVE = "ACTIVE";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String STAFF = "STAFF";
    private static final String STORE_MANAGER = "STORE_MANAGER";
    private static final String CAT_CARETAKER = "CAT_CARETAKER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final MemberAccountMapper memberAccountMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserStoreRoleMapper userStoreRoleMapper;
    private final StoreMapper storeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserMapper userMapper,
        RoleMapper roleMapper,
        UserRoleMapper userRoleMapper,
        MemberAccountMapper memberAccountMapper,
        UserPreferenceMapper userPreferenceMapper,
        UserStoreRoleMapper userStoreRoleMapper,
        StoreMapper storeMapper,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.memberAccountMapper = memberAccountMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.userStoreRoleMapper = userStoreRoleMapper;
        this.storeMapper = storeMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeRequired(request.username());
        String nickname = normalizeRequired(request.nickname());
        String phone = normalizeOptional(request.phone());
        String email = normalizeOptional(request.email());

        ensureUnique("username", username, 1002, "用户名已存在");
        if (phone != null) {
            ensureUnique("phone", phone, 1003, "手机号已存在");
        }
        if (email != null) {
            ensureUnique("email", email, 1004, "邮箱已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(ACTIVE);
        userMapper.insert(user);

        Role customerRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, CUSTOMER));
        if (customerRole == null) {
            throw new BizException(1006, "默认顾客角色未初始化");
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(customerRole.getId());
        userRoleMapper.insert(userRole);

        MemberAccount memberAccount = new MemberAccount();
        memberAccount.setUserId(user.getId());
        memberAccount.setLevelCode("NORMAL");
        memberAccount.setPoints(0);
        memberAccount.setTotalSpent(BigDecimal.ZERO);
        memberAccountMapper.insert(memberAccount);

        saveInitialPreferences(user.getId(), request.preferences());

        return buildAuthResponse(user, List.of(CUSTOMER));
    }

    public AuthResponse login(LoginRequest request) {
        String account = normalizeRequired(request.account());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getDeleted, 0)
            .and(wrapper -> wrapper
                .eq(User::getUsername, account)
                .or()
                .eq(User::getPhone, account)
                .or()
                .eq(User::getEmail, account)
            )
            .last("LIMIT 1"));

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(1001, "账号或密码错误");
        }
        if (!ACTIVE.equals(user.getStatus())) {
            throw new BizException(1005, "账号状态不可用");
        }
        return buildAuthResponse(user, loadRoleCodes(user.getId()));
    }

    public AuthUserResponse me(AuthPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new BizException(401, "请先登录");
        }
        User user = userMapper.selectById(principal.userId());
        if (user == null || Objects.equals(user.getDeleted(), 1)) {
            throw new BizException(401, "请先登录");
        }
        return toUserResponse(user, loadRoleCodes(user.getId()));
    }

    private void saveInitialPreferences(Long userId, List<RegisterRequest.PreferenceRequest> preferences) {
        for (RegisterRequest.PreferenceRequest request : normalizePreferences(preferences)) {
            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setPreferenceType(request.preferenceType());
            preference.setPreferenceValue(request.preferenceValue());
            userPreferenceMapper.insert(preference);
        }
    }

    private List<RegisterRequest.PreferenceRequest> normalizePreferences(List<RegisterRequest.PreferenceRequest> preferences) {
        if (preferences == null) {
            return List.of();
        }
        List<RegisterRequest.PreferenceRequest> result = new ArrayList<>();
        for (RegisterRequest.PreferenceRequest request : preferences) {
            if (request == null) continue;
            String type = normalizeOptional(request.preferenceType());
            String value = normalizeOptional(request.preferenceValue());
            if (type == null || value == null) continue;
            RegisterRequest.PreferenceRequest normalized = new RegisterRequest.PreferenceRequest(type.toUpperCase(), limitText(value, 128));
            if (result.stream().noneMatch(item -> Objects.equals(item.preferenceType(), normalized.preferenceType())
                && Objects.equals(item.preferenceValue(), normalized.preferenceValue()))) {
                result.add(normalized);
            }
        }
        return result;
    }

    private AuthResponse buildAuthResponse(User user, List<String> roles) {
        AuthUserResponse authUser = toUserResponse(user, roles);
        return new AuthResponse(jwtService.generateToken(authUser), "Bearer", jwtService.getExpiresAt(), authUser);
    }

    private AuthUserResponse toUserResponse(User user, List<String> roles) {
        AssignedStoreInfo storeInfo = resolveAssignedStoreInfo(user.getId(), roles);
        return new AuthUserResponse(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getPhone(),
            user.getEmail(),
            roles,
            storeInfo.storeId(),
            storeInfo.storeName(),
            storeInfo.storeNames(),
            storeInfo.stores()
        );
    }

    private AssignedStoreInfo resolveAssignedStoreInfo(Long userId, List<String> roles) {
        if (userId == null || roles == null || roles.stream().noneMatch(this::isStoreScopedRole)) {
            return new AssignedStoreInfo(null, null, List.of(), List.of());
        }

        List<UserStoreRole> userStoreRoles = userStoreRoleMapper.selectList(new LambdaQueryWrapper<UserStoreRole>()
            .eq(UserStoreRole::getUserId, userId)
            .in(UserStoreRole::getRoleCode, roles.stream().filter(this::isStoreScopedRole).toList())
            .eq(UserStoreRole::getStatus, ACTIVE)
            .orderByAsc(UserStoreRole::getStoreId));
        if (userStoreRoles.isEmpty()) {
            return new AssignedStoreInfo(null, null, List.of(), List.of());
        }

        List<Long> storeIds = userStoreRoles.stream()
            .map(UserStoreRole::getStoreId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (storeIds.isEmpty()) {
            return new AssignedStoreInfo(null, null, List.of(), List.of());
        }

        List<Store> stores = storeMapper.selectBatchIds(storeIds);
        if (stores == null || stores.isEmpty()) {
            return new AssignedStoreInfo(null, null, List.of(), List.of());
        }

        List<Store> orderedStores = storeIds.stream()
            .map(storeId -> stores.stream()
                .filter(store -> Objects.equals(store.getId(), storeId))
                .findFirst()
                .orElse(null))
            .filter(Objects::nonNull)
            .filter(store -> !Objects.equals(store.getDeleted(), 1))
            .toList();
        if (orderedStores.isEmpty()) {
            return new AssignedStoreInfo(null, null, List.of(), List.of());
        }

        List<AuthAssignedStore> assignedStores = orderedStores.stream()
            .map(store -> new AuthAssignedStore(store.getId(), store.getName(), store.getCity(), store.getAddress()))
            .toList();
        List<String> storeNames = assignedStores.stream()
            .map(AuthAssignedStore::name)
            .filter(name -> name != null && !name.isBlank())
            .toList();
        AuthAssignedStore primaryStore = assignedStores.get(0);
        return new AssignedStoreInfo(
            primaryStore.id(),
            primaryStore.name(),
            storeNames,
            assignedStores
        );
    }

    private boolean isStoreScopedRole(String role) {
        return STAFF.equals(role) || STORE_MANAGER.equals(role) || CAT_CARETAKER.equals(role);
    }

    private record AssignedStoreInfo(Long storeId, String storeName, List<String> storeNames, List<AuthAssignedStore> stores) {
    }

    private List<String> loadRoleCodes(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds).stream().map(Role::getCode).toList();
    }

    private void ensureUnique(String column, String value, int code, String message) {
        long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getDeleted, 0)
            .eq("username".equals(column), User::getUsername, value)
            .eq("phone".equals(column), User::getPhone, value)
            .eq("email".equals(column), User::getEmail, value));
        if (count > 0) {
            throw new BizException(code, message);
        }
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
