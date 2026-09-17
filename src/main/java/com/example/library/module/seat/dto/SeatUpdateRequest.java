package com.example.library.module.seat.dto;

import lombok.Data;

/** 管理员更新座位状态请求DTO */
@Data
public class SeatUpdateRequest {
    private Integer status;
    private String statusReason;

    private Integer hasPower;
    private Integer isWindow;
    private Integer isSingle;
}
