package com.github.dghng36.eauction.modules.finance.bankAccount.dto.response;

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
public class BankAccountResponse {
    String bankAccountId;

    String bankCode;

    String bankName;

    String accountNumberMasked;

    String accountHolderName;

    String userId;

    String username;

    Boolean isDefault;

    String bankAccountStatus;

    Instant verifiedAt;
}
