package com.example.library.module.statistics.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计 MyBatis Plus Mapper — 报表查询（纯自定义SQL，无需BaseMapper）
 */
@Mapper
public interface StatisticsMapper {

    /** 爽约总数 */
    int countViolations(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

    /** 当前被禁用户数 */
    int countCurrentlyBanned(@Param("today") LocalDate today);

    /** 爽约Top榜 */
    List<Map<String, Object>> topViolators(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("limit") int limit);

    /** 座位利用率统计 */
    List<Map<String, Object>> seatUsageRate(@Param("date") LocalDate date);
}
