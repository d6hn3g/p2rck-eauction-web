package com.github.dghng36.eauction.modules.auction.bid.listener;

import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.auction.bid.event.BidOutbidEvent;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.github.dghng36.eauction.modules.notification.enums.NotificationType;
import com.github.dghng36.eauction.modules.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BidNotificationListener {
    NotificationService notificationService;

    InternalWalletService internalWalletService;

    @Async
    @EventListener
    public void handleBidOutbidEvent(
        BidOutbidEvent event
    ) {
        String outbidUserId = event.getOutbidUserId();
        log.info("Processing asynchronous un-hold balance for outbid user: [{}], because auction room [{}] has a new highest price: [{}]", 
            outbidUserId, event.getAuctionRoomId(), event.getCurrentHighestPrice());

        Double unHoldAmount = event.getCurrentHighestPrice() - event.getPreviousHighestPrice();

        try {
            internalWalletService.unHoldBalance(outbidUserId, unHoldAmount);
        } catch (Exception ex) {
            log.error("Failed to un-hold balance for user: [{}] in async flow, error: [{}]: ", outbidUserId, ex.getMessage(), ex);
        }

        notificationService.createNotification(
            event.getOutbidUserId(),
            NotificationType.BID_OUTBID,
            "Your bid has been outbid",
            "Another bidder has placed a higher bid",
            event.getAuctionRoomId(),
            Map.of(
                "auctionRoomId", event.getAuctionRoomId(),
                "newHighestBidder", event.getNewHighestBidderId(),
                "previousHighestPrice", event.getPreviousHighestPrice(),
                "currentHighestPrice", event.getCurrentHighestPrice()
            )
        );
    }
}
