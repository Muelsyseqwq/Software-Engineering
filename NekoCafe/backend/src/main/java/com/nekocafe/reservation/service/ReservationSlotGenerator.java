package com.nekocafe.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nekocafe.reservation.entity.ReservationSlot;
import com.nekocafe.reservation.mapper.ReservationSlotMapper;
import com.nekocafe.store.entity.DiningTable;
import com.nekocafe.store.entity.Store;
import com.nekocafe.store.mapper.DiningTableMapper;
import com.nekocafe.store.mapper.StoreMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservationSlotGenerator implements ApplicationRunner {

    private static final String OPEN = "OPEN";
    private static final String AVAILABLE = "AVAILABLE";
    private static final List<SlotTemplate> TEMPLATES = List.of(
        new SlotTemplate(LocalTime.of(10, 0), LocalTime.of(12, 0)),
        new SlotTemplate(LocalTime.of(12, 0), LocalTime.of(14, 0)),
        new SlotTemplate(LocalTime.of(14, 0), LocalTime.of(16, 0)),
        new SlotTemplate(LocalTime.of(16, 0), LocalTime.of(18, 0)),
        new SlotTemplate(LocalTime.of(18, 0), LocalTime.of(20, 0)),
        new SlotTemplate(LocalTime.of(20, 0), LocalTime.of(22, 0))
    );

    private final StoreMapper storeMapper;
    private final DiningTableMapper diningTableMapper;
    private final ReservationSlotMapper slotMapper;

    public ReservationSlotGenerator(StoreMapper storeMapper, DiningTableMapper diningTableMapper, ReservationSlotMapper slotMapper) {
        this.storeMapper = storeMapper;
        this.diningTableMapper = diningTableMapper;
        this.slotMapper = slotMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureNextSevenDaysSlots();
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void ensureNextSevenDaysSlots() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
            .eq(Store::getDeleted, 0)
            .eq(Store::getStatus, OPEN));
        LocalDate today = LocalDate.now();
        for (Store store : stores) {
            for (int i = 0; i < 7; i++) {
                ensureSlotsForStoreAndDate(store.getId(), today.plusDays(i));
            }
        }
    }

    @Transactional
    public void ensureSlotsForStoreAndDate(Long storeId, LocalDate date) {
        if (storeId == null || date == null) {
            return;
        }
        List<DiningTable> tables = diningTableMapper.selectList(new LambdaQueryWrapper<DiningTable>()
            .eq(DiningTable::getStoreId, storeId)
            .eq(DiningTable::getDeleted, 0)
            .eq(DiningTable::getStatus, AVAILABLE)
            .orderByAsc(DiningTable::getId));
        for (DiningTable table : tables) {
            for (SlotTemplate template : TEMPLATES) {
                if (exists(storeId, table.getId(), date, template.startTime())) {
                    continue;
                }
                ReservationSlot slot = new ReservationSlot();
                slot.setStoreId(storeId);
                slot.setTableId(table.getId());
                slot.setSlotDate(date);
                slot.setStartTime(template.startTime());
                slot.setEndTime(template.endTime());
                slot.setCapacity(table.getCapacity());
                slot.setReservedCount(0);
                slot.setAvailableCount(1);
                slot.setStatus(AVAILABLE);
                slotMapper.insert(slot);
            }
        }
    }

    private boolean exists(Long storeId, Long tableId, LocalDate date, LocalTime startTime) {
        return slotMapper.selectCount(new LambdaQueryWrapper<ReservationSlot>()
            .eq(ReservationSlot::getStoreId, storeId)
            .eq(ReservationSlot::getTableId, tableId)
            .eq(ReservationSlot::getSlotDate, date)
            .eq(ReservationSlot::getStartTime, startTime)) > 0;
    }

    private record SlotTemplate(LocalTime startTime, LocalTime endTime) {
    }
}
