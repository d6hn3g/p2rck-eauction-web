package com.github.dghng36.eauction.modules.identity.enums;

import java.util.Arrays;
import java.util.Optional;

public enum UserRole {
    ADMIN,
    MANAGER,
    USER;

    public String toSpringAuthority() {
        return "ROLE_" + this.name();
    }

    public static Optional<UserRole> fromString(String role) {
        if (role == null|| role.isBlank()) {
            return Optional.empty();
        }

        String normalize = role.trim().toUpperCase();
        return Arrays.stream(UserRole.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
