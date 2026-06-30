package com.github.dghng36.eauction.modules.auction.auctionRoom.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoomParticipant;
import com.github.dghng36.eauction.modules.auction.enums.ParticipantStatus;

public interface AuctionRoomParticipantRepository extends MongoRepository<AuctionRoomParticipant, String> {
    List<AuctionRoomParticipant> findAllByUserIdAndStatusIn(String userId, List<ParticipantStatus> status);

    boolean existsByAuctionRoomIdAndUserId(String auctionRoomId, String userId);

    boolean existsByAuctionRoomIdAndUserIdAndStatus(String auctionRoomId, String userId, ParticipantStatus status);
    
    boolean existsByAuctionRoomIdAndUserIdAndStatusIn(String auctionRoomId, String userId, List<ParticipantStatus> status);

    List<AuctionRoomParticipant> findAllByAuctionRoomIdAndStatus(String auctionRoomId, ParticipantStatus status);
}
