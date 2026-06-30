package com.github.dghng36.eauction.modules.social.chat.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.social.chat.model.ChatSession;
import com.github.dghng36.eauction.modules.social.chat.repository.ChatSessionRepository;
import com.github.dghng36.eauction.modules.social.presence.service.InternalPresenceService;
import com.mongodb.client.result.DeleteResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChatSessionService {
    MongoTemplate mongoTemplate;

    ChatSessionRepository chatSessionRepo;

    InternalPresenceService internalPresenceService;

    @Transactional(propagation = Propagation.SUPPORTS)
    public void heartbeat(
        String userId,
        String sessionId
    ) {
        Query query = new Query().addCriteria(
            Criteria.where("sessionId").is(sessionId).and("userId").is(userId)
        );
        
        Update update = new Update().set("lastHeartbeatAt", Instant.now());

        long matchedCount = mongoTemplate.updateFirst(query, update, ChatSession.class).getMatchedCount();
        if (matchedCount == 0) {
            throw new AppException("Chat session not found or unauthorized", HttpStatus.NOT_FOUND);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void connect(
        String userId,
        String sessionId
    ) {
        ChatSession chatSession = ChatSession.builder()
            .userId(userId)
            .sessionId(sessionId)
            .connectedAt(Instant.now())
            .lastHeartbeatAt(Instant.now())
            .build();

        mongoTemplate.insert(chatSession);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void disconnect(
        String userId,
        String sessionId
    ) {
        Query query = new Query().addCriteria(
            Criteria.where("sessionId").is(sessionId).and("userId").is(userId)
        );

        DeleteResult result = mongoTemplate.remove(query, ChatSession.class);
        if (result.getDeletedCount() == 0) {
            throw new AppException("Chat session not found or unauthorized", HttpStatus.NOT_FOUND);
        }
    }

    public boolean isOnline(String userId) {
        Instant threshold = Instant.now().minusSeconds(90); // Consider online if heartbeat within last 90 seconds
        return chatSessionRepo.existsByUserIdAndLastHeartbeatAtAfter(userId, threshold);
    }

    public ChatSession getChatSession(String sessionId) {
        return chatSessionRepo.findBySessionId(sessionId)
            .orElseThrow(() -> new AppException("Chat session not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void cleanUpExpiredChatSessions() {
        Instant expirationThreshold = Instant.now().minusSeconds(90);

        Query findExpiredQuery = new Query().addCriteria(Criteria.where("lastHeartbeatAt")
            .lt(expirationThreshold));
        findExpiredQuery.fields().include("userId");
        
        List<ChatSession> expiredSessions = mongoTemplate.find(findExpiredQuery, ChatSession.class);
        if (expiredSessions.isEmpty()) {
            return;
        }

        List<String> potentialOfflineUserIds = expiredSessions.stream()
            .map(chatSession -> chatSession.getUserId())
            .distinct()
            .toList();

        Query deleteQuery = new Query().addCriteria(Criteria.where("lastHeartbeatAt")
            .lt(expirationThreshold));
        mongoTemplate.remove(deleteQuery, ChatSession.class);

        for (String userId : potentialOfflineUserIds) {
            if (!isOnline(userId)) {
                internalPresenceService.markUserOffline(userId);
            }
        }
        
        log.info("Cleaned up expired chat sessions. Checked [{}] potential offline users.", potentialOfflineUserIds.size());
    }
}
