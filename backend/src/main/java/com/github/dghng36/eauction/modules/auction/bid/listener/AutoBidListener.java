package com.github.dghng36.eauction.modules.auction.bid.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.auction.bid.event.BidPlacedEvent;
import com.github.dghng36.eauction.modules.auction.bid.service.AutoBidResolver;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AutoBidListener {
    AutoBidResolver autoBidResolver;

    @Async
    @EventListener
    public void handleBidPlacedEvent(
        BidPlacedEvent event
    ) {
        if (event.getIsAutoBid()) {
            return;
        }

        log.info("Auto-bid robot has triggered for auction room: [{}] by user: [{}], current hand-placed bid: [{}]", 
            event.getAuctionRoomId(), event.getUserId(), event.getBidAmount());

        try {
            autoBidResolver.processAutoBid(
                event.getAuctionRoomId(),
                event.getUserId(),
                event.getBidAmount()
            );
        } catch (Exception ex) {
            log.error("Emergency error in AutoBid Engine for auction room: [{}], error: [{}]: ", event.getAuctionRoomId(), ex.getMessage(), ex);
        }
    }
}
