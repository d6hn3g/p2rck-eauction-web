package com.github.dghng36.eauction.modules.finance.bankAccount.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateBankAccountRequest {
    @NotBlank(message = "Bank code is required")
    String bankCode;

    @NotBlank(message = "Bank name is required")
    String bankName;

    @NotBlank(message = "Account number is required")
    String accountNumber;

    @NotBlank(message = "Account holder name is required")
    String accountHolderName;
}
