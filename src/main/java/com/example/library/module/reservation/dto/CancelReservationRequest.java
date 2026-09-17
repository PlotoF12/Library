package com.example.library.module.reservation.dto;

import lombok.Data;

/** 取消预约请求DTO */
@Data
public class CancelReservationRequest {
    private String reason;
}
