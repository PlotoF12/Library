package com.example.library.module.statistics.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 爽约统计报表VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationReportVO {
    private int totalViolations;
    private int currentlyBanned;
    private List<TopViolator> topViolators;

    @Data
    @Builder
@NoArgsConstructor
@AllArgsConstructor
    public static class TopViolator {
        private String studentNo;
        private String studentName;
        private int violationCount;
        private LocalDateTime bannedUntil;
        private boolean isCurrentlyBanned;
    }
}
