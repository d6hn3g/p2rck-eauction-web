package com.github.dghng36.eauction.modules.social.message.dto.request;

import java.util.List;

import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageAttachment;

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
public class SendMessageRequest {
    String conversationId;

    String content;

    String messageType;

    List<MessageAttachment> attachments;
    
    String replyToMessageId;
}
