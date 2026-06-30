package com.github.dghng36.eauction.modules.social.conversation.mapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationParticipantResponse;
import com.github.dghng36.eauction.modules.social.conversation.model.ConversationParticipant;
import com.github.dghng36.eauction.modules.social.enums.PresenceStatus;

@Component
public class ConversationParticipantMapper {
    public ConversationParticipant toConversationParticipantEntity(String conversationId, String userId) {
        return ConversationParticipant.builder()
            .conversationId(conversationId)
            .userId(userId)
            .unreadCount(0)
            .lastReadMessageId(null)
            .joinedAt(Instant.now())
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }

    private ConversationParticipantResponse toConversationParticipantResponse( 
        UserInfo userInfo, PresenceStatus presenceStatus
    ) {
        return ConversationParticipantResponse.builder()
            .userInfo(userInfo)
            .presenceStatus(presenceStatus)
            .build();
    }

    public List<ConversationParticipantResponse> toConversationParticipantResponseList(
        List<ConversationParticipant> participants, 
        Map<String, UserInfo> userInfoMap, 
        Map<String, PresenceStatus> presenceStatusMap
    ) {
        if (participants == null) {
            return List.of();
        }

        return participants.stream()
            .map(participant -> toConversationParticipantResponse(
                userInfoMap.get(participant.getUserId()), 
                presenceStatusMap.getOrDefault(participant.getUserId(), PresenceStatus.OFFLINE)
            ))
            .toList();
    }
}