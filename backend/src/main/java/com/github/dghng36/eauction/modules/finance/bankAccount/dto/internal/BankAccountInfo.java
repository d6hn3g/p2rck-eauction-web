package com.github.dghng36.eauction.modules.finance.bankAccount.dto.internal;

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
public class BankAccountInfo {
    String userId;
    
    String bankAccountId;

    String bankCode;

    String bankName;

    String accountNumber;

    String accountHolderName;
}
