package com.nekocafe.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.user.entity.MemberAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberAccountMapper extends BaseMapper<MemberAccount> {
}
