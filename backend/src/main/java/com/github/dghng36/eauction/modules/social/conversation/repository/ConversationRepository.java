package com.github.dghng36.eauction.modules.social.conversation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.github.dghng36.eauction.modules.social.conversation.model.Conversation;


public interface ConversationRepository extends MongoRepository<Conversation, String> {
    @Query(
        "{ 'type': 'DIRECT', 'participantIds': { $all: [?0, ?1] }, 'isDeleted': false }"
    )
    Optional<Conversation> findDirectConversationBetweenTwoUsersAndIsDeletedFalse(String userId1, String userId2);

    @Query("{ 'participantIds': ?0, 'isDeleted': false }")
    Page<Conversation> findActiveConversationsByUserId(String userId, Pageable pageable);

    List<Conversation> findByIdInAndIsDeletedFalse(List<String> conversationIds);

    Optional<Conversation> findByIdAndIsDeletedFalse(String conversationId);

    @Query("{ 'isDeleted': false, 'type': 'DIRECT', 'participantIds': { $all: [ ?0 ], $in: ?1 } }")
    List<Conversation> findAllDirectConversationsByParticipants(String userId, List<String> participantIds);
    
    boolean existsByAuctionRoomIdAndIsDeletedFalse(String auctionRoomId);
}
