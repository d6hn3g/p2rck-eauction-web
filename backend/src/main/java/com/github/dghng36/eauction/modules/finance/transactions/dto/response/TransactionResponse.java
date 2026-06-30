package com.github.dghng36.eauction.modules.finance.transactions.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class TransactionResponse {
    String id;

    String transactionCode;

    String transactionType;
    String transactionStatus;

    BigDecimal amount;

    BigDecimal availableBefore;
    BigDecimal availableAfter;

    BigDecimal holdBefore;
    BigDecimal holdAfter;

    BigDecimal totalBefore;
    BigDecimal totalAfter;

    String referenceId;

    String description;

    Map<String, Object> metadata;

    Instant createdAt;
}
