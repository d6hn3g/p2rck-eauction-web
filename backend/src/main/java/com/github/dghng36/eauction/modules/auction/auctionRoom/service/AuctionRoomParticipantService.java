package com.github.dghng36.eauction.modules.auction.auctionRoom.service;

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
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoomParticipant;
import com.github.dghng36.eauction.modules.auction.auctionRoom.repository.AuctionRoomParticipantRepository;
import com.github.dghng36.eauction.modules.auction.enums.ParticipantStatus;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuctionRoomParticipantService {
    MongoTemplate mongoTemplate;
    AuctionRoomParticipantRepository auctionRoomParticipantRepo;

    @Transactional
    public void createParticipant(
        String auctionRoomId,
        String userId,
        String joinReason
    ) {
        AuctionRoomParticipant auctionRoomParticipant = AuctionRoomParticipant.builder()
            .auctionRoomId(auctionRoomId)
            .userId(userId)
            .status(ParticipantStatus.PENDING)
            .joinReason(joinReason)
            .build();

        auctionRoomParticipantRepo.save(auctionRoomParticipant);
    }

    public boolean existsParticipant(String userId, String auctionRoomId) {
        return auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserId(auctionRoomId, userId);
    }

    public boolean isParticipantApproved(String userId, String auctionRoomId) {
        return auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserIdAndStatus(auctionRoomId, userId, ParticipantStatus.APPROVED);
    }

    public boolean isParticipantInsideAuctionRoom(String userId, String auctionRoomId) {
        return auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserIdAndStatusIn(
            auctionRoomId, userId, List.of(ParticipantStatus.APPROVED, ParticipantStatus.PENDING)
        );
    }

    public boolean isParticipantLeftAuctionRoom(String userId, String auctionRoomId) {
        return auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserIdAndStatus(auctionRoomId, userId, ParticipantStatus.LEFT);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void requestToJoin(String userId, String auctionRoomId, String joinReason) {
        Query query = new Query().addCriteria(
            Criteria.where("auctionRoomId").is(auctionRoomId).and("userId").is(userId)
        );

        Update update = new Update()
            .set("status", ParticipantStatus.PENDING)
            .set("joinedAt", Instant.now())
            .set("joinReason", joinReason);

        UpdateResult result = mongoTemplate.updateFirst(query, update, AuctionRoomParticipant.class);
        if (result.getMatchedCount() == 0) {
            throw new AppException("Participant not found", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean processParticipantStatus(String managerId, String auctionRoomId, String userId, String status) {
        ParticipantStatus newStatus = ParticipantStatus.fromString(status)
            .orElseThrow(() -> new AppException("Invalid participant status: " + status, HttpStatus.BAD_REQUEST));

        Query query = new Query().addCriteria(
            Criteria.where("auctionRoomId").is(auctionRoomId).and("userId").is(userId)
        );

        Update update = new Update().set("status", newStatus.name());

        if (newStatus.equals(ParticipantStatus.APPROVED)) {
            update.set("approvedAt", Instant.now());
            update.set("approvedBy", managerId);
        }

        UpdateResult result = mongoTemplate.updateFirst(query, update, AuctionRoomParticipant.class);
        if (result.getMatchedCount() == 0) {
            throw new AppException("Participant not found", HttpStatus.BAD_REQUEST);
        }

        return newStatus.equals(ParticipantStatus.APPROVED);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void leaveAuctionRoom(String auctionRoomId, String userId) {
        Query query = new Query().addCriteria(
            Criteria.where("auctionRoomId").is(auctionRoomId).and("userId").is(userId)
        );

        Update update = new Update().set("status", ParticipantStatus.LEFT.name());

        UpdateResult result = mongoTemplate.updateFirst(query, update, AuctionRoomParticipant.class);
        if (result.getMatchedCount() == 0) {
            throw new AppException("Participant not found", HttpStatus.BAD_REQUEST);
        }
    }

    public List<String> getActiveRoomIds(String userId) {
        return auctionRoomParticipantRepo.findAllByUserIdAndStatusIn(userId, List.of(ParticipantStatus.APPROVED, ParticipantStatus.PENDING))
            .stream()
            .map(participant -> participant.getAuctionRoomId())
            .toList();
    }

    public List<String> getActiveParticipantIds(String auctionRoomId) {
        return auctionRoomParticipantRepo.findAllByAuctionRoomIdAndStatus(auctionRoomId, ParticipantStatus.APPROVED)
            .stream()
            .map(participant -> participant.getUserId())
            .toList();
    }
}
