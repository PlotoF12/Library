package com.example.library.module.notification.service;

import com.example.library.common.response.PageResult;
import com.example.library.module.notification.event.ReservationEvent;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /** 异步处理预约事件 → 发送短信 */
    void handleReservationEvent(ReservationEvent event);

    /** 查询通知历史 */
    PageResult<?> listNotifications(Long studentId, Integer type, int page, int size);

    /** 补偿重发失败的短信 */
    void retryFailedNotifications();
}
