package com.github.dghng36.eauction.modules.identity.enums;

import java.util.Arrays;
import java.util.Optional;

public enum UserAuctionStatus {
    JOINED,
    CREATED,
    WIN,
    BIDS;

    public static Optional<UserAuctionStatus> fromString(String status) {
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }

        String normalize = status.trim().toUpperCase();
        return Arrays.stream(UserAuctionStatus.values())
            .filter(s -> s.name().equals(normalize))
            .findFirst();
    }
}
