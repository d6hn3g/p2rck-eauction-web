package com.github.dghng36.eauction.modules.finance.bankAccount.model;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.finance.enums.BankAccountStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "bank_accounts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class BankAccount extends BaseEntity {
    @Indexed(unique = true)
    String userId;

    String bankCode;

    String bankName;

    String accountNumber;

    String accountHolderName;

    Boolean isDefault;

    BankAccountStatus status;

    Instant verifiedAt;
}
