package com.github.dghng36.eauction.modules.auction.bid.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.websocket.enums.SocketEventType;
import com.github.dghng36.eauction.core.websocket.publisher.SocketPublisher;
import com.github.dghng36.eauction.modules.auction.bid.event.BidPlacedEvent;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@Slf4j
public class BidSocketListener {

    SocketPublisher socketPublisher;
    
    @Async
    @EventListener
    public void handleBidPlacedEvent(
        BidPlacedEvent event
    ) {
        String auctionRoomId = event.getAuctionRoomId();

        try {
            log.info("New bid is placed: [{}] from auction room: [{}]", event.getBidId(), event.getAuctionRoomId());

            socketPublisher.publish(
                "/topic/auctions/rooms/" + auctionRoomId + "/bids", 
                SocketEventType.NEW_BID, 
                event
            );
        } catch (Exception ex) {
            log.error("Failed during new place bid: [{}]: ", ex.getMessage(), ex);

        }
    }
}
