package com.github.dghng36.eauction.modules.social.message.event;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class MessageSentEvent {
    String messageId;

    String conversationId;

    String senderId;

    String content;

    Instant createdAt;
}
