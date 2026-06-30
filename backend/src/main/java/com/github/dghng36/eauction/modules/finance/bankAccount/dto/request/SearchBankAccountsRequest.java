package com.github.dghng36.eauction.modules.finance.bankAccount.dto.request;

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
public class SearchBankAccountsRequest {
    String searchQuery;

    String bankAccountStatus;

    String bankName;

    Instant createdAtBefore;
    Instant createdAtAfter;

    Instant updatedAtBefore;
    Instant updatedAtAfter;

    Instant verifiedAtBefore;
    Instant verifiedAtAfter;
}
