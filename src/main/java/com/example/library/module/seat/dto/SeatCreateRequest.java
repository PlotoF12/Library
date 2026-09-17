package com.example.library.module.seat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 管理员创建座位请求DTO */
@Data
public class SeatCreateRequest {
    @NotNull
    private Long roomId;

    @NotBlank
    private String seatCode;

    private Integer hasPower = 0;
    private Integer isWindow = 0;
    private Integer isSingle = 0;
    private Integer status = 1;
}
