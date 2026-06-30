package com.github.dghng36.eauction.modules.social.conversation.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationDetailResponse;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationParticipantResponse;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationResponse;
import com.github.dghng36.eauction.modules.social.conversation.model.Conversation;
import com.github.dghng36.eauction.modules.social.enums.ConversationType;

@Component
public class ConversationMapper {
    public Conversation toDirectConversationEntity(String userId, String recipientUserId) {
        if (userId == null || recipientUserId == null) {
            return null;
        }

        return Conversation.builder()
            .type(ConversationType.DIRECT)
            .participantIds(List.of(userId, recipientUserId))
            .active(true)
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }

    public Conversation toAuctionRoomConversationEntity(String auctionRoomId, String title, String creatorUserId) {
        if (auctionRoomId == null || title == null || creatorUserId == null) {
            return null;
        }

        return Conversation.builder()
            .title(title)
            .type(ConversationType.AUCTION_ROOM)
            .auctionRoomId(auctionRoomId)
            .participantIds(List.of(creatorUserId))
            .active(true)
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }

    public ConversationResponse toConversationResponse(Conversation conversation, Integer unreadCount, Boolean pinned, Boolean muted, Boolean hidden) {
        if (conversation == null) {
            return null;
        }

        return ConversationResponse.builder()
            .id(conversation.getId())
            .title(conversation.getTitle())
            .type(conversation.getType().name())
            .lastMessage(conversation.getLastMessage())
            .lastMessageAt(conversation.getLastMessageTime())
            .unreadCount(unreadCount)
            .pinned(pinned)
            .muted(muted)
            .hidden(hidden)
            .build();
    }

    public ConversationResponse toConversationResponse(Conversation conversation) {
        if (conversation == null) {
            return null;
        }

        return toConversationResponse(conversation, 0, false, false, false);
    }

    public ConversationDetailResponse toConversationDetailResponse(
        Conversation conversation,
        List<ConversationParticipantResponse> participantResponses,
        Integer unreadCount,
        Boolean pinned,
        Boolean muted,
        Boolean hidden
    ) {
        if (conversation == null) {
            return null;
        }

        return ConversationDetailResponse.builder()
            .id(conversation.getId())
            .title(conversation.getTitle())
            .type(conversation.getType().name())
            .lastMessage(conversation.getLastMessage())
            .lastMessageAt(conversation.getLastMessageTime())
            .unreadCount(unreadCount)
            .pinned(pinned)
            .muted(muted)
            .hidden(hidden)
            .participants(participantResponses)
            .build();
    }
    
}
