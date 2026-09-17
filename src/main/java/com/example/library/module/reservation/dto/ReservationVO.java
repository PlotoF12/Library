package com.example.library.module.reservation.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 预约响应VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationVO {
    private Long reservationId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String phone;
    private Long seatId;
    private String seatCode;
    private Long roomId;
    private String roomCode;
    private String roomName;
    private LocalDate reserveDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;
    private String statusText;
    private String signToken;
    private LocalDateTime actualSignTime;
    private LocalDateTime actualLeaveTime;
    private String duration;
    private LocalDateTime deadline;
    private String deadlineNote;
    private Integer violationCount;
    private Boolean isNoShow;
    private LocalDateTime createdAt;
}
