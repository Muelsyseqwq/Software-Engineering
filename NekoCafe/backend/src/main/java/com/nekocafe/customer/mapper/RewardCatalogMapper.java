package com.nekocafe.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.customer.entity.RewardCatalog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RewardCatalogMapper extends BaseMapper<RewardCatalog> {

    @Update("UPDATE reward_catalog SET stock = stock - 1 WHERE id = #{rewardId} AND stock IS NOT NULL AND stock > 0 AND deleted = 0")
    int decreaseStockIfAvailable(@Param("rewardId") Long rewardId);
}
