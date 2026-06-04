package com.nekocafe.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.order.entity.FoodOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FoodOrderItemMapper extends BaseMapper<FoodOrderItem> {
}
