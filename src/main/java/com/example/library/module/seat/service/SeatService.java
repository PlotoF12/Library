package com.example.library.module.seat.service;

import com.example.library.common.response.PageResult;
import com.example.library.module.seat.dto.*;

import java.util.List;

/**
 * 座位服务接口
 */
public interface SeatService {

    /** 获取自习室列表 */
    PageResult<RoomVO> listRooms(Integer floor, Integer status, int page, int size);

    /** 获取自习室详情（含座位布局） */
    RoomVO getRoomDetail(Long roomId);

    /** 查询可用座位（支持偏好筛选） */
    PageResult<SeatVO> listAvailableSeats(Long roomId, String date, String startTime,
                                           String endTime, Integer hasPower,
                                           Integer isWindow, Integer isSingle,
                                           int page, int size);

    /** 管理员：新增座位 */
    SeatVO createSeat(SeatCreateRequest request);

    /** 管理员：更新座位状态 */
    SeatVO updateSeat(Long seatId, SeatUpdateRequest request);

    /** 管理员：删除座位 */
    void deleteSeat(Long seatId);
}
