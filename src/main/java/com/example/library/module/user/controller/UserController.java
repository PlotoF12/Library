package com.example.library.module.user.controller;

import com.example.library.common.response.ApiResponse;
import com.example.library.module.user.dto.StudentProfileVO;
import com.example.library.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学生用户控制器 — 个人中心相关接口。
 */
@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class UserController {

    private final UserService userService;

    /** 获取个人信息 — 含爽约统计和预约状态 */
    @GetMapping("/profile")
    public ApiResponse<StudentProfileVO> getProfile(@AuthenticationPrincipal Long studentId) {
        return ApiResponse.success(userService.getProfile(studentId));
    }

    /** 更新手机号 — 需输入密码验证 */
    @PutMapping("/phone")
    public ApiResponse<Void> updatePhone(@AuthenticationPrincipal Long studentId,
                                          @RequestBody Map<String, String> body) {
        userService.updatePhone(studentId, body.get("phone"), body.get("password"));
        return ApiResponse.success("手机号更新成功", null);
    }

    /** 修改密码 — 验证旧密码后更新 */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal Long studentId,
                                             @RequestBody Map<String, String> body) {
        userService.changePassword(studentId, body.get("oldPassword"), body.get("newPassword"));
        return ApiResponse.success("密码修改成功", null);
    }
}
