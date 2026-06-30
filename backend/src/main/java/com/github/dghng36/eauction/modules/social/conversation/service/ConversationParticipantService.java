package com.github.dghng36.eauction.modules.social.conversation.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationParticipantResponse;
import com.github.dghng36.eauction.modules.social.conversation.mapper.ConversationParticipantMapper;
import com.github.dghng36.eauction.modules.social.conversation.model.ConversationParticipant;
import com.github.dghng36.eauction.modules.social.conversation.repository.ConversationParticipantRepository;
import com.github.dghng36.eauction.modules.social.enums.PresenceStatus;
import com.github.dghng36.eauction.modules.social.presence.service.InternalPresenceService;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ConversationParticipantService {
    MongoTemplate mongoTemplate;
    ConversationParticipantRepository conversationParticipantRepo;

    InternalPresenceService internalPresenceService;

    ConversationParticipantMapper conversationParticipantMapper;


    // CRUD operations
    @Transactional
    public void createParticipant(String userId, String conversationId) {
        ConversationParticipant conversationParticipant = conversationParticipantMapper.toConversationParticipantEntity(conversationId, userId);
        conversationParticipantRepo.save(conversationParticipant);
    }

    public ConversationParticipant getParticipant(String userId, String conversationId) {
        return conversationParticipantRepo.findByUserIdAndConversationIdAndIsDeletedFalseAndLeftAtNull(userId, conversationId)
            .orElseThrow(() -> new AppException("Conversation participant not found", HttpStatus.NOT_FOUND));
    }

    public List<ConversationParticipant> getParticipantsByConversationId(String conversationId) {
        return conversationParticipantRepo.findAllByConversationIdAndIsDeletedFalse(conversationId);
    }

    public Page<ConversationParticipant> getParticipantsByUserId(String userId, Pageable pageable) {
        return conversationParticipantRepo.findAllByUserIdAndIsDeletedFalseAndLeftAtNull(userId, pageable);
    }

    public List<ConversationParticipant> getParticipantsByUserIdAndConversationIds(String userId, List<String> conversationIds) {
        if (conversationIds.isEmpty()) {
            return List.of();
        }

        return conversationParticipantRepo.findAllByUserIdAndConversationIdInAndIsDeletedFalseAndLeftAtNull(
            userId, conversationIds, Sort.by(Sort.Direction.DESC, "joinedAt")
        );
    }

    public List<ConversationParticipant> getParticipantsByQuery(
        Query query
    ) {
        return mongoTemplate.find(query, ConversationParticipant.class);
    }

    public List<ConversationParticipantResponse> toParticipantResponseList(List<ConversationParticipant> participants, Map<String, UserInfo> userInfoMap) {
        if (participants.isEmpty()) {
            return List.of();
        }

        Map<String, PresenceStatus> presenceStatusMap = internalPresenceService.getUserPresencesByUserIds(
            participants.stream().map(participant -> participant.getUserId()).toList()
        );

        return conversationParticipantMapper.toConversationParticipantResponseList(participants, userInfoMap, presenceStatusMap);
    }

    public void leaveAllParticipants(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId)
            .and("isDeleted").is(false)
            .and("leftAt").is(null)
        );

        Update update = new Update()
            .set("leftAt", Instant.now())
            .set("unreadCount", 0);

        mongoTemplate.updateMulti(query, update, ConversationParticipant.class);
    }

    // Business logic
    public void hideParticipant(
        String userId,
        String conversationId
    ) {
        executeAtomicUpdate(
            userId, conversationId, 
            new Update()
                .set("hiddenAt", Instant.now())
        );
    }

    public void togglePinParticipant(
        String userId,
        String conversationId,
        Boolean pinned
    ) {
        executeAtomicUpdate(
            userId, conversationId, new Update()
                .set("pinnedAt", pinned ? Instant.now() : null)
        );
    }

    public void toggleMuteParticipant(
        String userId,
        String conversationId,
        Boolean muted
    ) {
        executeAtomicUpdate(
            userId, conversationId, new Update()
                .set("mutedAt", muted ? Instant.now() : null)
        );
    }

    public void markParticipantAsRead(
        String userId,
        String conversationId,
        String lastMessageId
    ) {
        Update update = new Update()
            .set("unreadCount", 0)
            .set("lastReadMessageId", lastMessageId);

        executeAtomicUpdate(userId, conversationId, update);
    }

    public void leaveParticipant(
        String userId,
        String conversationId
    ) {
        executeAtomicUpdate(
            userId, conversationId, new Update()
                .set("leftAt", Instant.now())
                .set("unreadCount", 0)
        );
    }

    public void incrementUnreadCount(
        String userId,
        String conversationId
    ) {
        executeAtomicUpdate(
            userId, conversationId, 
            new Update().inc("unreadCount", 1)
        );
    }

    public void incrementUnreadCountForAllExceptSender(
        String senderId, String conversationId
    ) {
        Query query = new Query(Criteria.where("userId").ne(senderId)
            .and("conversationId").is(conversationId)
            .and("isDeleted").is(false)
            .and("leftAt").is(null)
        );

        Update update = new Update().inc("unreadCount", 1);

        mongoTemplate.updateMulti(query, update, ConversationParticipant.class);
    }

    // Utility methods
    private void executeAtomicUpdate(
        String userId, String conversationId,
        Update update 
    ) {
        Query query = new Query(Criteria.where("userId").is(userId)
            .and("conversationId").is(conversationId)
            .and("isDeleted").is(false)
        );

        UpdateResult result = mongoTemplate.updateFirst(query, update, ConversationParticipant.class);
        if (result.getMatchedCount() == 0) {
            throw new AppException("Conversation participant not found", HttpStatus.NOT_FOUND);
        }
    }
}
