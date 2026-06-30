package com.github.dghng36.eauction.modules.social.chat.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.social.chat.model.ChatSession;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    Optional<ChatSession> findBySessionId(String sessionId);
    boolean existsByUserIdAndLastHeartbeatAtAfter(String userId, Instant threshold);

}
