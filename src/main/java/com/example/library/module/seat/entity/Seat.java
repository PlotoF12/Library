package com.example.library.module.seat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 座位实体 — 含偏好字段（为AI推荐预留）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "seat")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "seat_code", nullable = false, length = 20)
    private String seatCode;

    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;  // 1-可用 2-维修中 3-禁用

    @Column(name = "has_power", nullable = false)
    @Builder.Default
    private Integer hasPower = 0;  // 是否有电源: 0-无 1-有

    @Column(name = "is_window", nullable = false)
    @Builder.Default
    private Integer isWindow = 0;  // 是否靠窗: 0-否 1-是

    @Column(name = "is_single", nullable = false)
    @Builder.Default
    private Integer isSingle = 0;  // 是否单座: 0-否(联排) 1-是(独立)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
