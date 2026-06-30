package com.github.dghng36.eauction.modules.identity.reputation.service;

import java.util.List;

import org.bson.Document;
import org.springframework.context.ApplicationEventPublisher;
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
import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.core.utils.ReputationUtils;
import com.github.dghng36.eauction.modules.identity.reputation.event.CreatedAuctionAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.LostAuctionPenaltyEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.ParticipatedAuctionAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.WeeklyAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.WelcomeAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.WonAuctionBonusEvent;
import com.github.dghng36.eauction.modules.identity.user.model.User;
import com.github.dghng36.eauction.modules.identity.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReputationProcessor {
    MongoTemplate mongoTemplate;
    UserRepository userRepo;

    ApplicationEventPublisher eventPublisher;

    public void awardWelcomeBonus(User user) {
        double bonus = ConstantsUtils.ReputationConstants.WELCOME_REPUTATION_BONUS;
        user.setReputation(ReputationUtils.clampReputation(user.getReputation() + bonus));

        executeAtomicDeltaUpdate(user.getId(), bonus);

        // Publish event for awarding welcome bonus
        eventPublisher.publishEvent(
            WelcomeAwardedBonusEvent.builder()
                .userId(user.getId())
                .awardedAmount(bonus)
                .build()
        );
    }

    public void awardWeeklyLoginBonus(String userId) {
        executeAtomicDeltaUpdate(userId, ConstantsUtils.ReputationConstants.LOGIN_WEEKLY_REPUTATION_BONUS);

        // Publish event for awarding weekly login bonus
        eventPublisher.publishEvent(
            WeeklyAwardedBonusEvent.builder()
                .userId(userId)
                .awardedAmount(ConstantsUtils.ReputationConstants.LOGIN_WEEKLY_REPUTATION_BONUS)
                .build()
        );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void incUserCreatedAuctionRoom(String userId, String auctionRoomId) {
       User user = fetchActiveUser(userId);
        if (!ReputationUtils.isCreatedAuctionRoom(user.getReputation())) {
            throw new AppException("User's reputation is too low to create an auction room", HttpStatus.BAD_REQUEST);
        }
        executeAtomicDeltaUpdate(userId, ConstantsUtils.ReputationConstants.CREATED_AUCTION_ROOM_REPUTATION_BONUS);

        // Publish event for creating auction room
        eventPublisher.publishEvent(
            CreatedAuctionAwardedBonusEvent.builder()
                .userId(userId)
                .auctionRoomId(auctionRoomId)
                .awardedAmount(ConstantsUtils.ReputationConstants.CREATED_AUCTION_ROOM_REPUTATION_BONUS)
                .build()
        );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void incUserParticipateAuctionRoom(String userId, String auctionRoomId) {
        User user = fetchActiveUser(userId);
        if (!ReputationUtils.isParticipatedAuctionRoom(user.getReputation())) {
            throw new AppException("User's reputation is too low to join the auction room", HttpStatus.BAD_REQUEST);
        }
        executeAtomicDeltaUpdate(userId, ConstantsUtils.ReputationConstants.PARTICIPATED_AUCTION_ROOM_REPUTATION_BONUS);

        // Publish event for joining auction room
        eventPublisher.publishEvent(
            ParticipatedAuctionAwardedBonusEvent.builder()
                .userId(userId)
                .auctionRoomId(auctionRoomId)
                .awardedAmount(ConstantsUtils.ReputationConstants.PARTICIPATED_AUCTION_ROOM_REPUTATION_BONUS)
                .build()
        );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void incUserWonAuction(String userId, String auctionRoomId) {
        executeAtomicDeltaUpdate(userId, ConstantsUtils.ReputationConstants.WIN_REPUTATION_BONUS);

        // Publish event for winning auction
        eventPublisher.publishEvent(
            WonAuctionBonusEvent.builder()
                .userId(userId)
                .auctionRoomId(auctionRoomId)
                .awardedAmount(ConstantsUtils.ReputationConstants.WIN_REPUTATION_BONUS)
                .build()
        );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void decUserLostAuction(String userId, String auctionRoomId) {
        executeAtomicDeltaUpdate(userId, -ConstantsUtils.ReputationConstants.LOSS_REPUTATION_PENALTY);

        // Publish event for losing auction
        eventPublisher.publishEvent(
            LostAuctionPenaltyEvent.builder()
                .userId(userId)
                .auctionRoomId(auctionRoomId)
                .penaltyAmount(ConstantsUtils.ReputationConstants.LOSS_REPUTATION_PENALTY)
                .build()
        );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void decUsersLostForBatch(List<String> loserIds, String auctionRoomId) {
        if (loserIds == null || loserIds.isEmpty()) return;

        Query query = new Query().addCriteria(
            Criteria.where("userId").in(loserIds).and("isDeleted").is(false)
        );

        double penalty = ConstantsUtils.ReputationConstants.LOSS_REPUTATION_PENALTY;
        double minRep = ConstantsUtils.ReputationConstants.MIN_REPUTATION_SCORE;

        Document clampStage = new Document("$set", new Document("reputation", 
            new Document("$max", List.of(
            minRep, 
            new Document("$subtract", List.of("$reputation", penalty))
            ))
        ));

        Update update = Update.fromDocument(new Document("$set", clampStage));

        mongoTemplate.updateMulti(query, update, User.class);

        for (String loserId : loserIds) {
            eventPublisher.publishEvent(
                LostAuctionPenaltyEvent.builder()
                    .userId(loserId)
                    .auctionRoomId(auctionRoomId)
                    .penaltyAmount(ConstantsUtils.ReputationConstants.LOSS_REPUTATION_PENALTY)
                    .build()
            );
        }
    }

    // Utility methods
    private User fetchActiveUser(String userId) {
        User user = userRepo.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        ReputationUtils.validateReputationScore(user.getReputation());

        return user;
    }

    private void executeAtomicDeltaUpdate(String userId, double delta) {
        Query query = new Query(
            Criteria.where("_id").is(userId)
                .and("isDeleted").is(false)
            );

        Update update = new Update();
        update.inc("reputation", delta);

        User updatedUser = mongoTemplate.findAndModify(
            query, 
            update, 
            FindAndModifyOptions.options().returnNew(true), 
            User.class
        );

        if (updatedUser == null) {
            log.error("Atomic update failed. User with ID [{}] not found or is deleted", userId);
            throw new AppException("User not found", HttpStatus.NOT_FOUND);
        }

        double currentClamp = updatedUser.getReputation();
        double clampedReputation = ReputationUtils.clampReputation(currentClamp);

        if (Double.compare(currentClamp, clampedReputation) != 0) {
            mongoTemplate.updateFirst(query, new Update().set("reputation", clampedReputation), User.class);
        }
    }
}
