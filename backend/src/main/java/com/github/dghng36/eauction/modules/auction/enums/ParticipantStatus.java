package com.github.dghng36.eauction.modules.auction.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ParticipantStatus {
    PENDING,
    APPROVED,
    REJECTED,
    LEFT;

    public static Optional<ParticipantStatus> fromString(String status) {
        if (status == null|| status.isBlank()) {
            return Optional.empty();
        }

        String normalize = status.trim().toUpperCase();
        return Arrays.stream(ParticipantStatus.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
