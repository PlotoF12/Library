package com.example.library.module.seat.controller;

import com.example.library.common.response.ApiResponse;
import com.example.library.common.response.PageResult;
import com.example.library.module.seat.dto.SeatVO;
import com.example.library.module.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 座位控制器 — 学生端座位查询接口。
 *
 * <p>支持按偏好筛选：hasPower（电源）、isWindow（靠窗）、isSingle（单座）。
 * 这些偏好字段同时为后续 AI 推荐功能提供数据接口。
 */
@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    /**
     * 查询可用座位 — 支持多条件筛选。
     *
     * @param roomId    自习室ID（可选）
     * @param date      预约日期（可选，默认今天）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param hasPower  是否有电源（可选，0/1）
     * @param isWindow  是否靠窗（可选，0/1）
     * @param isSingle  是否单座（可选，0/1）
     */
    @GetMapping
    public ApiResponse<PageResult<SeatVO>> listAvailableSeats(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer hasPower,
            @RequestParam(required = false) Integer isWindow,
            @RequestParam(required = false) Integer isSingle,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                seatService.listAvailableSeats(
                        roomId, date, startTime, endTime,
                        hasPower, isWindow, isSingle, page, size));
    }
}
