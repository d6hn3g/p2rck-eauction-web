package com.github.dghng36.eauction.modules.finance.bankAccount.mapper;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.finance.bankAccount.dto.internal.BankAccountInfo;
import com.github.dghng36.eauction.modules.finance.bankAccount.model.BankAccount;

@Component
public class BankAccountInfoMapper {
    public BankAccountInfo toBankAccountInfo(BankAccount bankAccount) {
        if (bankAccount == null) {
            return null;
        }

        return BankAccountInfo.builder()
            .userId(bankAccount.getUserId())
            .bankAccountId(bankAccount.getId())
            .bankCode(bankAccount.getBankCode())
            .bankName(bankAccount.getBankName())
            .accountNumber(bankAccount.getAccountNumber())
            .accountHolderName(bankAccount.getAccountHolderName())
            .build();
    } 
}
