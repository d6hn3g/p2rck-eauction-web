package com.github.dghng36.eauction.modules.finance.enums;

import java.util.Arrays;
import java.util.Optional;

public enum BankAccountStatus {
    PENDING_VERIFIED,

    VERIFIED,

    REJECTED;

    public static Optional<BankAccountStatus> fromString(String status) {
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }
        String normalize = status.trim().toUpperCase();
        return Arrays.stream(BankAccountStatus.values())
            .filter(s -> s.name().equals(normalize))
            .findFirst();
    }
}
