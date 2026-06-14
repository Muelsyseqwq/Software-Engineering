package com.nekocafe.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.order.entity.FoodOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FoodOrderMapper extends BaseMapper<FoodOrder> {
}
