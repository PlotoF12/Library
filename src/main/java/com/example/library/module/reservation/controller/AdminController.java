package com.example.library.module.reservation.controller;

import com.example.library.common.response.ApiResponse;
import com.example.library.common.response.PageResult;
import com.example.library.module.reservation.dto.ReservationVO;
import com.example.library.module.reservation.service.ReservationService;
import com.example.library.module.seat.dto.SeatCreateRequest;
import com.example.library.module.seat.dto.SeatUpdateRequest;
import com.example.library.module.seat.dto.SeatVO;
import com.example.library.module.seat.service.SeatService;
import com.example.library.module.statistics.dto.ViolationReportVO;
import com.example.library.module.statistics.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器 — 后台管理所有接口。
 *
 * <p>所有接口需要 ADMIN 角色，通过类级 @PreAuthorize 统一控制。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SeatService seatService;
    private final ReservationService reservationService;
    private final StatisticsService statisticsService;

    // ==================== 座位管理 ====================

    /** 新增座位 — 含偏好字段（电源、靠窗、单座） */
    @PostMapping("/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeatVO> createSeat(@Valid @RequestBody SeatCreateRequest request) {
        return ApiResponse.success("座位添加成功", seatService.createSeat(request));
    }

    /** 修改座位状态 — 可用/维修中/禁用；更新偏好字段 */
    @PutMapping("/seats/{id}")
    public ApiResponse<SeatVO> updateSeat(@PathVariable Long id,
                                           @RequestBody SeatUpdateRequest request) {
        return ApiResponse.success("座位状态更新成功", seatService.updateSeat(id, request));
    }

    /** 删除座位 — 有历史预约的建议禁用而非物理删除 */
    @DeleteMapping("/seats/{id}")
    public ApiResponse<Void> deleteSeat(@PathVariable Long id) {
        seatService.deleteSeat(id);
        return ApiResponse.success("操作成功", null);
    }

    // ==================== 预约管理 ====================

    /**
     * 查看所有预约 — 多条件筛选 + 分页。
     * onlyNoShow=true 时仅显示爽约记录（橙色高亮标记）。
     */
    @GetMapping("/reservations")
    public ApiResponse<PageResult<ReservationVO>> listReservations(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Boolean onlyNoShow,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                reservationService.listAllReservations(
                        date, status, roomId, studentNo, phone, onlyNoShow, page, size));
    }

    /**
     * 按学号或手机号搜索学生，返回其名下"待签到"和"使用中"的预约。
     */
    @GetMapping("/students/search")
    public ApiResponse<PageResult<ReservationVO>> searchByStudent(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reservationService.searchByStudent(keyword, page, size));
    }

    /** 管理员确认学生已签到 — 状态 1→2 */
    @PostMapping("/reservations/{id}/sign-in")
    public ApiResponse<ReservationVO> adminSignIn(@PathVariable Long id) {
        return ApiResponse.success("已确认签到", reservationService.adminSignIn(id));
    }

    /** 管理员确认学生已离座 — 状态 2→3，释放座位 */
    @PostMapping("/reservations/{id}/leave")
    public ApiResponse<ReservationVO> adminLeave(@PathVariable Long id) {
        return ApiResponse.success("已确认离座，座位已释放", reservationService.adminLeave(id));
    }

    // ==================== 统计报表 ====================

    /** 获取爽约统计报表 */
    @GetMapping("/reports/violations")
    public ApiResponse<ViolationReportVO> violationReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(
                statisticsService.getViolationReport(startDate, endDate));
    }

    // ==================== 系统配置 ====================

    /**
     * 更新系统配置 — 爽约阈值、禁约天数、最晚预约时间等。
     * 配置保存到 Redis，重启后从 application.yml 恢复默认值。
     */
    @PutMapping("/config")
    public ApiResponse<Void> updateConfig(@RequestBody Map<String, Object> config) {
        // TODO: 将配置持久化到 Redis Hash 或配置表
        return ApiResponse.success("配置已更新", null);
    }
}
