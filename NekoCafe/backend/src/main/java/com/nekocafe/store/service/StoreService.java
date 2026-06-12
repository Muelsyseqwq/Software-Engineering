package com.nekocafe.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.common.exception.BizException;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Comparator;
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

    public List<NearbyStoreResponse> nearbyStores(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new BizException(2002, "请提供当前位置");
        }
        return storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getDeleted, 0)
                .eq(Store::getStatus, OPEN)
                .orderByAsc(Store::getCity)
                .orderByAsc(Store::getId))
            .stream()
            .filter(store -> store.getLatitude() != null && store.getLongitude() != null)
            .map(store -> toNearbyResponse(store, countAvailableTables(store.getId()), distanceKm(latitude, longitude, store.getLatitude(), store.getLongitude())))
            .sorted(Comparator.comparing(NearbyStoreResponse::distanceKm).thenComparing(NearbyStoreResponse::id))
            .toList();
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

    private NearbyStoreResponse toNearbyResponse(Store store, long availableTableCount, BigDecimal distanceKm) {
        return new NearbyStoreResponse(
            store.getId(),
            store.getName(),
            store.getCity(),
            store.getAddress(),
            store.getPhone(),
            store.getOpeningTime(),
            store.getClosingTime(),
            store.getStatus(),
            store.getDescription(),
            availableTableCount,
            store.getLatitude(),
            store.getLongitude(),
            distanceKm,
            distanceKm + "km"
        );
    }

    private BigDecimal distanceKm(BigDecimal fromLatitude, BigDecimal fromLongitude, BigDecimal toLatitude, BigDecimal toLongitude) {
        double lat1 = Math.toRadians(fromLatitude.doubleValue());
        double lat2 = Math.toRadians(toLatitude.doubleValue());
        double deltaLat = Math.toRadians(toLatitude.subtract(fromLatitude).doubleValue());
        double deltaLng = Math.toRadians(toLongitude.subtract(fromLongitude).doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(6371.0088 * c).setScale(2, RoundingMode.HALF_UP);
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

    public record NearbyStoreResponse(
        Long id,
        String name,
        String city,
        String address,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        String status,
        String description,
        long availableTableCount,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal distanceKm,
        String distanceText
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
