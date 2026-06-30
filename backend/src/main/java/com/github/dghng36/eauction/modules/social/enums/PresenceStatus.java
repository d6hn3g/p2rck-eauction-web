package com.github.dghng36.eauction.modules.social.enums;

import java.util.Arrays;
import java.util.Optional;

public enum PresenceStatus {
    ONLINE,

    OFFLINE,

    DO_NOT_DISTURB;

    public static Optional<PresenceStatus> fromString(String status) {
        if (status == null|| status.isBlank()) {
            return Optional.empty();
        }

        String normalize = status.trim().toUpperCase();
        return Arrays.stream(PresenceStatus.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
