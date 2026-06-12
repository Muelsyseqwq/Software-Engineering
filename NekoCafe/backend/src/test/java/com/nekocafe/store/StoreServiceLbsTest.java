package com.nekocafe.store;

import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import com.nekocafe.store.service.StoreService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StoreServiceLbsTest {

    @Test
    void nearbyStoresAreSortedByDistanceFromUserLocation() {
        StoreMapper storeMapper = mock(StoreMapper.class);
        DiningTableMapper diningTableMapper = mock(DiningTableMapper.class);
        StoreService storeService = new StoreService(storeMapper, diningTableMapper);

        Store farStore = store(1L, "远方猫咖", "杭州", "远方路", "OPEN", "休闲撸猫", "30.3000000", "120.3000000");
        Store nearStore = store(2L, "附近猫咖", "杭州", "附近路", "OPEN", "近距离猫咖", "30.2460000", "120.1500000");
        Store noLocationStore = store(3L, "无坐标猫咖", "杭州", "未知路", "OPEN", "缺少坐标", null, null);

        when(storeMapper.selectList(any())).thenReturn(List.of(farStore, nearStore, noLocationStore));
        when(diningTableMapper.selectCount(any())).thenReturn(3L);

        List<StoreService.NearbyStoreResponse> result = storeService.nearbyStores(
            new BigDecimal("30.2450000"),
            new BigDecimal("120.1510000")
        );

        assertThat(result).extracting(StoreService.NearbyStoreResponse::name)
            .containsExactly("附近猫咖", "远方猫咖");
        assertThat(result.get(0).distanceKm()).isLessThan(result.get(1).distanceKm());
        assertThat(result.get(0).distanceText()).endsWith("km");
    }

    private Store store(Long id, String name, String city, String address, String status, String description, String latitude, String longitude) {
        Store store = new Store();
        store.setId(id);
        store.setName(name);
        store.setCity(city);
        store.setAddress(address);
        store.setPhone("0571-10000000");
        store.setOpeningTime(LocalTime.of(10, 0));
        store.setClosingTime(LocalTime.of(22, 0));
        store.setStatus(status);
        store.setDescription(description);
        store.setLatitude(latitude == null ? null : new BigDecimal(latitude));
        store.setLongitude(longitude == null ? null : new BigDecimal(longitude));
        return store;
    }
}
