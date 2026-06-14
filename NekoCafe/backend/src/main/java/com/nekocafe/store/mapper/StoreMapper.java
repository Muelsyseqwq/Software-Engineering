package com.nekocafe.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.store.entity.Store;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StoreMapper extends BaseMapper<Store> {
}
