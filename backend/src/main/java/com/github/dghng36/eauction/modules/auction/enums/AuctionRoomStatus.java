package com.github.dghng36.eauction.modules.auction.enums;

import java.util.Arrays;
import java.util.Optional;

public enum AuctionRoomStatus {
    PENDING_VERIFIED,
    UPCOMING,
    ONGOING,
    ENDED,
    CANCELLED;

    public static Optional<AuctionRoomStatus> fromString(String status) {
        if (status == null|| status.isBlank()) {
            return Optional.empty();
        }

        String normalize = status.trim().toUpperCase();
        return Arrays.stream(AuctionRoomStatus.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
