package com.github.dghng36.eauction.modules.finance.payment.event;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class PaymentEvent {
    String userId;

    String paymentId;
    String paymentCode;

    BigDecimal amount;

    String description;
}
