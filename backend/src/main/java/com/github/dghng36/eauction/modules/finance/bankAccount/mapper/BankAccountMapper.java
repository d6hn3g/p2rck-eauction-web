package com.github.dghng36.eauction.modules.finance.bankAccount.mapper;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.finance.bankAccount.dto.request.CreateBankAccountRequest;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.response.BankAccountResponse;
import com.github.dghng36.eauction.modules.finance.bankAccount.model.BankAccount;
import com.github.dghng36.eauction.modules.finance.enums.BankAccountStatus;
import com.github.dghng36.eauction.modules.finance.helper.BankAccountMaskedHelper;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;

@Component
public class BankAccountMapper {
    public BankAccount toBankAccountEntity(String userId, CreateBankAccountRequest createBankAccountRequest) {
        return BankAccount.builder()
            .userId(userId)
            .bankName(createBankAccountRequest.getBankName())
            .bankCode(createBankAccountRequest.getBankCode())
            .accountNumber(createBankAccountRequest.getAccountNumber())
            .accountHolderName(createBankAccountRequest.getAccountHolderName())
            .isDefault(false)
            .status(BankAccountStatus.PENDING_VERIFIED)
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }

    public BankAccountResponse toBankAccountResponse(BankAccount bankAccount, UserInfo userInfo) {
        if (bankAccount == null || userInfo == null) {
            return null;
        }

        return BankAccountResponse.builder()
            .bankAccountId(bankAccount.getId())
            .userId(bankAccount.getUserId())
            .username(userInfo.getUsername())
            .bankCode(bankAccount.getBankCode())
            .bankName(bankAccount.getBankName())
            .accountNumberMasked(BankAccountMaskedHelper.maskAccountNumber(bankAccount.getAccountNumber()))
            .accountHolderName(bankAccount.getAccountHolderName())
            .isDefault(bankAccount.getIsDefault())
            .bankAccountStatus(bankAccount.getStatus().name())
            .verifiedAt(bankAccount.getVerifiedAt())
            .build();
    }

    public List<BankAccountResponse> toBankAccountResponseList(List<BankAccount> bankAccounts, Map<String, UserInfo> userInfoMap) {
        return bankAccounts.stream()
            .map(bankAccount -> toBankAccountResponse(bankAccount, userInfoMap.get(bankAccount.getUserId())))
            .toList();
    }
}
