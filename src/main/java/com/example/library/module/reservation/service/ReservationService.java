package com.example.library.module.reservation.service;

import com.example.library.common.response.PageResult;
import com.example.library.module.reservation.dto.*;

/**
 * 预约服务接口 — 核心业务
 */
public interface ReservationService {

    /** 创建预约（并发安全） */
    ReservationVO createReservation(Long studentId, CreateReservationRequest request,
                                     String idempotencyKey);

    /** 模拟签到确认（乐观锁） */
    ReservationVO signIn(Long reservationId, Long studentId);

    /** 确认离座（乐观锁） */
    ReservationVO leave(Long reservationId, Long studentId);

    /** 取消预约 */
    ReservationVO cancel(Long reservationId, Long studentId, String reason);

    /** 查询个人预约 */
    PageResult<ReservationVO> listMyReservations(Long studentId, Integer status,
                                                  String date, int page, int size);

    /** 获取预约详情 */
    ReservationVO getDetail(Long reservationId, Long studentId, boolean isAdmin);

    /** 管理员：查询所有预约 */
    PageResult<ReservationVO> listAllReservations(String date, Integer status, Long roomId,
                                                   String studentNo, String phone,
                                                   Boolean onlyNoShow, int page, int size);

    /** 管理员：按学号或手机号搜索学生名下的待签到/使用中预约 */
    PageResult<ReservationVO> searchByStudent(String keyword, int page, int size);

    /** 管理员：确认学生签到（无需学生本人鉴权） */
    ReservationVO adminSignIn(Long reservationId);

    /** 管理员：确认学生离座（无需学生本人鉴权） */
    ReservationVO adminLeave(Long reservationId);

    /** 爽约扫描（定时任务调用） */
    void checkNoShowReservations();

    /** 超时释放扫描（定时任务调用） */
    void checkTimeoutReservations();
}
