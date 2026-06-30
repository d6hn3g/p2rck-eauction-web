package com.github.dghng36.eauction.modules.auction.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ProductStatus {
    AVAILABLE,
    IN_AUCTION,
    SOLD;

    public static Optional<ProductStatus> fromString(String status) {
        if (status == null|| status.isBlank()) {
            return Optional.empty();
        }

        String normalize = status.trim().toUpperCase();
        return Arrays.stream(ProductStatus.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
