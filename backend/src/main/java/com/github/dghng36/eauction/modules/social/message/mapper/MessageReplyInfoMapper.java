package com.github.dghng36.eauction.modules.social.message.mapper;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageReplyInfo;
import com.github.dghng36.eauction.modules.social.message.model.Message;

@Component
public class MessageReplyInfoMapper {
    public MessageReplyInfo toMessageReplyInfo(Message message, UserInfo sender) {
        if (message == null || sender == null) {
            return null;
        }

        return MessageReplyInfo.builder()
            .messageId(message.getId())
            .senderId(sender.getId())
            .senderName(sender.getUsername())
            .content(message.getContent())
            .build();
    }
}
