package com.nekocafe.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.reservation.entity.ReservationSlot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReservationSlotMapper extends BaseMapper<ReservationSlot> {
}
