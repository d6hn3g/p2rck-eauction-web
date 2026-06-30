package com.github.dghng36.eauction.modules.finance.enums;

import java.util.Arrays;
import java.util.Optional;

public enum TransactionStatus {
    PENDING,

    SUCCESS,

    FAILED,
    
    CANCELED;

    public static Optional<TransactionStatus> fromString(String status) {
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }

        String normalize = status.trim().toUpperCase();
        return Arrays.stream(TransactionStatus.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
