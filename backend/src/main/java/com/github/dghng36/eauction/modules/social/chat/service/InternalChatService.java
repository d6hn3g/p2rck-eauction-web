package com.github.dghng36.eauction.modules.social.chat.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class InternalChatService {
    ChatSessionService chatSessionService;

    public boolean isUserOnline(String userId) {
        return chatSessionService.isOnline(userId);
    }
}
