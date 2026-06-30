package com.github.dghng36.eauction.modules.identity.reputation.listener;

import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.identity.reputation.event.CreatedAuctionAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.LostAuctionPenaltyEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.ParticipatedAuctionAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.WeeklyAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.WelcomeAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.WonAuctionBonusEvent;
import com.github.dghng36.eauction.modules.notification.enums.NotificationType;
import com.github.dghng36.eauction.modules.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReputationNotificationListener {
    NotificationService notificationService;

    @Async
    @EventListener
    public void handleWelcomeAwardedBonusEvent(
        WelcomeAwardedBonusEvent event
    ) {
        try {
            notificationService.createNotification(
                event.getUserId(),
                NotificationType.WELCOME_BONUS,
                "Welcome new user!!",
                "I hope you enjoy your experience on our platform!",
                null,
                Map.of(
                    "awardedBonus", event.getAwardedAmount()
                )
            );
        } catch(Exception ex) {
            log.error("Failed to create new notification WELCOME_AWARDED_BONUS: [{}]: ", ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void handleWeeklyAwardedBonusEvent(
        WeeklyAwardedBonusEvent event
    ) {
        try {
             notificationService.createNotification(
                event.getUserId(),
                NotificationType.WEEKLY_LOGIN_BONUS,
                "Weekly Bonus",
                "You have received a weekly bonus for your activity on the platform. Keep up the good work!",
                null,
                Map.of(
                    "awardedBonus", event.getAwardedAmount()
                )
            );
        } catch(Exception ex) {
            log.error("Failed to create new notification WEEKLY_AWARDED_BONUS: [{}]: ", ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void handleCreatedAuctionAwardedBonusEvent(
        CreatedAuctionAwardedBonusEvent event
    ) {
        try {
            notificationService.createNotification(
                event.getUserId(),
                NotificationType.AUCTION_ROOM_CREATED_BONUS,
                "Created Auction Bonus",
                "You have received a bonus for creating an auction. Thank you for contributing to our community!",
                event.getAuctionRoomId(),
                Map.of(
                    "awardedBonus", event.getAwardedAmount()
                )
            );
        } catch(Exception ex) {
            log.error("Failed to create new notification CREATED_AUCTION_AWARDED_BONUS: [{}]: ", ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void handleParticipatedAuctionAwardedBonusEvent(
        ParticipatedAuctionAwardedBonusEvent event
    ) {
        try {
            notificationService.createNotification(
                event.getUserId(),
                NotificationType.AUCTION_ROOM_PARTICIPATED_BONUS,
                "Participated Auction Bonus",
                "You have received a bonus for participating in an auction. Thank you for being an active member of our community!",
                event.getAuctionRoomId(),
                Map.of(
                    "awardedBonus", event.getAwardedAmount()
                )
            );
        } catch(Exception ex) {
            log.error("Failed to create new notification PARTICIPATED_AUCTION_AWARDED_BONUS: [{}]: ", ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void handleWonAuctionAwardedBonusEvent(
        WonAuctionBonusEvent event
    ) {
        try {
            notificationService.createNotification(
                event.getUserId(),
                NotificationType.AUCTION_WON_BONUS,
                "Won Auction Bonus",
                "Congratulations! You have received a bonus for winning an auction. Thank you for being an active member of our community!",
                event.getAuctionRoomId(),
                Map.of(
                    "awardedBonus", event.getAwardedAmount()
                )
            );
        } catch(Exception ex) {
            log.error("Failed to create new notification WON_AUCTION_AWARDED_BONUS: [{}]: ", ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void handleLostAuctionPenaltyEvent(
        LostAuctionPenaltyEvent event
    ) {
        try {
            notificationService.createNotification(
                event.getUserId(),
                NotificationType.AUCTION_LOST_PENALTY,
                "Lost Auction Penalty",
                "Oh no! You have received a penalty for losing an auction. Please try to be more competitive in future auctions!",
                event.getAuctionRoomId(),
                Map.of(
                    "penaltyAmount", event.getPenaltyAmount()
                )
            );
        } catch(Exception ex) {
            log.error("Failed to create new notification LOST_AUCTION_AWARDED_BONUS: [{}]: ", ex.getMessage(), ex);
        }
    }
}
