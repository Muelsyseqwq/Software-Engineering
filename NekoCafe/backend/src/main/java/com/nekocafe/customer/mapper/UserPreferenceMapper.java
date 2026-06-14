package com.nekocafe.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.customer.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}
