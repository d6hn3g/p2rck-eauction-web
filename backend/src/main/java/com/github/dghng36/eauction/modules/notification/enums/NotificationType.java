package com.github.dghng36.eauction.modules.notification.enums;

import java.util.Arrays;
import java.util.Optional;

public enum NotificationType {
    WELCOME_BONUS,
    
    WEEKLY_LOGIN_BONUS,

    AUCTION_ROOM_CREATED_BONUS,

    AUCTION_ROOM_PARTICIPATED_BONUS,

    AUCTION_WON_BONUS,

    AUCTION_LOST_PENALTY,

    NEW_MESSAGE,

    AUCTION_WON,

    AUCTION_LOST,

    AUCTION_UPCOMING,

    AUCTION_STARTED,

    AUCTION_ENDED,

    AUCTION_CANCELED,

    BID_OUTBID,

    VERIFIED_BANK_ACCOUNT,

    REJECTED_BANK_ACCOUNT,

    SYSTEM;

    public static Optional<NotificationType> fromString(String type) {
        if (type == null|| type.isBlank()) {
            return Optional.empty();
        }

        String normalize = type.trim().toUpperCase();
        return Arrays.stream(NotificationType.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
