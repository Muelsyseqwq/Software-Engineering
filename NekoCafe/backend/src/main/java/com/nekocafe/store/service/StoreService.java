package com.nekocafe.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
public class StoreService {

    private static final String OPEN = "OPEN";
    private static final String AVAILABLE = "AVAILABLE";

    private final StoreMapper storeMapper;
    private final DiningTableMapper diningTableMapper;

    public StoreService(StoreMapper storeMapper, DiningTableMapper diningTableMapper) {
        this.storeMapper = storeMapper;
        this.diningTableMapper = diningTableMapper;
    }

    public List<StoreSummaryResponse> listStores() {
        return storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getDeleted, 0)
                .eq(Store::getStatus, OPEN)
                .orderByAsc(Store::getCity)
                .orderByAsc(Store::getId))
            .stream()
            .map(store -> toSummary(store, countAvailableTables(store.getId())))
            .toList();
    }

    public StoreDetailResponse detail(Long id) {
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
            .eq(Store::getId, id)
            .eq(Store::getDeleted, 0)
            .last("LIMIT 1"));
        if (store == null) {
            throw new BizException(2001, "门店不存在");
        }
        List<TableSummaryResponse> tables = diningTableMapper.selectList(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getStoreId, id)
                .eq(DiningTable::getDeleted, 0)
                .orderByAsc(DiningTable::getArea)
                .orderByAsc(DiningTable::getTableNo))
            .stream()
            .map(this::toTable)
            .toList();
        return new StoreDetailResponse(
            store.getId(),
            store.getName(),
            store.getCity(),
            store.getAddress(),
            store.getPhone(),
            store.getOpeningTime(),
            store.getClosingTime(),
            store.getStatus(),
            store.getDescription(),
            tables
        );
    }

    private long countAvailableTables(Long storeId) {
        return diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
            .eq(DiningTable::getStoreId, storeId)
            .eq(DiningTable::getDeleted, 0)
            .eq(DiningTable::getStatus, AVAILABLE));
    }

    private StoreSummaryResponse toSummary(Store store, long availableTableCount) {
        return new StoreSummaryResponse(
            store.getId(),
            store.getName(),
            store.getCity(),
            store.getAddress(),
            store.getPhone(),
            store.getOpeningTime(),
            store.getClosingTime(),
            store.getStatus(),
            store.getDescription(),
            availableTableCount
        );
    }

    private TableSummaryResponse toTable(DiningTable table) {
        return new TableSummaryResponse(table.getId(), table.getTableNo(), table.getCapacity(), table.getArea(), table.getStatus());
    }

    public record StoreSummaryResponse(
        Long id,
        String name,
        String city,
        String address,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        String status,
        String description,
        long availableTableCount
    ) {
    }

    public record StoreDetailResponse(
        Long id,
        String name,
        String city,
        String address,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        String status,
        String description,
        List<TableSummaryResponse> tables
    ) {
    }

    public record TableSummaryResponse(Long id, String tableNo, Integer capacity, String area, String status) {
    }
}
