package com.github.dghng36.eauction.modules.social.chat.dto.internal;

import java.util.List;

import com.github.dghng36.eauction.modules.social.enums.MessageType;
import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageAttachment;

import jakarta.validation.constraints.NotBlank;
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
public class ChatSocketMessage {
    @NotBlank(message = "Conversation ID is required")
    String conversationId;

    String content;

    MessageType type;

    List<MessageAttachment> attachments;

    String replyToMessageId;
}
