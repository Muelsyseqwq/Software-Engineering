package com.nekocafe.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.payment.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    @Select("SELECT o.store_id, COALESCE(SUM(p.amount), 0) AS total "
            + "FROM payment_record p "
            + "JOIN food_order o ON p.order_id = o.id AND o.deleted = 0 "
            + "WHERE p.status = 'SUCCESS' AND DATE(p.paid_at) = CURDATE() "
            + "GROUP BY o.store_id")
    List<Map<String, Object>> selectStoreRevenueToday();

    @Select("SELECT DATE(p.paid_at) AS stat_date, COALESCE(SUM(p.amount), 0) AS total "
            + "FROM payment_record p "
            + "JOIN food_order o ON p.order_id = o.id AND o.deleted = 0 "
            + "WHERE p.status = 'SUCCESS' AND o.store_id = #{storeId} "
            + "AND DATE(p.paid_at) BETWEEN #{start} AND #{end} "
            + "GROUP BY DATE(p.paid_at) "
            + "ORDER BY stat_date")
    List<Map<String, Object>> selectStoreRevenueByDateRange(@Param("storeId") Long storeId,
                                                             @Param("start") String start,
                                                             @Param("end") String end);

    @Select("SELECT COALESCE(SUM(p.amount), 0) - COALESCE(( "
            + "SELECT SUM(r.amount) "
            + "FROM refund_request r "
            + "JOIN food_order ro ON r.order_id = ro.id AND ro.deleted = 0 "
            + "WHERE ro.store_id = #{storeId} AND r.status IN ('APPROVED', 'REFUNDED') "
            + "AND r.reviewed_at >= #{startAt} AND r.reviewed_at < #{endAt}"
            + "), 0) AS revenue, COUNT(DISTINCT p.order_id) AS paid_order_count "
            + "FROM payment_record p "
            + "JOIN food_order o ON p.order_id = o.id AND o.deleted = 0 "
            + "WHERE p.status = 'SUCCESS' AND o.store_id = #{storeId} "
            + "AND p.paid_at >= #{startAt} AND p.paid_at < #{endAt}")
    Map<String, Object> selectStoreRevenueSummary(@Param("storeId") Long storeId,
                                                  @Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt);

    /**
     * Revenue trend for all stores (storeId=null) or a specific store.
     */
    @Select("<script>"
            + "SELECT DATE(p.paid_at) AS stat_date, COALESCE(SUM(p.amount), 0) AS total "
            + "FROM payment_record p "
            + "JOIN food_order o ON p.order_id = o.id AND o.deleted = 0 "
            + "WHERE p.status = 'SUCCESS' "
            + "<if test='storeId != null'>AND o.store_id = #{storeId} </if>"
            + "AND DATE(p.paid_at) BETWEEN #{start} AND #{end} "
            + "GROUP BY DATE(p.paid_at) "
            + "ORDER BY stat_date"
            + "</script>")
    List<Map<String, Object>> selectStoreRevenueByDateRangeAll(@Param("storeId") Long storeId,
                                                                @Param("start") String start,
                                                                @Param("end") String end);
}
