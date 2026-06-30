package com.github.dghng36.eauction.modules.finance.payment.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateDepositRequest {
    @NotNull(message = "Payment provider is required")
    String paymentProvider;

    @NotNull(message = "Amount is required")
    @DecimalMin(
        value = "10000",
        message = "Minimum deposit amount is 10,000 VND"
    )
    @Digits(
        integer = 15,
        fraction = 2,
        message = "Invalid amount format"
    )
    BigDecimal amount;

    @Size(
        max = ConstantsUtils.FinanceConstants.MAX_DESCRIPTION_LENGTH,
        message = "Description must not exceed " + ConstantsUtils.FinanceConstants.MAX_DESCRIPTION_LENGTH + " characters"
    )
    String description;

    @Size(
        max = ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE,
        message = "Metadata must not contain more than " + ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE + " entries"
    )
    Map<String, Object> metadata;
}
