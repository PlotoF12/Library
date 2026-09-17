package com.example.library.module.seat.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 座位响应VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatVO {
    private Long seatId;
    private String seatCode;
    private Long roomId;
    private String roomCode;
    private String roomName;
    private Integer floor;
    private Integer status;
    private String statusText;
    private Integer hasPower;    // 是否有电源
    private Integer isWindow;    // 是否靠窗
    private Integer isSingle;    // 是否单座
    private boolean available;
    private String currentStatus; // AVAILABLE / IN_USE / RESERVED / MAINTENANCE / DISABLED
}
