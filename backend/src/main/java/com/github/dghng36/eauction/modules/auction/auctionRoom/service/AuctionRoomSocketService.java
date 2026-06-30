package com.github.dghng36.eauction.modules.auction.auctionRoom.service;

import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.ParticipateAuctionRoomSocketResponse;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuctionRoomSocketService {
    AuctionRoomParticipantService auctionRoomParticipantService;

    public ParticipateAuctionRoomSocketResponse participateAuctionRoomSocket(
        String userId,
        String auctionRoomId
    ) {
        
        return ParticipateAuctionRoomSocketResponse.builder()
            .auctionRoomId(auctionRoomId)
            .userId(userId)
            .hasParticipated(auctionRoomParticipantService.isParticipantApproved(auctionRoomId, userId))
            .build();
    }
}
