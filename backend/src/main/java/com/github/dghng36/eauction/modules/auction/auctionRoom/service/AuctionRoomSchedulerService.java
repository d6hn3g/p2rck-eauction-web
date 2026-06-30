package com.github.dghng36.eauction.modules.auction.auctionRoom.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionCanceledEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionEndedEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionLostEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionStartedEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionWinnerEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.product.service.AuctionProductService;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuctionRoomSchedulerService {
    MongoTemplate mongoTemplate;

    AuctionRoomParticipantService auctionRoomParticipantService;
    AuctionProductService auctionProductService;
    InternalUserService internalUserService;

    ApplicationEventPublisher eventPublisher;

    JobExecutorTasks jobExecutorTasks;

    public void notifyAllUpcomingAuctionRooms() {
        // Find all upcoming auction rooms that should be notified
        Instant now = Instant.now();

        Instant upcomingTime = now.plus(15, ChronoUnit.MINUTES);
        
        Query findQuery = new Query(
            Criteria.where("startTime").lte(upcomingTime)
                .and("status").is(AuctionRoomStatus.UPCOMING)
                .and("isDeleted").is(false)
        );

        List<AuctionRoom> upcomingAuctionRooms = mongoTemplate.find(findQuery, AuctionRoom.class);
        if (upcomingAuctionRooms.isEmpty()) {
            return;
        }

        for (AuctionRoom auctionRoom : upcomingAuctionRooms) {
            List<String> participantIds = auctionRoomParticipantService.getActiveParticipantIds(auctionRoom.getId());

            eventPublisher.publishEvent(
                AuctionStartedEvent.builder()
                    .auctionRoomId(auctionRoom.getId())
                    .auctionTitle(auctionRoom.getTitle())
                    .participantIds(participantIds)
                    .build()  
            );
        }

        log.info("Notified all upcoming auction rooms, count: [{}]", upcomingAuctionRooms.size());
    }

    public void startAllUpcomingAuctionRooms() {
        // Find all upcoming auction rooms that should be started
        Query findQuery = new Query(
            Criteria.where("startTime").lte(Instant.now())
                .and("status").is(AuctionRoomStatus.UPCOMING)
                .and("isDeleted").is(false)
        );
        findQuery.fields().include("id").include("title");

        List<AuctionRoom> upcomingAuctionRooms = mongoTemplate.find(findQuery, AuctionRoom.class);
        if (upcomingAuctionRooms.isEmpty()) {
            return;
        }

        List<String> auctionRoomIds = upcomingAuctionRooms.stream()
            .map(auctionRoom -> auctionRoom.getId())
            .toList();

        // Update the status of the found auction rooms to "ONGOING"
        Query updateQuery = new Query(
            Criteria.where("_id").in(auctionRoomIds)
                .and("status").is(AuctionRoomStatus.UPCOMING)
                .and("isDeleted").is(false)
        );
        Update update = new Update().set("status", AuctionRoomStatus.ONGOING);

        UpdateResult result = mongoTemplate.updateMulti(updateQuery, update, AuctionRoom.class);
        if (result.getModifiedCount() == 0) {
            return;
        }

        // Publish events for the started auction rooms
        for (AuctionRoom auctionRoom : upcomingAuctionRooms) {
            // Mark product as in auction
            auctionProductService.markProductInAuction(auctionRoom.getAuctionProduct().getProductId());
            
            List<String> participantIds = auctionRoomParticipantService.getActiveParticipantIds(auctionRoom.getId());

            eventPublisher.publishEvent(
                AuctionStartedEvent.builder()
                    .auctionRoomId(auctionRoom.getId())
                    .auctionTitle(auctionRoom.getTitle())
                    .participantIds(participantIds)
                    .build()  
            );
        }

        log.info("Started all upcoming auction rooms, count: [{}]", upcomingAuctionRooms.size());
    }

    public void endAllOngoingAuctionRooms() {
        Instant now = Instant.now();

        Query findQuery = new Query(
            Criteria.where("endTime").lte(now)
                .and("status").is(AuctionRoomStatus.ONGOING)
                .and("isDeleted").is(false)
        );

        List<AuctionRoom> ongoingAuctionRooms = mongoTemplate.find(findQuery, AuctionRoom.class);
        if (ongoingAuctionRooms.isEmpty()) {
            return;
        }

        List<String> auctionRoomIds = ongoingAuctionRooms.stream()
            .map(auctionRoom -> auctionRoom.getId())
            .toList();

        Query updateQuery = new Query(
            Criteria.where("_id").in(auctionRoomIds)
                .and("status").is(AuctionRoomStatus.ONGOING)
                .and("isDeleted").is(false)
        );
        Update update = new Update().set("status", AuctionRoomStatus.ENDED);

        UpdateResult result = mongoTemplate.updateMulti(updateQuery, update, AuctionRoom.class);
        if (result.getModifiedCount() == 0) {
            return;
        }

        for (AuctionRoom auctionRoom : ongoingAuctionRooms) {
            List<String> participantIds = auctionRoomParticipantService.getActiveParticipantIds(auctionRoom.getId());

            eventPublisher.publishEvent(
                AuctionEndedEvent.builder()
                    .auctionRoomId(auctionRoom.getId())
                    .auctionTitle(auctionRoom.getTitle())
                    .winnerId(auctionRoom.getLastWinnerId())
                    .participantIds(participantIds)
                    .build()
            );

            processAuctionResult(auctionRoom, participantIds);
        }
    }

    private void processAuctionResult(AuctionRoom auctionRoom, List<String> participantIds) {
        String roomId = auctionRoom.getId();
        String roomTitle = auctionRoom.getTitle();
        String winnerId = auctionRoom.getLastWinnerId();

        // Publish winner event if there is a winner, otherwise publish cancellation event
        if (auctionRoom.getLastWinnerId() == null) {
            // Mark the product as available again
            auctionProductService.markProductAvailable(auctionRoom.getAuctionProduct().getProductId());
            
            eventPublisher.publishEvent(
                AuctionCanceledEvent.builder()
                    .auctionRoomId(auctionRoom.getId())
                    .auctionTitle(auctionRoom.getTitle())
                    .cancelReason("No bids placed")
                    .participantIds(participantIds)
                    .build()  
            );
            return;
        }

        // Mark the product as sold
        auctionProductService.markProductAsSold(auctionRoom.getAuctionProduct().getProductId());

        eventPublisher.publishEvent(
            AuctionWinnerEvent.builder()
                .auctionRoomId(auctionRoom.getId())
                .auctionTitle(auctionRoom.getTitle())
                .winnerId(auctionRoom.getLastWinnerId())
                .winningPrice(auctionRoom.getCurrentMaxPrice())
                .build()  
        );

        // Update the reputation of the winner and losers concurrently
        List<String> loserIds = participantIds.stream()
            .filter(id -> !id.equals(winnerId))
            .toList();

        if (loserIds.isEmpty()) {
            internalUserService.incReputationWon(winnerId, roomId);
            return;
        }

        jobExecutorTasks.runAsync(() -> {
            try {
                internalUserService.incReputationWon(winnerId, roomId);

                internalUserService.decReputationLostForBatch(loserIds, roomId);

                for (String loserId : loserIds) {
                    eventPublisher.publishEvent(
                        AuctionLostEvent.builder()
                            .auctionRoomId(roomId)
                            .auctionTitle(roomTitle)
                            .loserId(loserId)
                            .build()
                    );
                }
                
                log.info("Successfully processed post-auction logistics for room: [{}]", roomId);
            } catch (Exception ex) {
                log.error("Critical to process post-auction background tasks for room [{}]: ", roomId, ex);
            }
        });

        log.info("Auction room [{}] result submitted to background worker. Main thread released.", roomId);
    }

}
