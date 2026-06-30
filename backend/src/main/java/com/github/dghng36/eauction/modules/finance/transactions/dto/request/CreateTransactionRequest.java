package com.github.dghng36.eauction.modules.finance.transactions.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateTransactionRequest {
    String walletId;

    String transactionType;

    BigDecimal amount;

    BigDecimal availableBefore;
    BigDecimal availableAfter;

    BigDecimal holdBefore;
    BigDecimal holdAfter;

    String referenceId;

    String description;

    Map<String, Object> metadata;
}
