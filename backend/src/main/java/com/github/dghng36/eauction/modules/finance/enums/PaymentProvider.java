package com.github.dghng36.eauction.modules.finance.enums;

import java.util.Arrays;
import java.util.Optional;

public enum PaymentProvider {
    SE_PAY,

    VN_PAY;

    public static Optional<PaymentProvider> fromString(String provider) {
        if (provider == null || provider.isBlank()) {
            return Optional.empty();
        }

        String normalize = provider.trim().toUpperCase();
        return Arrays.stream(PaymentProvider.values())
            .filter(p -> p.name().equals(normalize))
            .findFirst();
    }
}
