package com.github.dghng36.eauction.modules.social.message.dto.internal;

import java.time.Instant;

import com.github.dghng36.eauction.modules.social.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class MessageInfo {
    String id;

    String conversationId;

    String senderId;

    String content;

    MessageType messageType;

    Instant createdAt;
}
