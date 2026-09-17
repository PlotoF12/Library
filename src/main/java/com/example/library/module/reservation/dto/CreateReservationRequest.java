package com.example.library.module.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建预约请求DTO */
@Data
public class CreateReservationRequest {

    @NotNull(message = "座位ID不能为空")
    private Long seatId;

    @NotBlank(message = "预约日期不能为空")
    private String reserveDate;  // yyyy-MM-dd

    @NotBlank(message = "开始时间不能为空")
    private String startTime;    // HH:mm

    @NotBlank(message = "结束时间不能为空")
    private String endTime;      // HH:mm
}
