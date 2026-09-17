package com.example.library.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约记录实体 — 核心实体。
 * JPA 管理主键和乐观锁，MyBatis Plus 通过 @TableName 识别表映射。
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reservation")
@TableName("reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "reserve_date", nullable = false)
    private LocalDate reserveDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;
    // 1-待签到 2-使用中 3-已完成 4-已爽约 5-已取消

    @Column(name = "sign_token", length = 64)
    private String signToken;

    @Column(name = "actual_sign_time")
    private LocalDateTime actualSignTime;

    @Column(name = "actual_leave_time")
    private LocalDateTime actualLeaveTime;

    /** JPA 乐观锁版本号 */
    @jakarta.persistence.Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

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
