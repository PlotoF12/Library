package com.example.library.module.statistics.service.impl;

import com.example.library.module.statistics.dto.ViolationReportVO;
import com.example.library.module.statistics.mapper.StatisticsMapper;
import com.example.library.module.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计服务实现 — 爽约报表、座位利用率等聚合统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    /**
     * 获取爽约统计报表。
     * 包含：爽约总数、当前被禁人数、爽约TOP榜。
     */
    @Override
    public ViolationReportVO getViolationReport(String startDate, String endDate) {
        LocalDate start = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate) : null;
        LocalDate today = LocalDate.now();

        // 爽约总数
        int totalViolations = statisticsMapper.countViolations(start, end);

        // 当前被禁人数
        int currentlyBanned = statisticsMapper.countCurrentlyBanned(today);

        // 爽约TOP榜（取前10）
        List<Map<String, Object>> topList = statisticsMapper.topViolators(start, end, 10);
        List<ViolationReportVO.TopViolator> topViolators = topList.stream()
                .map(m -> ViolationReportVO.TopViolator.builder()
                        .studentNo((String) m.get("student_no"))
                        .studentName((String) m.get("name"))
                        .violationCount(((Number) m.get("violation_count")).intValue())
                        .bannedUntil(m.get("banned_until") != null
                                ? ((java.sql.Timestamp) m.get("banned_until")).toLocalDateTime()
                                : null)
                        .isCurrentlyBanned(m.get("banned_until") != null)
                        .build())
                .collect(Collectors.toList());

        return ViolationReportVO.builder()
                .totalViolations(totalViolations)
                .currentlyBanned(currentlyBanned)
                .topViolators(topViolators)
                .build();
    }
}
