package com.example.library.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 爽约惩罚记录实体（JPA + MyBatis Plus 双映射）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "violation_record")
@TableName("violation_record")
public class ViolationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "violation_type", nullable = false)
    private Integer violationType;

    @Column(name = "penalty_start", nullable = false)
    private LocalDate penaltyStart;

    @Column(name = "penalty_end", nullable = false)
    private LocalDate penaltyEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
