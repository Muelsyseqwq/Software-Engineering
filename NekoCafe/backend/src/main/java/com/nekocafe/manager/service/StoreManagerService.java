package com.nekocafe.manager.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StoreManagerService {

    private final List<ManagerTableRow> tables = new ArrayList<>(List.of(
        new ManagerTableRow(1L, "A01", 2, "猫咪互动区", "AVAILABLE"),
        new ManagerTableRow(2L, "B01", 4, "安静用餐区", "AVAILABLE")
    ));

    public ManagerStoreInfo store() {
        return new ManagerStoreInfo(1L, "NekoCafé 春日店", "杭州", "西湖区猫爪路 18 号", "0571-88888888", "OPEN");
    }

    public void updateStoreStatus(String status) {
        // 框架占位：后续由店长模块负责人接入门店营业状态更新。
    }

    public List<ManagerTableRow> tables() {
        return tables;
    }

    public ManagerTableRow createTable(ManagerTableRow payload) {
        long nextId = tables.stream().mapToLong(ManagerTableRow::id).max().orElse(0L) + 1;
        ManagerTableRow created = new ManagerTableRow(nextId, payload.tableNo(), payload.capacity(), payload.area(), payload.status());
        tables.add(created);
        return created;
    }

    public ManagerTableRow updateTable(Long id, ManagerTableRow payload) {
        ManagerTableRow updated = new ManagerTableRow(id, payload.tableNo(), payload.capacity(), payload.area(), payload.status());
        tables.removeIf(table -> table.id().equals(id));
        tables.add(updated);
        return updated;
    }

    public List<ManagerReservationRow> reservations() {
        return List.of(
            new ManagerReservationRow(1L, "R20260604001", "猫爪布丁", 2, "2026-06-04 18:00", "待到店")
        );
    }

    public record ManagerStoreInfo(Long id, String name, String city, String address, String phone, String status) {
    }

    public record ManagerTableRow(Long id, String tableNo, int capacity, String area, String status) {
    }

    public record ManagerReservationRow(Long id, String reservationNo, String customerName, int partySize, String slotTime, String status) {
    }
}
