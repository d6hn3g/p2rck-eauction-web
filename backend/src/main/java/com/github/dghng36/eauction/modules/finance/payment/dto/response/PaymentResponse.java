package com.github.dghng36.eauction.modules.finance.payment.dto.response;

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
public class PaymentResponse {
    String userId;
    
    String paymentId;
    String paymentCode;

    String paymentProvider;
    String paymentType;
    String paymentStatus;

    BigDecimal amount;

    String providerTransactionId;

    String failureReason;

    String description;
    Map<String, Object> metadata;

    Instant createdAt;

    Instant updatedAt;
}
