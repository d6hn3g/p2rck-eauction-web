package com.github.dghng36.eauction.modules.social.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ConversationType {
    DIRECT,

    AUCTION_ROOM;

    public static Optional<ConversationType> fromString(String type) {
        if (type == null|| type.isBlank()) {
            return Optional.empty();
        }

        String normalize = type.trim().toUpperCase();
        return Arrays.stream(ConversationType.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
