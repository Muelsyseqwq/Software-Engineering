package com.nekocafe.queue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.queue.entity.WaitingQueueCounter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface WaitingQueueCounterMapper extends BaseMapper<WaitingQueueCounter> {

    @Insert("""
        INSERT INTO waiting_queue_counter (store_id, queue_date)
        VALUES (#{storeId}, #{queueDate})
        ON DUPLICATE KEY UPDATE id = id
        """)
    void ensureCounter(@Param("storeId") Long storeId, @Param("queueDate") LocalDate queueDate);

    @Select("""
        SELECT *
        FROM waiting_queue_counter
        WHERE store_id = #{storeId}
          AND queue_date = #{queueDate}
          AND deleted = 0
        LIMIT 1
        FOR UPDATE
        """)
    WaitingQueueCounter selectForUpdate(@Param("storeId") Long storeId, @Param("queueDate") LocalDate queueDate);
}
