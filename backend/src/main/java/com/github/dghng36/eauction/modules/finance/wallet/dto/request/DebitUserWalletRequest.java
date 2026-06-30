package com.github.dghng36.eauction.modules.finance.wallet.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
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
public class DebitUserWalletRequest {
    @Digits(
        integer = 15,
        fraction = 2,
        message = "Invalid amount format"
    )
    BigDecimal amount;
}
