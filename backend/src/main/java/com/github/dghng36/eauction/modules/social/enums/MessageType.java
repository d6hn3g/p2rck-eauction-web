package com.github.dghng36.eauction.modules.social.enums;

import java.util.Arrays;
import java.util.Optional;

public enum MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM;

    public static Optional<MessageType> fromString(String type) {
        if (type == null|| type.isBlank()) {
            return Optional.empty();
        }

        String normalize = type.trim().toUpperCase();
        return Arrays.stream(MessageType.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
