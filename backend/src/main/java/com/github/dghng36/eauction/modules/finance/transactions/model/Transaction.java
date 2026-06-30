package com.github.dghng36.eauction.modules.finance.transactions.model;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.finance.enums.TransactionStatus;
import com.github.dghng36.eauction.modules.finance.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "finance_transactions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Transaction extends BaseEntity {
    @Indexed(unique = true)
    String transactionCode;

    @Indexed
    String walletId;

    @Indexed
    String userId;

    TransactionType type;
    TransactionStatus status;

    BigDecimal amount;

    BigDecimal availableBefore;
    BigDecimal availableAfter;

    BigDecimal holdBefore;
    BigDecimal holdAfter;

    @Indexed
    String referenceId;

    String description;

    Map<String, Object> metadata;
}
