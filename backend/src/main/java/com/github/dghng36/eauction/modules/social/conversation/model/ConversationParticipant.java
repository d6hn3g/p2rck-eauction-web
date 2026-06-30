package com.github.dghng36.eauction.modules.social.conversation.model;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "conversation_participants")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@CompoundIndex(name = "idx_user_left_at", def = "{'userId': 1, 'leftAt': 1}")
public class ConversationParticipant extends BaseEntity {
    @Indexed
    String conversationId;
    
    String userId;

    // Notification muted
    Instant mutedAt;

    // Hidden conversation
    Instant hiddenAt;

    // Pin conversation
    Instant pinnedAt;

    // Leave room
    Instant leftAt;

    // Unread count
    Integer unreadCount;

    // Last seen message id
    String lastReadMessageId;

    Instant joinedAt;
}
