package com.nekocafe.queue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.queue.entity.WaitingQueueTicket;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WaitingQueueTicketMapper extends BaseMapper<WaitingQueueTicket> {
}
