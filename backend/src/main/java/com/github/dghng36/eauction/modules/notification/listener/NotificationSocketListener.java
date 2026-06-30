package com.github.dghng36.eauction.modules.notification.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.github.dghng36.eauction.core.websocket.enums.SocketEventType;
import com.github.dghng36.eauction.core.websocket.publisher.SocketPublisher;
import com.github.dghng36.eauction.modules.notification.dto.response.NotificationSocketResponse;
import com.github.dghng36.eauction.modules.notification.event.NotificationCreatedEvent;
import com.github.dghng36.eauction.modules.notification.event.NotificationReadEvent;
import com.github.dghng36.eauction.modules.notification.service.InternalNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationSocketListener {
    InternalNotificationService internalNotificationService;

    SocketPublisher socketPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNotificationCreated(
        NotificationCreatedEvent event
    ) {
        try {
            NotificationSocketResponse notificationResp = internalNotificationService.getNotificationById(
                event.getNotificationId()
            );

            socketPublisher.publish(
                "/topic/notifications/" + event.getReceiverId(),
                SocketEventType.NOTIFICATION_CREATED,
                notificationResp
            );

            log.info("Pushed 'Created' notification to user [{}]", event.getReceiverId());
        } catch(Exception ex) {
            log.error("Error while processing created new notification: [{}]: ", ex.getMessage(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNotificationRead(
        NotificationReadEvent event
    ) {
        try {
            socketPublisher.publish(
                "/topic/notifications/" + event.getReceiverId() + "/read",
                SocketEventType.NOTIFICATION_READ,
                event.getNotificationId()
            );
            
            log.info("Pushed 'Read' signal for notification: [{}] to user: [{}]", event.getNotificationId(), event.getReceiverId());
        } catch (Exception ex) {
            log.error("Error while publishing read notification event for notification: [{}], error: [{}]: ", event.getNotificationId(), ex.getMessage(), ex);
        }
    }
}
