package com.example.library.module.notification.controller;

import com.example.library.common.response.ApiResponse;
import com.example.library.common.response.PageResult;
import com.example.library.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器 — 查询通知历史。
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 查询通知历史 — 学生只能看自己的，管理员可以看所有。
     * 支持按通知类型筛选。
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ApiResponse<PageResult<?>> listNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 管理员查询所有通知时 userId 用 null
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        return ApiResponse.success(
                notificationService.listNotifications(isAdmin ? null : userId, type, page, size));
    }
}
