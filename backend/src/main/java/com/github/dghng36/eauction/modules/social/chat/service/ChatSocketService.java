package com.github.dghng36.eauction.modules.social.chat.service;

import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.modules.social.chat.dto.internal.ChatSocketMessage;
import com.github.dghng36.eauction.modules.social.chat.dto.request.HeartbeatRequest;
import com.github.dghng36.eauction.modules.social.chat.dto.request.ReadMessageRequest;
import com.github.dghng36.eauction.modules.social.chat.dto.request.TypingRequest;
import com.github.dghng36.eauction.modules.social.message.service.MessageService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ChatSocketService {
    MessageService messageService;

    TypingService typingService;

    ChatSessionService chatSessionService;

    ChatAuthorizationService chatAuthorizationService;

    public void sendMessage(
        String userId,
        ChatSocketMessage chatSocketMessage
    ) {
        chatAuthorizationService.validateParticipant(
            userId, chatSocketMessage.getConversationId()
        );

        messageService.sendMessage(
            userId, chatSocketMessage
        );
    }

    public void readMessage(
        String userId,
        ReadMessageRequest readMessageRequest
    ) {
        chatAuthorizationService.validateParticipant(
            userId, readMessageRequest.getConversationId()
        );

        messageService.mark(
            userId, readMessageRequest
        );
    }

    public void typing(
        String userId,
        TypingRequest typingRequest
    ) {
        chatAuthorizationService.validateParticipant(
            userId, typingRequest.getConversationId()
        );

        typingService.typing(
            userId, typingRequest.getConversationId(), typingRequest.getTyping()
        );
    }

    public void heartbeat(
        String userId,
        HeartbeatRequest heartbeatRequest
    ) {
        chatSessionService.heartbeat(
            userId, heartbeatRequest.getSessionId()
        );
    }
}
