package com.nekocafe.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.user.entity.MemberAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MemberAccountMapper extends BaseMapper<MemberAccount> {

    @Update("UPDATE member_account SET points = points - #{cost} WHERE id = #{accountId} AND points >= #{cost}")
    int deductPointsIfEnough(@Param("accountId") Long accountId, @Param("cost") Integer cost);
}
