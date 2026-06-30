package com.github.dghng36.eauction.modules.finance.bankAccount.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.internal.BankAccountInfo;
import com.github.dghng36.eauction.modules.finance.bankAccount.mapper.BankAccountInfoMapper;
import com.github.dghng36.eauction.modules.finance.bankAccount.model.BankAccount;
import com.github.dghng36.eauction.modules.finance.bankAccount.repository.BankAccountRepository;
import com.github.dghng36.eauction.modules.finance.enums.BankAccountStatus;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class InternalBankAccountService {
    BankAccountRepository bankAccountRepo;

    BankAccountInfoMapper bankAccountInfoMapper;

    public BankAccountInfo getBankAccountInfo(String bankAccountId) {
        return bankAccountRepo.findByIdAndIsDeletedFalse(bankAccountId)
            .map(bankAccountInfoMapper::toBankAccountInfo)
            .orElse(null);
    }

    public void validateBankAccountVerified(String bankAccountId) {
        BankAccount bankAccount = bankAccountRepo.findByIdAndIsDeletedFalse(bankAccountId)
            .orElseThrow(() -> new AppException("Bank account not found", HttpStatus.NOT_FOUND));

        if (!bankAccount.getStatus().equals(BankAccountStatus.VERIFIED)) {
            throw new AppException("Bank account is not verified", HttpStatus.BAD_REQUEST);
        }
    }
}
