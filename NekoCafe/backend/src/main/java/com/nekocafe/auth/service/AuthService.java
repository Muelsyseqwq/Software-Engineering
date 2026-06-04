package com.nekocafe.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.auth.dto.AuthResponse;
import com.nekocafe.auth.dto.AuthUserResponse;
import com.nekocafe.auth.dto.LoginRequest;
import com.nekocafe.auth.dto.RegisterRequest;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.security.AuthPrincipal;
import com.nekocafe.security.JwtService;
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
import java.util.List;
import java.util.Objects;

@Service
public class AuthService {

    private static final String ACTIVE = "ACTIVE";
    private static final String CUSTOMER = "CUSTOMER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final MemberAccountMapper memberAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserMapper userMapper,
        RoleMapper roleMapper,
        UserRoleMapper userRoleMapper,
        MemberAccountMapper memberAccountMapper,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.memberAccountMapper = memberAccountMapper;
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

    private AuthResponse buildAuthResponse(User user, List<String> roles) {
        AuthUserResponse authUser = toUserResponse(user, roles);
        return new AuthResponse(jwtService.generateToken(authUser), "Bearer", jwtService.getExpiresAt(), authUser);
    }

    private AuthUserResponse toUserResponse(User user, List<String> roles) {
        return new AuthUserResponse(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getPhone(),
            user.getEmail(),
            roles
        );
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
}
