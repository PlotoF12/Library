package com.example.library.module.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 预约事件 — 用于异步短信通知
 */
@Getter
public class ReservationEvent extends ApplicationEvent {

    private final Long reservationId;
    private final EventType eventType;

    public ReservationEvent(Object source, Long reservationId, EventType eventType) {
        super(source);
        this.reservationId = reservationId;
        this.eventType = eventType;
    }

    public enum EventType {
        RESERVATION_CREATED,   // 预约成功
        SIGN_IN_REMIND,        // 签到提醒
        NO_SHOW,               // 爽约通知
        TIMEOUT,               // 超时提醒
        PENALTY_APPLIED        // 惩罚通知
    }
}
