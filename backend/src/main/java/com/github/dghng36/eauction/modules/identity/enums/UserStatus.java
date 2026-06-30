package com.github.dghng36.eauction.modules.identity.enums;

import java.util.Arrays;
import java.util.Optional;

public enum UserStatus {
    PENDING,
    VERIFIED,
    BLOCKED,
    BANNED;

    public static Optional<UserStatus> fromString(String status) {
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }

        String normalize = status.trim().toUpperCase();
        return Arrays.stream(UserStatus.values())
            .filter(s -> s.name().equals(normalize))
            .findFirst();
    }
}
