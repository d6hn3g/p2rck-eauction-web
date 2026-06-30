package com.github.dghng36.eauction.modules.finance.transactions.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class TransactionAdminResponse extends TransactionResponse {
    String walletId;
    
    String userId;

    String username;

    String fullName;

    Instant updatedAt;
}
