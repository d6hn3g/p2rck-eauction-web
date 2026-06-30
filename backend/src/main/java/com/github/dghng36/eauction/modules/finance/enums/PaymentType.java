package com.github.dghng36.eauction.modules.finance.enums;

import java.util.Arrays;
import java.util.Optional;

public enum PaymentType {
    DEPOSIT,

    WITHDRAW;

    public static Optional<PaymentType> fromString(String type) {
        if (type == null || type.isBlank()) {
            return Optional.empty();
        }

        String normalize = type.trim().toUpperCase();
        return Arrays.stream(PaymentType.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
