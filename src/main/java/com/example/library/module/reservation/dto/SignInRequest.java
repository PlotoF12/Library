package com.example.library.module.reservation.dto;

import lombok.Data;

/**
 * 签到请求DTO
 * 模拟扫码确认 — 通过 reservationId 直接签到
 */
@Data
public class SignInRequest {
    // 当前为空；预留 signToken 字段用于将来真实二维码场景
}
