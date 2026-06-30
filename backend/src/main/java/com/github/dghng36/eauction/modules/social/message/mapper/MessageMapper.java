package com.github.dghng36.eauction.modules.social.message.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.social.enums.MessageType;
import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageAttachment;
import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageReplyInfo;
import com.github.dghng36.eauction.modules.social.message.dto.response.MessageResponse;
import com.github.dghng36.eauction.modules.social.message.model.Message;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class MessageMapper {

    MessageReactionMapper messageReactionMapper;
    MessageReplyInfoMapper messageReplyInfoMapper;

    public Message toMessageEntity(
        String userId, String conversationId,
        String content, String type,
        List<MessageAttachment> attachments, String replyToMessageId
    ) {
        if (attachments == null || attachments.isEmpty()) {
            attachments = new ArrayList<>();
        }

        MessageType messageType = MessageType.fromString(type)
            .orElseThrow(() -> new AppException("Invalid message type", HttpStatus.BAD_REQUEST));

        return Message.builder()
            .senderId(userId)
            .conversationId(conversationId)
            .content(content)
            .type(messageType)
            .attachments(attachments)
            .seenBy(List.of(userId))
            .deletedForUsers(null)
            .deletedForEveryone(false)
            .editedAt(null)
            .replyToMessageId(replyToMessageId)
            .reactions(new ArrayList<>())
            .build();
    }

    public MessageResponse toMessageResponse(Message message, UserInfo sender, MessageReplyInfo replyInfo) {
        if (message == null) {
            return null;
        }

        return MessageResponse.builder()
            .id(message.getId())
            .conversationId(message.getConversationId())
            .sender(sender)
            .content(message.getContent())
            .messageType(message.getType().name())
            .attachments(message.getAttachments())
            .reactions(messageReactionMapper.toMessageReactionResponseList(sender.getId(), message.getReactions()))
            .replyTo(replyInfo)
            .deletedForEveryone(message.getDeletedForEveryone())
            .deletedForEveryoneAt(message.getDeletedForEveryoneAt())
            .editedAt(message.getEditedAt())
            .createdAt(message.getCreatedAt())
            .build();
    }

    public MessageReplyInfo toMessageReplyInfo(Message message, UserInfo sender) {
        return messageReplyInfoMapper.toMessageReplyInfo(message, sender);
    }
}
