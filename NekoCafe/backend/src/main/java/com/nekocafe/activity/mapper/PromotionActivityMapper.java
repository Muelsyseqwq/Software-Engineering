package com.nekocafe.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.activity.entity.PromotionActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromotionActivityMapper extends BaseMapper<PromotionActivity> {

    /** Uses PromotionActivityMapper.xml — raw SQL bypasses MyBatis-Plus logic-delete filter. */
    List<PromotionActivity> selectAllActivities(@Param("type") String type, @Param("status") String status);
}
