package com.example.library.module.user.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** 学生信息响应VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileVO {
    private Long studentId;
    private String studentNo;
    private String name;
    private String phone;        // 脱敏
    private Integer violationCount;
    private LocalDateTime bannedUntil;
    private boolean canReserve;
    private int todayReservationCount;
}
