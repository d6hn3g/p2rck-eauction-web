package com.github.dghng36.eauction.modules.social.chat.service;

import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.core.websocket.enums.SocketEventType;
import com.github.dghng36.eauction.core.websocket.publisher.SocketPublisher;
import com.github.dghng36.eauction.modules.social.chat.dto.response.TypingResponse;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TypingService {
    SocketPublisher socketPublisher;

    public void typing(String userId, String conversationId, Boolean typing) {

        try {
            socketPublisher.publish(
                "/topic/chat/" + conversationId + "/typing",
                SocketEventType.CHAT_MESSAGE_TYPING,
                TypingResponse.builder()
                    .conversationId(conversationId)
                    .userId(userId)
                    .typing(typing)
                    .build()
            );  
        } catch(Exception ex) {
            log.error("Error exists during identify typing: [{}]: ", ex.getMessage(), ex);
        }
    }
}
