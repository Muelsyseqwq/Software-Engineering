package com.nekocafe.cat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.cat.entity.CatHealthRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CatHealthRecordMapper extends BaseMapper<CatHealthRecord> {
}
