package com.nekocafe.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.entity.UserStoreRole;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.store.mapper.UserStoreRoleMapper;
import com.nekocafe.user.entity.Role;
import com.nekocafe.user.entity.User;
import com.nekocafe.user.entity.UserRole;
import com.nekocafe.user.mapper.RoleMapper;
import com.nekocafe.user.mapper.UserMapper;
import com.nekocafe.user.mapper.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final String STORE_MANAGER = "STORE_MANAGER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final StoreMapper storeMapper;
    private final UserStoreRoleMapper userStoreRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserMapper userMapper, RoleMapper roleMapper,
                        UserRoleMapper userRoleMapper, StoreMapper storeMapper,
                        UserStoreRoleMapper userStoreRoleMapper,
                        PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.storeMapper = storeMapper;
        this.userStoreRoleMapper = userStoreRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ---- users ----

    public List<AdminUserRow> users() {
        List<User> allUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0).orderByDesc(User::getCreatedAt));
        if (allUsers.isEmpty()) return List.of();

        List<Long> userIds = allUsers.stream().map(User::getId).toList();
        List<UserRole> allUserRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
        List<Long> roleIds = allUserRoles.stream().map(UserRole::getRoleId).distinct().toList();
        final Map<Long, String> roleIdToCode;
        if (!roleIds.isEmpty()) {
            roleIdToCode = roleMapper.selectBatchIds(roleIds).stream()
                    .collect(Collectors.toMap(Role::getId, Role::getCode));
        } else {
            roleIdToCode = Map.of();
        }

        Map<Long, List<String>> userRolesMap = allUserRoles.stream()
                .collect(Collectors.groupingBy(UserRole::getUserId,
                        Collectors.mapping(ur -> roleIdToCode.getOrDefault(ur.getRoleId(), "UNKNOWN"),
                                Collectors.toList())));

        return allUsers.stream()
                .map(u -> new AdminUserRow(u.getId(), u.getUsername(), u.getNickname(),
                        u.getPhone(), u.getEmail(), u.getStatus(),
                        userRolesMap.getOrDefault(u.getId(), List.of())))
                .toList();
    }

    // ---- roles ----

    public List<AdminRoleRow> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId))
                .stream()
                .map(r -> new AdminRoleRow(r.getId(), r.getCode(), r.getName(), r.getDescription()))
                .toList();
    }

    // ---- stores ----

    public List<AdminStoreRow> stores() {
        return storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getDeleted, 0).orderByAsc(Store::getId))
                .stream()
                .map(s -> new AdminStoreRow(s.getId(), s.getName(), s.getCity(), s.getAddress(), s.getStatus()))
                .toList();
    }

    // ---- store managers ----

    public List<StoreManagerRow> storeManagers() {
        List<UserStoreRole> mappings = userStoreRoleMapper.selectList(
                new LambdaQueryWrapper<UserStoreRole>()
                        .eq(UserStoreRole::getRoleCode, STORE_MANAGER)
                        .eq(UserStoreRole::getStatus, "ACTIVE"));
        if (mappings.isEmpty()) return List.of();

        List<Long> userIds = mappings.stream().map(UserStoreRole::getUserId).distinct().toList();
        List<Long> storeIds = mappings.stream().map(UserStoreRole::getStoreId).distinct().toList();

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Store> storeMap = storeMapper.selectBatchIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, s -> s));

        return mappings.stream().map(m -> {
            User user = userMap.get(m.getUserId());
            Store store = storeMap.get(m.getStoreId());
            return new StoreManagerRow(
                    m.getId(), m.getUserId(),
                    user != null ? user.getUsername() : "未知",
                    user != null ? user.getNickname() : "未知",
                    m.getStoreId(),
                    store != null ? store.getName() : "未知",
                    m.getStatus(), m.getCreatedAt());
        }).toList();
    }

    @Transactional
    public void assignStoreManager(Long userId, Long storeId, Long createdBy) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BizException(4001, "用户不存在");
        }
        Store store = storeMapper.selectOne(
                new LambdaQueryWrapper<Store>().eq(Store::getId, storeId).eq(Store::getDeleted, 0));
        if (store == null) {
            throw new BizException(4002, "门店不存在");
        }

        Role managerRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, STORE_MANAGER));
        if (managerRole == null) {
            throw new BizException(4003, "店长角色未初始化");
        }
        Long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, managerRole.getId()));
        if (count == 0) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(managerRole.getId());
            userRoleMapper.insert(ur);
        }

        UserStoreRole existing = userStoreRoleMapper.selectOne(
                new LambdaQueryWrapper<UserStoreRole>()
                        .eq(UserStoreRole::getUserId, userId)
                        .eq(UserStoreRole::getStoreId, storeId)
                        .eq(UserStoreRole::getRoleCode, STORE_MANAGER));
        if (existing != null) {
            if ("ACTIVE".equals(existing.getStatus())) {
                throw new BizException(4004, "该用户已是此门店的店长");
            }
            existing.setStatus("ACTIVE");
            existing.setDismissedBy(null);
            existing.setDismissedAt(null);
            existing.setDismissReason(null);
            existing.setCreatedBy(createdBy);
            userStoreRoleMapper.updateById(existing);
            return;
        }

        UserStoreRole usr = new UserStoreRole();
        usr.setUserId(userId);
        usr.setStoreId(storeId);
        usr.setRoleCode(STORE_MANAGER);
        usr.setStatus("ACTIVE");
        usr.setCreatedBy(createdBy);
        userStoreRoleMapper.insert(usr);
    }

    @Transactional
    public void removeStoreManager(Long userId, Long storeId, Long dismissedBy) {
        UserStoreRole existing = userStoreRoleMapper.selectOne(
                new LambdaQueryWrapper<UserStoreRole>()
                        .eq(UserStoreRole::getUserId, userId)
                        .eq(UserStoreRole::getStoreId, storeId)
                        .eq(UserStoreRole::getRoleCode, STORE_MANAGER));
        if (existing == null) {
            throw new BizException(4005, "该用户不是此门店的店长");
        }
        existing.setStatus("DISMISSED");
        existing.setDismissedBy(dismissedBy);
        existing.setDismissedAt(LocalDateTime.now());
        userStoreRoleMapper.updateById(existing);
    }

    /**
     * Create a new user and assign as store manager in a single transaction.
     */
    @Transactional
    public AdminUserRow createUserAndAssignStoreManager(CreateStoreManagerRequest request, Long createdBy) {
        // validate
        if (request.username() == null || request.username().trim().isEmpty()) {
            throw new BizException(400, "用户名不能为空");
        }
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new BizException(400, "密码不能为空");
        }
        if (request.nickname() == null || request.nickname().trim().isEmpty()) {
            throw new BizException(400, "昵称不能为空");
        }
        if (request.storeId() == null) {
            throw new BizException(400, "请选择门店");
        }

        String username = request.username().trim();
        String nickname = request.nickname().trim();
        String phone = request.phone() != null ? request.phone().trim() : null;
        String email = request.email() != null ? request.email().trim() : null;

        // check uniqueness
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0) {
            throw new BizException(4006, "用户名已存在");
        }
        if (phone != null && userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)) > 0) {
            throw new BizException(4007, "手机号已存在");
        }
        if (email != null && userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)) > 0) {
            throw new BizException(4008, "邮箱已存在");
        }

        // create user
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        // assign STORE_MANAGER role
        Role managerRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, STORE_MANAGER));
        if (managerRole == null) {
            throw new BizException(4003, "店长角色未初始化");
        }
        UserRole ur = new UserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(managerRole.getId());
        userRoleMapper.insert(ur);

        // assign to store
        UserStoreRole usr = new UserStoreRole();
        usr.setUserId(user.getId());
        usr.setStoreId(request.storeId());
        usr.setRoleCode(STORE_MANAGER);
        usr.setStatus("ACTIVE");
        usr.setCreatedBy(createdBy);
        userStoreRoleMapper.insert(usr);

        return new AdminUserRow(user.getId(), user.getUsername(), user.getNickname(),
                user.getPhone(), user.getEmail(), user.getStatus(),
                List.of(STORE_MANAGER));
    }

    // ---- user status ----

    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BizException(4001, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    // ---- DTOs ----

    public record AdminUserRow(Long id, String username, String nickname, String phone,
                               String email, String status, List<String> roles) {}

    public record AdminRoleRow(Long id, String code, String name, String description) {}

    public record AdminStoreRow(Long id, String name, String city, String address, String status) {}

    public record StoreManagerRow(Long id, Long userId, String username, String nickname,
                                  Long storeId, String storeName, String status,
                                  LocalDateTime createdAt) {}

    public record CreateStoreManagerRequest(
            String username, String password, String nickname,
            String phone, String email, Long storeId) {}
}
