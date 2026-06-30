package com.github.dghng36.eauction.modules.social.conversation.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ConversationResponse {
    String id;

    String title;

    String type;

    String lastMessage;
    Instant lastMessageAt;

    Integer unreadCount;

    Boolean pinned;

    Boolean muted;

    Boolean hidden;
}
