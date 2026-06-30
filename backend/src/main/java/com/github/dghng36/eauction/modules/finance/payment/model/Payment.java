package com.github.dghng36.eauction.modules.finance.payment.model;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.finance.enums.PaymentProvider;
import com.github.dghng36.eauction.modules.finance.enums.PaymentStatus;
import com.github.dghng36.eauction.modules.finance.enums.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "payments")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Payment extends BaseEntity {
    @Indexed(unique = true)
    String paymentCode;

    @Indexed(unique = true)
    String userId;

    PaymentProvider provider;
    PaymentType type;
    PaymentStatus status;

    BigDecimal amount;

    @Indexed(unique = true)
    String providerTransactionId; // After executing payment with provider, we will get transaction id from provider and save it here

    @Indexed(unique = true)
    String bankAccountId;

    String failureReason;

    String description;
    Map<String, Object> metadata;

    Map<String, String> callbackData;
}
