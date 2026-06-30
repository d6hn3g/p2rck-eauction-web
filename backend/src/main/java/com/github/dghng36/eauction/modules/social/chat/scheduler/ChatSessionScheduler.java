package com.github.dghng36.eauction.modules.social.chat.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.social.chat.service.ChatSessionService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ChatSessionScheduler {
    ChatSessionService chatSessionService;

    @Scheduled(fixedDelay = 60000)
    public void cleanUpExpiredChatSessions() {
        chatSessionService.cleanUpExpiredChatSessions();
    }
}
