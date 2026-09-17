package com.example.library.module.reservation.controller;

import com.example.library.common.annotation.RateLimit;
import com.example.library.common.response.ApiResponse;
import com.example.library.common.response.PageResult;
import com.example.library.module.reservation.dto.*;
import com.example.library.module.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 预约控制器 — 学生端预约核心接口。
 *
 * <p>所有接口需要 STUDENT 角色，创建预约有限流保护。
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 创建预约 — 幂等、并发安全。
     * 客户端需传入 Idempotency-Key（UUID），防止重复提交。
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimit(key = "reservation", limit = 10, window = 60)
    public ApiResponse<ReservationVO> create(
            @AuthenticationPrincipal Long studentId,
            @Valid @RequestBody CreateReservationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ReservationVO vo = reservationService.createReservation(
                studentId, request, idempotencyKey);
        return ApiResponse.success("预约成功，请按时到达并确认签到", vo);
    }

    /**
     * 模拟签到确认 — 学生点击"确认到达座位"后调用。
     * 成功后前端弹窗"已完成二维码扫描确认"。
     */
    @PostMapping("/{id}/sign-in")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ReservationVO> signIn(@PathVariable Long id,
                                              @AuthenticationPrincipal Long studentId) {
        ReservationVO vo = reservationService.signIn(id, studentId);
        return ApiResponse.success("已完成二维码扫描确认", vo);
    }

    /** 确认离座 — 释放座位 */
    @PostMapping("/{id}/leave")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ReservationVO> leave(@PathVariable Long id,
                                             @AuthenticationPrincipal Long studentId) {
        ReservationVO vo = reservationService.leave(id, studentId);
        return ApiResponse.success("离座确认成功", vo);
    }

    /** 取消预约（仅限未开始的预约） */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ReservationVO> cancel(@PathVariable Long id,
                                              @AuthenticationPrincipal Long studentId,
                                              @RequestBody(required = false) CancelReservationRequest request) {
        String reason = request != null ? request.getReason() : null;
        ReservationVO vo = reservationService.cancel(id, studentId, reason);
        return ApiResponse.success("预约已取消", vo);
    }

    /** 查询个人预约列表（支持状态和日期筛选） */
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PageResult<ReservationVO>> listMy(
            @AuthenticationPrincipal Long studentId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                reservationService.listMyReservations(studentId, status, date, page, size));
    }

    /** 获取预约详情 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ApiResponse<ReservationVO> getDetail(@PathVariable Long id,
                                                 @AuthenticationPrincipal Long userId) {
        // isAdmin 通过 SecurityContext 中的角色判断
        boolean isAdmin = false;
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null) {
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return ApiResponse.success(reservationService.getDetail(id, userId, isAdmin));
    }
}
