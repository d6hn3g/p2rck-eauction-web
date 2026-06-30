package com.github.dghng36.eauction.modules.social.chat.controller.v1;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.social.chat.dto.internal.ChatSocketMessage;
import com.github.dghng36.eauction.modules.social.chat.dto.request.HeartbeatRequest;
import com.github.dghng36.eauction.modules.social.chat.dto.request.ReadMessageRequest;
import com.github.dghng36.eauction.modules.social.chat.dto.request.TypingRequest;
import com.github.dghng36.eauction.modules.social.chat.service.ChatSocketService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ChatSocketController {
    ChatSocketService chatSocketService;

    @MessageMapping("/chat.send")
    public void sendMessage(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        ChatSocketMessage chatSocketMessage
    ) {
        chatSocketService.sendMessage(
            userId, chatSocketMessage
        );
    }

    @MessageMapping("/chat.read")
    public void readMessage(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        ReadMessageRequest readMessageRequest
    ) {
        chatSocketService.readMessage(
            userId, readMessageRequest
        );
    }

    @MessageMapping("/chat.typing")
    public void typing(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        TypingRequest typingRequest
    ) {
        chatSocketService.typing(
            userId, typingRequest
        );
    }

    @MessageMapping("/chat.heartbeat")
    public void heartbeat(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        HeartbeatRequest heartbeatRequest
    ) {
        chatSocketService.heartbeat(
            userId, heartbeatRequest
        );
    }
}
