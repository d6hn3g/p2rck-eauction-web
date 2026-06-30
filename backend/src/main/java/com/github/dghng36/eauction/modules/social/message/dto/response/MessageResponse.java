package com.github.dghng36.eauction.modules.social.message.dto.response;

import java.time.Instant;
import java.util.List;

import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageAttachment;
import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageReplyInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class MessageResponse {
    String id;

    String conversationId;

    UserInfo sender;

    String content;

    String messageType;

    List<MessageAttachment> attachments;

    MessageReplyInfo replyTo;

    List<MessageReactionResponse> reactions;

    Boolean deletedForEveryone;
    Instant deletedForEveryoneAt;

    Instant editedAt;
    Instant createdAt;
}
