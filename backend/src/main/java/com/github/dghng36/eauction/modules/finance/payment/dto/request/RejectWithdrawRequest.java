package com.github.dghng36.eauction.modules.finance.payment.dto.request;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.NotBlank;
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
public class RejectWithdrawRequest {
    @NotBlank(message = "Reject reason is required")
    @Size(
        max = ConstantsUtils.FinanceConstants.MAX_DESCRIPTION_LENGTH,
        message = "Reject reason must be at most " + ConstantsUtils.FinanceConstants.MAX_DESCRIPTION_LENGTH + " characters"
    )
    String rejectedReason;
}
