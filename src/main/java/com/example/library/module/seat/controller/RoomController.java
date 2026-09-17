package com.example.library.module.seat.controller;

import com.example.library.common.response.ApiResponse;
import com.example.library.common.response.PageResult;
import com.example.library.module.seat.dto.RoomVO;
import com.example.library.module.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 自习室控制器 — 自习室列表查询和详情查看。
 * 所有认证用户（学生和管理员）均可访问。
 */
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final SeatService seatService;

    /** 获取自习室列表 — 支持按楼层和状态筛选 */
    @GetMapping
    public ApiResponse<PageResult<RoomVO>> listRooms(
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false, defaultValue = "1") Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(seatService.listRooms(floor, status, page, size));
    }

    /**
     * 获取自习室详情 — 包含座位布局和各座位实时状态。
     * 返回 currentStatus 字段表示座位当前是可用/占用/维修。
     */
    @GetMapping("/{roomId}")
    public ApiResponse<RoomVO> getRoomDetail(@PathVariable Long roomId) {
        return ApiResponse.success(seatService.getRoomDetail(roomId));
    }
}
