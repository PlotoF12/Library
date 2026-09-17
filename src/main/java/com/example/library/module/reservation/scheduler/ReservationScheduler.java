package com.example.library.module.reservation.scheduler;

import com.example.library.module.notification.service.NotificationService;
import com.example.library.module.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预约定时任务 — 爽约扫描 + 超时释放 + 短信重发。
 *
 * <p>三层定时策略：
 * <ol>
 *   <li><b>Redisson RDelayedQueue</b>：预约创建时投递，15分钟后精确触发（主力方案）</li>
 *   <li><b>@Scheduled(fixedRate=1s)</b>：每秒扫描兜底（防止延迟队列消息丢失）</li>
 *   <li><b>@Scheduled(fixedRate=60s)</b>：每分钟扫描超时未离座的座位</li>
 * </ol>
 *
 * <p>应用启动后初始化延迟队列消费者线程，阻塞等待延迟消息到达。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final RDelayedQueue<Long> noShowDelayedQueue;
    private final RBlockingQueue<Long> noShowBlockingQueue;

    /**
     * 延迟队列消费者 — 应用启动后自动开始监听。
     * Redisson RDelayedQueue 的消息在延迟时间到达后会自动转移到关联的 RBlockingQueue，
     * 消费者调用 take() 阻塞获取。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startNoShowConsumer() {
        Thread consumer = new Thread(() -> {
            log.info("爽约延迟队列消费者启动");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Long reservationId = noShowBlockingQueue.take();
                    log.debug("延迟队列触发爽约检查: reservationId={}", reservationId);
                    reservationService.checkNoShowReservations();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("爽约延迟队列消费者停止");
                    break;
                } catch (Exception e) {
                    log.error("爽约延迟队列消费异常", e);
                }
            }
        }, "no-show-consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    /**
     * 爽约扫描兜底 — 每秒执行（与延迟队列双保险）。
     * 即使延迟队列消息丢失，最坏情况延迟1秒扫描到超时预约。
     */
    @Scheduled(fixedRateString = "${app.scheduler.no-show-check-rate:1000}")
    public void checkNoShow() {
        try {
            reservationService.checkNoShowReservations();
        } catch (Exception e) {
            log.error("爽约扫描异常", e);
        }
    }

    /**
     * 超时释放扫描 — 每分钟执行。
     * 释放已超过预约结束时间的座位。
     */
    @Scheduled(fixedRateString = "${app.scheduler.timeout-check-rate:60000}")
    public void checkTimeout() {
        try {
            reservationService.checkTimeoutReservations();
        } catch (Exception e) {
            log.error("超时释放扫描异常", e);
        }
    }

    /**
     * 短信重发补偿 — 每5分钟执行。
     * 查询 notification_log 中 status=0（待发送）或 status=2（失败）的记录重试。
     */
    @Scheduled(fixedRate = 300000)
    public void retryFailedNotifications() {
        try {
            notificationService.retryFailedNotifications();
        } catch (Exception e) {
            log.error("短信重发异常", e);
        }
    }
}
