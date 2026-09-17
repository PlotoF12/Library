package com.example.library.module.notification.service.impl;

import com.example.library.common.response.PageResult;
import com.example.library.module.notification.entity.NotificationLog;
import com.example.library.module.notification.event.ReservationEvent;
import com.example.library.module.notification.repository.NotificationLogRepository;
import com.example.library.module.notification.service.NotificationService;
import com.example.library.module.reservation.entity.Reservation;
import com.example.library.module.reservation.repository.ReservationRepository;
import com.example.library.module.user.entity.Student;
import com.example.library.module.user.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * 通知服务实现 — 异步短信发送 + 补偿重试。
 *
 * <p>设计原则：
 * <ul>
 *   <li>使用 @TransactionalEventListener(AFTER_COMMIT) 确保事务提交后才发送，
 *       避免事务回滚导致短信已发出但数据库无记录</li>
 *   <li>先落库（status=0），发送成功后更新（status=1），失败标记（status=2）</li>
 *   <li>定时任务补偿重试 status=0 和 status=2 的记录</li>
 *   <li>开发环境 sms-enabled=false 时仅打日志，不真实调用短信网关</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final ReservationRepository reservationRepository;
    private final StudentRepository studentRepository;

    @Value("${app.notification.sms-enabled:false}")
    private boolean smsEnabled;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 异步处理预约事件 → 构建短信内容 → 落库 → 发送。
     *
     * <p>@TransactionalEventListener(phase = AFTER_COMMIT)：
     * 确保只有事务成功提交后才执行短信发送。
     * 如果事务回滚（如并发预约失败），不发送短信。
     */
    @Override
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationEvent(ReservationEvent event) {
        try {
            Reservation reservation = reservationRepository.findById(event.getReservationId())
                    .orElse(null);
            if (reservation == null) {
                log.warn("通知事件关联的预约不存在: reservationId={}", event.getReservationId());
                return;
            }

            Student student = studentRepository.findById(reservation.getStudentId()).orElse(null);
            if (student == null) return;

            // 构建短信内容
            String content = buildSmsContent(event.getEventType(), reservation, student);

            // 先落库
            NotificationLog logEntity = NotificationLog.builder()
                    .studentId(student.getId())
                    .phone(student.getPhone())
                    .type(getNotificationType(event.getEventType()))
                    .content(content)
                    .status(0) // 待发送
                    .build();
            logEntity = notificationLogRepository.save(logEntity);

            // 发送短信
            boolean sent = sendSms(student.getPhone(), content);
            logEntity.setStatus(sent ? 1 : 2);
            logEntity.setSentAt(sent ? LocalDateTime.now() : null);
            notificationLogRepository.save(logEntity);

            log.info("短信通知: type={}, studentId={}, sent={}",
                    event.getEventType(), student.getId(), sent);

        } catch (Exception e) {
            log.error("通知处理异常: eventType={}, reservationId={}",
                    event.getEventType(), event.getReservationId(), e);
        }
    }

    /**
     * 查询通知历史 — 分页。
     */
    @Override
    public PageResult<?> listNotifications(Long studentId, Integer type, int page, int size) {
        PageRequest pr = PageRequest.of(page - 1, size);
        Page<NotificationLog> result;
        if (type != null) {
            result = notificationLogRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(
                    studentId, type, pr);
        } else {
            result = notificationLogRepository.findByStudentIdOrderByCreatedAtDesc(
                    studentId, pr);
        }

        return PageResult.of(
                result.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                page, size, result.getTotalElements());
    }

    /**
     * 补偿重发失败的短信 — 定时任务调用。
     * 查询 status=0 或 status=2 的记录，尝试重新发送。
     */
    @Override
    public void retryFailedNotifications() {
        var failedList = notificationLogRepository.findByStatus(0);
        failedList.addAll(notificationLogRepository.findByStatus(2));

        int retryCount = 0;
        for (NotificationLog logEntity : failedList) {
            if (retryCount >= 10) break; // 每次最多重试10条
            boolean sent = sendSms(logEntity.getPhone(), logEntity.getContent());
            logEntity.setStatus(sent ? 1 : 2);
            logEntity.setSentAt(sent ? LocalDateTime.now() : null);
            notificationLogRepository.save(logEntity);
            retryCount++;
        }
        if (retryCount > 0) {
            log.info("短信重发完成: 处理{}条", retryCount);
        }
    }

    // ==================== 私有方法 ====================

    /** 构建短信内容 */
    private String buildSmsContent(ReservationEvent.EventType eventType,
                                    Reservation r, Student s) {
        String seatInfo = r.getRoomId() + "自习室" + r.getSeatId() + "号座位";
        String timeInfo = r.getReserveDate() + " " + r.getStartTime() + "-" + r.getEndTime();

        return switch (eventType) {
            case RESERVATION_CREATED ->
                    String.format("【自习室预约】%s同学，您已成功预约%s，时段%s。请按时到达并确认签到。",
                            s.getName(), seatInfo, timeInfo);
            case SIGN_IN_REMIND ->
                    String.format("【签到提醒】%s同学，您预约的%s（%s）即将开始，请及时确认到达。",
                            s.getName(), seatInfo, timeInfo);
            case NO_SHOW ->
                    String.format("【爽约通知】%s同学，您未在规定时间内签到，该预约(%s)已被取消并记为爽约。",
                            s.getName(), timeInfo);
            case TIMEOUT ->
                    String.format("【超时提醒】%s同学，您的预约(%s)已超时，座位已被系统释放。",
                            s.getName(), timeInfo);
            case PENALTY_APPLIED ->
                    String.format("【处罚通知】%s同学，您因累计爽约已被限制预约10天。",
                            s.getName());
        };
    }

    /** 根据事件类型映射通知类型 */
    private int getNotificationType(ReservationEvent.EventType eventType) {
        return switch (eventType) {
            case RESERVATION_CREATED -> 1;
            case SIGN_IN_REMIND -> 2;
            case NO_SHOW -> 3;
            case TIMEOUT -> 4;
            case PENALTY_APPLIED -> 5;
        };
    }

    /** 发送短信（开发环境仅打日志） */
    private boolean sendSms(String phone, String content) {
        if (!smsEnabled) {
            log.info("[模拟短信] phone={}, content={}", maskPhone(phone), content);
            return true; // 开发环境模拟成功
        }
        // TODO: 对接真实短信网关（阿里云短信/腾讯云短信）
        log.info("[真实短信] phone={}, content={}", phone, content);
        return true;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private Object toVO(NotificationLog logEntity) {
        return java.util.Map.of(
                "id", logEntity.getId(),
                "type", logEntity.getType(),
                "typeText", getTypeText(logEntity.getType()),
                "content", logEntity.getContent(),
                "sentAt", logEntity.getSentAt() != null ? logEntity.getSentAt().toString() : "",
                "status", logEntity.getStatus(),
                "statusText", logEntity.getStatus() == 1 ? "已发送" :
                        logEntity.getStatus() == 2 ? "发送失败" : "待发送"
        );
    }

    private String getTypeText(int type) {
        return switch (type) {
            case 1 -> "预约成功通知";
            case 2 -> "签到提醒";
            case 3 -> "爽约通知";
            case 4 -> "超时提醒";
            case 5 -> "惩罚通知";
            default -> "未知";
        };
    }
}
