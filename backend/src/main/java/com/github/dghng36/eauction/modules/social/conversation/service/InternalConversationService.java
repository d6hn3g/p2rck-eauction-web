package com.github.dghng36.eauction.modules.social.conversation.service;

import java.time.Instant;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.social.conversation.mapper.ConversationMapper;
import com.github.dghng36.eauction.modules.social.conversation.model.Conversation;
import com.github.dghng36.eauction.modules.social.conversation.model.ConversationParticipant;
import com.github.dghng36.eauction.modules.social.conversation.repository.ConversationRepository;
import com.github.dghng36.eauction.modules.social.enums.ConversationType;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InternalConversationService {
    MongoTemplate mongoTemplate;
    ConversationRepository conversationRepo;

    ConversationParticipantService conversationParticipantService;

    ConversationMapper conversationMapper;

    @Transactional(propagation = Propagation.SUPPORTS)
    public String createAuctionRoomConversation(
        String auctionRoomId,
        String title,
        String creatorUserId
    ) {
        if (conversationRepo.existsByAuctionRoomIdAndIsDeletedFalse(auctionRoomId)) {
            log.error("Attempted to create a conversation for auction room {} but it already exists", auctionRoomId);

            throw new AppException("Conversation for this auction room already exists", HttpStatus.CONFLICT);
        }

        Conversation conversation = conversationMapper.toAuctionRoomConversationEntity(auctionRoomId, title, creatorUserId);
        
        conversation = conversationRepo.save(conversation);
        log.info("Auction room conversation created for auction room {} with conversationId {}", auctionRoomId, conversation.getId());
        
        return conversation.getId();
    }
    
    public void archiveAuctionRoomConversation(String conversationId) {
        Query query = new Query(
            Criteria.where("_id").is(conversationId)
                .and("isDeleted").is(false)        
        );

        Update update = new Update()
            .set("active", false);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (result.getMatchedCount() == 0) {
            throw new AppException("Conversation not found", HttpStatus.NOT_FOUND);
        }   
    }

    public void unarchiveAuctionRoomConversation(String conversationId) {
        Query query = new Query(
            Criteria.where("_id").is(conversationId)
                .and("isDeleted").is(false)        
        );

        Update update = new Update()
            .set("active", true);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (result.getMatchedCount() == 0) {
            throw new AppException("Conversation not found", HttpStatus.NOT_FOUND);
        }
    }

    public void validateParticipant(String userId, String conversationId) {
        // Check conversation participant
        ConversationParticipant participant = conversationParticipantService.getParticipant(userId, conversationId);
        if (participant == null || participant.getLeftAt() != null) {
            throw new AppException("User is not a participant of this conversation", HttpStatus.FORBIDDEN);
        }
    }

    public void validateActive(String conversationId) {
        Query query = new Query().addCriteria(
            Criteria.where("_id").is(conversationId).and("isDeleted").is(false)
        );

        query.fields().include("type", "active"); 

        Conversation conversation = mongoTemplate.findOne(query, Conversation.class);
        if (conversation == null) {
            throw new AppException("Conversation not found", HttpStatus.NOT_FOUND);
        }

        if (ConversationType.AUCTION_ROOM.equals(conversation.getType()) 
                && conversation.getActive() != null 
                && !conversation.getActive()) {
            throw new AppException("Conversation is archived", HttpStatus.GONE);
        }
    }

    public void updateLastMessage(
        String userId,
        String conversationId,
        String messageId,
        String messageContent,
        Instant messageCreatedAt
    ) {
        if (!StringUtils.hasText(messageContent) || messageCreatedAt == null) {
            return;
        }

        Query query = new Query(
            Criteria.where("_id").is(conversationId)
                .and("isDeleted").is(false)        
        );
        Update update = new Update()
            .set("lastMessageId", messageId)
            .set("lastSenderId", userId)
            .set("lastMessage", messageContent)
            .set("lastMessageTime", messageCreatedAt);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (result.getMatchedCount() == 0) {
            throw new AppException("Conversation not found", HttpStatus.NOT_FOUND);
        }

        conversationParticipantService.markParticipantAsRead(userId, conversationId, messageId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void incrementUnreadCount(
        String senderId,
        String conversationId
    ) {
        conversationParticipantService.incrementUnreadCountForAllExceptSender(senderId, conversationId);
    }
}
