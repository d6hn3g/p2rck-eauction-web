package com.github.dghng36.eauction.core.websocket.publisher;

import java.time.Instant;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.websocket.enums.SocketEventType;
import com.github.dghng36.eauction.core.websocket.event.SocketEvent;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SocketPublisher {
    SimpMessagingTemplate messagingTemplate;

    public <T> void publish(
            String topic,
            SocketEventType eventType,
            T data
    ) {

        SocketEvent<T> event =
                SocketEvent.<T>builder()
                        .eventType(eventType)
                        .data(data)
                        .timestamp(Instant.now())
                        .build();

        messagingTemplate.convertAndSend(
                topic,
                event
        );
    }

    public <T> void publishException(
            String userId,
            T data
    ) {
        publish("/user/" + userId + "/queue/errors", SocketEventType.EXCEPTION, data);
    }
}
