package com.github.dghng36.eauction.modules.social.chat.service;

import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.modules.social.conversation.service.InternalConversationService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ChatAuthorizationService {
    InternalConversationService internalConversationService;

    public void validateParticipant(String userId, String conversationId) {
        internalConversationService.validateParticipant(userId, conversationId);
    }
}
