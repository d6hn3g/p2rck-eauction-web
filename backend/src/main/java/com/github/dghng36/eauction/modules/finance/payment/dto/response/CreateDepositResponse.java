package com.github.dghng36.eauction.modules.finance.payment.dto.response;

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
public class CreateDepositResponse {
    String paymentId;

    String paymentCode;

    String paymentUrl;

    String paymentStatus;

    String description;
    Map<String, Object> metadata;

    Instant createdAt;
}
