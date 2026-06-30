package com.github.dghng36.eauction.modules.finance.payment.dto.request;

import java.math.BigDecimal;
import java.time.Instant;

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
public class SePayWebhookRequest {
    Long id;

    String gateway; // Name of bank 

    BigDecimal amountIn;
    BigDecimal amountOut;

    String content;

    String accountNumber;

    String code;
    
    Instant transactionDate;
}
