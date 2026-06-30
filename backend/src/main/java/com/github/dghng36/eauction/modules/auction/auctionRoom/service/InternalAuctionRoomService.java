package com.github.dghng36.eauction.modules.auction.auctionRoom.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.TransientClientSessionException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.internal.AuctionRoomInfo;
import com.github.dghng36.eauction.modules.auction.auctionRoom.mapper.AuctionRoomInfoMapper;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.auctionRoom.repository.AuctionRoomRepository;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;
import com.mongodb.MongoCommandException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/*
 * This file is used for other module
 */

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class InternalAuctionRoomService {
    MongoTemplate mongoTemplate;
    AuctionRoomRepository auctionRoomRepo;

    AuctionRoomParticipantService auctionRoomParticipantService;

    AuctionRoomInfoMapper auctionRoomInfoMapper;

    public String getProductId(String auctionRoomId) {
        return auctionRoomRepo.findByIdAndIsDeletedFalse(auctionRoomId)
            .map(room -> room.getAuctionProduct().getProductId())
            .orElseThrow(() -> new AppException("Auction room not found with id: " + auctionRoomId, HttpStatus.NOT_FOUND));
    }

    public boolean existsAuctionRoom(String auctionRoomId) {
        return auctionRoomRepo.existsByIdAndIsDeletedFalse(auctionRoomId);
    }

    public Map<String, AuctionRoomInfo> getAuctionRoomInfoByIds(Set<String> auctionRoomIds) {
        List<AuctionRoom> auctionRooms = auctionRoomRepo.findAllByIdInAndIsDeletedFalse(auctionRoomIds);

        return auctionRooms.stream()
            .collect(Collectors.toMap(auctionRoom -> auctionRoom.getId(), auctionRoom -> auctionRoomInfoMapper.toAuctionRoomInfo(auctionRoom.getId(), auctionRoom.getTitle())));
    }

    public AuctionRoom validateAndGetForBidding(String auctionRoomId, String userId) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(auctionRoomId).and("isDeleted").is(false));
        query.fields().include("status", "startTime", "endTime", "title", "auctionProduct");

        AuctionRoom auctionRoom = mongoTemplate.findOne(query, AuctionRoom.class);
        
        // Validate auction room status
        if (auctionRoom.getStatus() != null 
            && !AuctionRoomStatus.ONGOING.equals(auctionRoom.getStatus())
        ) {
            throw new AppException("Auction room is not ongoing", HttpStatus.BAD_REQUEST);
        }

        // Validate auction room time
        Instant now = Instant.now();
        if (auctionRoom.getStartTime() != null 
            && auctionRoom.getStartTime().isAfter(now)
        ) {
            throw new AppException("Auction room has not started yet", HttpStatus.BAD_REQUEST);
        }

        if (auctionRoom.getEndTime() != null 
            && auctionRoom.getEndTime().isBefore(now)
        ) {
            throw new AppException("Auction room has ended", HttpStatus.BAD_REQUEST);
        }

        if (!auctionRoomParticipantService.isParticipantInsideAuctionRoom(userId, auctionRoom.getId())) {
            throw new AppException("User is not a participant in this auction room", HttpStatus.FORBIDDEN);
        }

        return auctionRoom;
    }

    public void validateBidAmount(AuctionRoom auctionRoom, Double bidAmount) {
        AuctionProduct auctionProduct = auctionRoom.getAuctionProduct();
        Double currentPrice = auctionProduct.getCurrentPrice();
        Double priceStep = auctionProduct.getPriceStep();

        if (currentPrice == null || priceStep == null) {
            throw new AppException("Current price or price step is not set for this auction product", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (bidAmount < currentPrice + priceStep) {
            throw new AppException("Bid amount is less than the minimum required bid", HttpStatus.BAD_REQUEST);
        }
    }

    public boolean isBuyoutReached(AuctionRoom auctionRoom, Double bidAmount) {
        AuctionProduct auctionProduct = auctionRoom.getAuctionProduct();
        Double currentPrice = auctionProduct.getCurrentPrice();
        Double buyoutPrice = auctionProduct.getBuyoutPrice();
        

        if (buyoutPrice == null || currentPrice == null) {
            return false;
        }

        return currentPrice + bidAmount >= buyoutPrice;
    }

    @Retryable(
        retryFor = { 
            DataIntegrityViolationException.class, 
            TransientClientSessionException.class,
            DataIntegrityViolationException.class,
            MongoCommandException.class,
            ConcurrencyFailureException.class,
            AppException.class
        },
        maxAttempts = 5,
        backoff = @Backoff(delay = 50)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuctionRoom processNewBidSuccess(
        AuctionRoom auctionRoom, String winnerId,
        Double bidAmount
    ) {
        Instant now = Instant.now();

        Query query = new Query()
            .addCriteria(Criteria.where("_id").is(auctionRoom.getId())
                .and("isDeleted").is(false)
                .and("currentMaxPrice").lte(bidAmount)
        );

        Update update = new Update()
            .set("currentMaxPrice", bidAmount)
            .set("auctionProduct.currentPrice", bidAmount)
            .set("currentWinnerId", winnerId)
            .set("lastWinnerId", winnerId);

        if (Boolean.TRUE.equals(auctionRoom.getAllowAutoExtend()) 
            && auctionRoom.getEndTime() != null
            && auctionRoom.getEndTime().isBefore(now.plusSeconds(auctionRoom.getExtensionTime()))
        ) {
            update.set(
                "endTime",
                now.plusSeconds(auctionRoom.getExtensionTime())
            );
        }

        FindAndModifyOptions options = FindAndModifyOptions.options()
            .returnNew(true);

        AuctionRoom updated = mongoTemplate.findAndModify(query, update, options, AuctionRoom.class);

        if (updated == null) {
            throw new AppException(
                "Auction room has been updated by another bidder",
                HttpStatus.CONFLICT
            );
        }

        return updated;
    }

    public void updateAuctionRoomStatus(String auctionRoomId, String status) {
        AuctionRoomStatus newStatus = AuctionRoomStatus.fromString(status)
            .orElseThrow(() -> new AppException("Invalid auction room status: " + status, HttpStatus.BAD_REQUEST));

        Query query = new Query()
            .addCriteria(Criteria.where("_id").is(auctionRoomId).and("isDeleted").is(false));
        
        Update update = new Update()
            .set("status", newStatus);

        mongoTemplate.updateFirst(query, update, AuctionRoom.class);
    }

}
