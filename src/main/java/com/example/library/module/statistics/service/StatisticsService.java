package com.example.library.module.statistics.service;

import com.example.library.module.statistics.dto.ViolationReportVO;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /** 爽约统计报表 */
    ViolationReportVO getViolationReport(String startDate, String endDate);
}
