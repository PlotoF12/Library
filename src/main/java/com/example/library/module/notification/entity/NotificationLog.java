package com.example.library.module.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 短信通知记录实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private Integer type;
    // 1-预约成功 2-签到提醒 3-爽约 4-超时提醒 5-惩罚通知

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Integer status = 0;  // 0-待发送 1-已发送 2-失败

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
