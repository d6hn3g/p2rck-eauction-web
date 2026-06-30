package com.github.dghng36.eauction.modules.social.conversation.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.social.enums.ConversationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "conversations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Conversation extends BaseEntity {
    String title; // NULL for direct conversations, non-null for group conversations
    
    ConversationType type;

    List<String> participantIds; // List of user ids participating in the conversation

    // Used for auction room conversations to link to the auction
    @Indexed
    String auctionRoomId; // Link to auction room id

    String lastMessageId;
    String lastMessage;
    String lastSenderId;
    Instant lastMessageTime;

    Boolean active; // Whether the conversation is active or archived for auction rooms
}
