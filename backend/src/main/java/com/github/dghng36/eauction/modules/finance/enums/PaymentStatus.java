package com.github.dghng36.eauction.modules.finance.enums;

import java.util.Arrays;
import java.util.Optional;

public enum PaymentStatus {
    PENDING,

    PROCESSING,

    SUCCESS,

    FAILED,

    CANCELLED,

    EXPIRED;

    public static Optional<PaymentStatus> fromString(String status) {
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }

        String normalize = status.trim().toUpperCase();
        return Arrays.stream(PaymentStatus.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
