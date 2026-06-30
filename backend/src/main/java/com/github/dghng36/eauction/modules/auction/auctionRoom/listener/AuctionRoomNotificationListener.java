package com.github.dghng36.eauction.modules.auction.auctionRoom.listener;

import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionCanceledEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionEndedEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionLostEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionStartedEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionUpcomingEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionWinnerEvent;
import com.github.dghng36.eauction.modules.notification.enums.NotificationType;
import com.github.dghng36.eauction.modules.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuctionRoomNotificationListener {
    NotificationService notificationService;

    @Async
    @EventListener
    public void handleAuctionUpcomingEvent(
        AuctionUpcomingEvent event
    ) {
        for (String participantId : event.getParticipantIds()) {
            try {
                notificationService.createNotification(
                    participantId, 
                    NotificationType.AUCTION_UPCOMING, 
                    "Auction upcoming", 
                    "Auction room " + event.getAuctionTitle() + " is starting in 15 minutes.", 
                    event.getAuctionRoomId(), 
                    Map.of(
                        "auctionId", event.getAuctionRoomId(),
                        "auctionTitle", event.getAuctionTitle()
                    )
                );
            } catch(Exception ex) {
                log.error("Error exists during create new notification AUCTION_UPCOMING" +
                    "for auction room: [{}], participant: [{}], error: [{}]: ", 
                    event.getAuctionRoomId(),
                    participantId,
                    ex.getMessage(),
                    ex
                );
            }
        }
    }

    @Async
    @EventListener
    public void handleAuctionStartedEvent(
        AuctionStartedEvent event
    ) {
        for (String participantId : event.getParticipantIds()) {
            try {
                notificationService.createNotification(
                    participantId, 
                    NotificationType.AUCTION_STARTED, 
                    "Auction started", 
                    "Auction room " + event.getAuctionTitle() + " has started.", 
                    event.getAuctionRoomId(), 
                    Map.of(
                        "auctionId", event.getAuctionRoomId(),
                        "auctionTitle", event.getAuctionTitle()
                    )
                );
            } catch(Exception ex) {
                log.error("Error exists during create new notification AUCTION_STARTED" +
                    "for auction room: [{}], participant: [{}], error: [{}]: ", 
                    event.getAuctionRoomId(),
                    participantId,
                    ex.getMessage(),
                    ex
                );
            }
        }
    }

    @Async
    @EventListener
    public void handleAuctionEndedEvent(
        AuctionEndedEvent event
    ) {
        
        for (String participantId : event.getParticipantIds()) {
            try {
                notificationService.createNotification(
                    participantId,
                    NotificationType.AUCTION_ENDED,
                    "AUCTION ENDED",
                    "Auction room " + event.getAuctionTitle() + " has ended",
                    event.getAuctionRoomId(),
                    Map.of(
                        "auctionId", event.getAuctionRoomId(),
                        "auctionTitle", event.getAuctionTitle()
                    )
                );
            } catch(Exception ex) {
                log.error("Error exists during create new notification AUCTION_ENDED" +
                    "for auction room: [{}], participant: [{}], error: [{}]: ", 
                    event.getAuctionRoomId(),
                    participantId,
                    ex.getMessage(),
                    ex
                );
            }
        }
    }

    @Async
    @EventListener
    public void handleAuctionCanceledEvent(
        AuctionCanceledEvent event
    ) {
        for (String participantId : event.getParticipantIds()) {
            try {
                notificationService.createNotification(
                    participantId,
                    NotificationType.AUCTION_CANCELED,
                    "AUCTION CANCELED",
                    "Auction room " + event.getAuctionTitle() + " has been canceled. Reason: " + event.getCancelReason(),
                    event.getAuctionRoomId(),
                    Map.of(
                        "auctionId", event.getAuctionRoomId(),
                        "auctionTitle", event.getAuctionTitle(),
                        "cancelReason", event.getCancelReason()
                    )
                );
            } catch(Exception ex) {
                log.error("Error exists during create new notification AUCTION_CANCELED" +
                    "for auction room: [{}], participant: [{}], error: [{}]: ", 
                    event.getAuctionRoomId(),
                    participantId,
                    ex.getMessage(),
                    ex
                );
            }
        }
    }

    @Async
    @EventListener
    public void handleAuctionWinnerEvent(
        AuctionWinnerEvent event
    ) {
        try {
            notificationService.createNotification(
                event.getWinnerId(),
                NotificationType.AUCTION_WON,
                "AUCTION WON",
                "Congratulations! You won the auction room " + event.getAuctionTitle() + " with a winning price of " + event.getWinningPrice(),
                event.getAuctionRoomId(),
                Map.of(
                    "auctionId", event.getAuctionRoomId(),
                    "auctionTitle", event.getAuctionTitle(),
                    "winningPrice", String.valueOf(event.getWinningPrice())
                )
            );
        } catch(Exception ex) {
            log.error("Error exists during create new notification AUCTION_WINNER_EVENT" +
                "for auction room: [{}] to winner: [{}], error: [{}]: ", 
                event.getAuctionRoomId(),
                event.getWinnerId(),
                ex.getMessage(),
                ex
            );
        }
    }

    @Async
    @EventListener
    public void handleAuctionLostEvent(
        AuctionLostEvent event
    ) {
        try {
            notificationService.createNotification(
                event.getLoserId(),
                NotificationType.AUCTION_LOST,
                "AUCTION LOST",
                "Unfortunately, you lost the auction room " + event.getAuctionTitle(),
                event.getAuctionRoomId(),
                Map.of(
                    "auctionId", event.getAuctionRoomId(),
                    "auctionTitle", event.getAuctionTitle()
                )
            );
        } catch(Exception ex) {
            log.error("Error exists during create new notification AUCTION_LOSER_EVENT" +
                "for auction room: [{}] to loser: [{}], error: [{}]: ", 
                event.getAuctionRoomId(),
                event.getLoserId(),
                ex.getMessage(),
                ex
            );
        }
    }
}
