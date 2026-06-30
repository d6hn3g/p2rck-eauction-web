package com.github.dghng36.eauction.modules.finance.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateWithDrawResponse {
    String paymentId;

    String paymentCode;

    BigDecimal amount;

    String paymentStatus;

    String bankName;
    String bankCode;
    String accountNumber;
    String accountHolderName;

    String description;
    Map<String, Object> metadata;

    Instant createdAt;
}
