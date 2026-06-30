package com.github.dghng36.eauction.modules.finance.wallet.dto.request;

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
public class HoldBalanceUserWalletRequest {
    String paymentId;

    String paymentCode;

    String description;

    Map<String, Object> metadata;

    BigDecimal holdAmount;
}
