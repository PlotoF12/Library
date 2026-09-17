package com.example.library.module.seat.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 自习室响应VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomVO {
    private Long roomId;
    private String roomCode;
    private String name;
    private Integer floor;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer status;
    private String statusText;
    private List<SeatVO> seats;  // 仅详情接口填充
}
