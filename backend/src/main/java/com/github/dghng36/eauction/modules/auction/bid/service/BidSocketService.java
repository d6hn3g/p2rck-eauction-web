package com.github.dghng36.eauction.modules.auction.bid.service;

import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.modules.auction.bid.dto.request.DisableAutoBidSocketRequest;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.EnableAutoBidSocketRequest;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.PlaceBidSocketRequest;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class BidSocketService {
    BidService bidService;

    public void placeBid(String userId, PlaceBidSocketRequest placeBidSocketRequest) {
        bidService.placeBid(
            userId, 
            placeBidSocketRequest.getAuctionRoomId(), 
            placeBidSocketRequest.getBidAmount(),
            placeBidSocketRequest.getMetadata(),
            placeBidSocketRequest.getEnableAutoBid(),
            placeBidSocketRequest.getMaxAutoBidPrice(),
            placeBidSocketRequest.getIncrementAmount(),
            false
        );
    }

    public void enableAutoBid(String userId, EnableAutoBidSocketRequest enableAutoBidSocketRequest) {
        bidService.enableAutoBid(
            userId,
            enableAutoBidSocketRequest.getAuctionRoomId(),
            enableAutoBidSocketRequest.getMaxAutoBidPrice(),
            enableAutoBidSocketRequest.getIncrementAmount()
        );
    }

    public void disableAutoBid(String userId, DisableAutoBidSocketRequest disableAutoBidSocketRequest) {
        bidService.disableAutoBid(userId, disableAutoBidSocketRequest.getAuctionRoomId());

    }
}
