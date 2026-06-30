package com.github.dghng36.eauction.modules.social.conversation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.social.conversation.model.ConversationParticipant;

public interface ConversationParticipantRepository extends MongoRepository<ConversationParticipant, String> {
    Optional<ConversationParticipant> findByUserIdAndConversationIdAndIsDeletedFalseAndLeftAtNull(String userId, String conversationId);
    
    Page<ConversationParticipant> findAllByUserIdAndIsDeletedFalseAndLeftAtNull(String userId, Pageable pageable);
    
    List<ConversationParticipant> findAllByConversationIdAndIsDeletedFalse(String conversationId);
    
    List<ConversationParticipant> findAllByUserIdAndConversationIdInAndIsDeletedFalseAndLeftAtNull(String userId, List<String> conversationIds, Sort sort);
}
