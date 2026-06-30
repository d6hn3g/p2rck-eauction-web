package com.github.dghng36.eauction.modules.social.presence.service;

import java.time.Instant;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.social.enums.PresenceStatus;
import com.github.dghng36.eauction.modules.social.presence.dto.request.UpdatePresenceRequest;
import com.github.dghng36.eauction.modules.social.presence.dto.response.PresenceResponse;
import com.github.dghng36.eauction.modules.social.presence.mapper.PresenceMapper;
import com.github.dghng36.eauction.modules.social.presence.model.Presence;
import com.github.dghng36.eauction.modules.social.presence.repository.PresenceRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PresenceService {
    MongoTemplate mongoTemplate;
    
    PresenceRepository presenceRepo;

    PresenceMapper presenceMapper;

    public PresenceResponse getUserPresence(String userId) {
        Presence presence = presenceRepo.findByUserIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User presence not found for userId: " + userId, HttpStatus.NOT_FOUND));

        return presenceMapper.toPresenceResponse(presence);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public PresenceResponse updateUserPresence(String userId, UpdatePresenceRequest updatePresenceRequest) {
        PresenceStatus newStatus = PresenceStatus.fromString(updatePresenceRequest.getNewStatus())
            .orElseThrow(() -> new AppException("Invalid presence status: " + updatePresenceRequest.getNewStatus(), HttpStatus.BAD_REQUEST));

        Query query = new Query().addCriteria(
            Criteria.where("userId").is(userId).and("isDeleted").is(false)
        );

        Update update = new Update()
            .set("userId", userId)
            .set("status", newStatus)
            .set("updatedAt", Instant.now())
            .setOnInsert("isDeleted", false)
            .setOnInsert("createdAt", Instant.now());

        Presence updatedPresence = mongoTemplate.findAndModify(
            query, 
            update, 
            FindAndModifyOptions.options().upsert(true).returnNew(true), 
            Presence.class
        );

        if (updatedPresence == null) {
            throw new AppException("Failed to update user presence due to concurrent modification", HttpStatus.CONFLICT);
        }

        log.info("Updated presence successfully via Atomic Upsert for userId: {} to status: {}", userId, newStatus);

        return presenceMapper.toPresenceResponse(updatedPresence);
    }
}
