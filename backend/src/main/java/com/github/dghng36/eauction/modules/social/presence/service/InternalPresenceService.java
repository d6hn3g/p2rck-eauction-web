package com.github.dghng36.eauction.modules.social.presence.service;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.modules.social.enums.PresenceStatus;
import com.github.dghng36.eauction.modules.social.presence.model.Presence;
import com.github.dghng36.eauction.modules.social.presence.repository.PresenceRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class InternalPresenceService {
    MongoTemplate mongoTemplate;
    
    PresenceRepository presenceRepo;

    public Map<String, PresenceStatus> getUserPresencesByUserIds(Iterable<String> userIds) {
        return presenceRepo.findAllByUserIdInAndIsDeletedFalse(userIds)
            .stream()
            .collect(Collectors.toMap(
                presence -> presence.getUserId(),
                presence -> presence.getStatus()
            ));
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void markUserOnline(
        String userId
    ) {
        executePresenceUpsert(userId, PresenceStatus.ONLINE);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void markUserOffline(
        String userId
    ) {
        executePresenceUpsert(userId, PresenceStatus.OFFLINE);
    }

    private void executePresenceUpsert(String userId, PresenceStatus status) {
        Query query = new Query().addCriteria(
            Criteria.where("userId").is(userId).and("isDeleted").is(false)
        );

        Update update = new Update()
            .set("userId", userId)
            .set("status", status)
            .set("updatedAt", Instant.now())
            .setOnInsert("isDeleted", false)
            .setOnInsert("createdAt", Instant.now());

        mongoTemplate.upsert(query, update, Presence.class);
    }
}
