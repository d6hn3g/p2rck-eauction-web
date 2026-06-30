package com.github.dghng36.eauction.modules.social.message.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.social.enums.MessageType;
import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageAttachment;
import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageReaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "messages")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Message extends BaseEntity {
    String conversationId;

    String senderId;
    
    String content;

    MessageType type;

    // Attachments
    List<MessageAttachment> attachments;

    // Seen user
    List<String> seenBy;

    // Deleted for specific users
    List<String> deletedForUsers;

    // Global un-send
    Boolean deletedForEveryone;
    Instant deletedForEveryoneAt;

    // Edit message
    Instant editedAt;

    // Reply support
    String replyToMessageId;

    // Reaction support
    List<MessageReaction> reactions;
}
