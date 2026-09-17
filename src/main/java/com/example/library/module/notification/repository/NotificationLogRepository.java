package com.example.library.module.notification.repository;

import com.example.library.module.notification.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知记录 JPA Repository — 短信通知的持久化操作。
 * 用于异步短信发送的落库、状态更新、补偿重发查询。
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /** 按学生分页查询（按时间倒序） */
    Page<NotificationLog> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    /** 按学生+类型分页查询 */
    Page<NotificationLog> findByStudentIdAndTypeOrderByCreatedAtDesc(
            Long studentId, Integer type, Pageable pageable);

    /** 按学生查询全部（非分页） */
    List<NotificationLog> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /** 按发送状态查询 — 用于补偿重发（0=待发送, 2=失败） */
    List<NotificationLog> findByStatus(Integer status);

    /** 检查短时间内是否已发送过同类通知 — 防重复发送 */
    boolean existsByStudentIdAndTypeAndCreatedAtAfter(Long studentId, Integer type,
                                                       java.time.LocalDateTime after);
}
