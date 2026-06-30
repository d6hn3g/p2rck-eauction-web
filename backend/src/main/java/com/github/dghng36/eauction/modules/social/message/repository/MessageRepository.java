package com.github.dghng36.eauction.modules.social.message.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.social.message.model.Message;

public interface MessageRepository extends MongoRepository<Message, String>{
    Page<Message> findAllByConversationIdAndIsDeletedFalse(String conversationId, Pageable pageable);

    Optional<Message> findByIdAndIsDeletedFalse(String messageId);

    List<Message> findByIdInAndIsDeletedFalse(Set<String> messageIds);
}
