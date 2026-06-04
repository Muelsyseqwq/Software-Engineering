package com.nekocafe.admin.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    public List<AdminUserRow> users() {
        return List.of(
            new AdminUserRow(1L, "customer_test", "顾客测试号", "13800000000", "customer@example.com", "ACTIVE", List.of("CUSTOMER")),
            new AdminUserRow(2L, "staff_test", "店员测试号", "13800000001", "staff@example.com", "ACTIVE", List.of("STAFF")),
            new AdminUserRow(3L, "admin_test", "管理员测试号", "13800000002", "admin@example.com", "ACTIVE", List.of("ADMIN"))
        );
    }

    public List<AdminRoleRow> roles() {
        return List.of(
            new AdminRoleRow(1L, "CUSTOMER", "顾客", "预约、点单、支付和查看个人记录"),
            new AdminRoleRow(2L, "STAFF", "店员", "签到、订单履约和现场服务"),
            new AdminRoleRow(3L, "STORE_MANAGER", "店长", "门店、桌位和本店经营管理"),
            new AdminRoleRow(4L, "HQ_OPERATOR", "总部运营", "跨门店数据看板和运营分析"),
            new AdminRoleRow(5L, "CAT_CARETAKER", "猫咪管家", "猫咪档案和健康状态维护"),
            new AdminRoleRow(6L, "ADMIN", "系统管理员", "账号、角色和系统配置管理")
        );
    }

    public List<AdminStoreRow> stores() {
        return List.of(
            new AdminStoreRow(1L, "NekoCafé 春日店", "杭州", "西湖区猫爪路 18 号", "OPEN"),
            new AdminStoreRow(2L, "NekoCafé 夏夜店", "上海", "徐汇区绒球街 6 号", "PREPARING")
        );
    }

    public record AdminUserRow(Long id, String username, String nickname, String phone, String email, String status, List<String> roles) {
    }

    public record AdminRoleRow(Long id, String code, String name, String description) {
    }

    public record AdminStoreRow(Long id, String name, String city, String address, String status) {
    }
}
