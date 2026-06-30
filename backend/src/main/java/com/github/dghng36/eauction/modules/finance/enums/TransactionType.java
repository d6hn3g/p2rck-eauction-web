package com.github.dghng36.eauction.modules.finance.enums;

import java.util.Arrays;
import java.util.Optional;

public enum TransactionType {
    DEPOSIT_SUCCESS,
    DEPOSIT_REFUND,
    
    WITHDRAW_HOLD,
    WITHDRAW_SUCCESS,
    WITHDRAW_REFUND;

    public static Optional<TransactionType> fromString(String type) {
        if (type == null|| type.isBlank()) {
            return Optional.empty();
        }

        String normalize = type.trim().toUpperCase();
        return Arrays.stream(TransactionType.values())
            .filter(r -> r.name().equals(normalize))
            .findFirst();
    }
}
