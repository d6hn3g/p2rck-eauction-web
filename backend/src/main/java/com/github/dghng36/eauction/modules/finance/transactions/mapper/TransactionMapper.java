package com.github.dghng36.eauction.modules.finance.transactions.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.utils.MetadataUtils;
import com.github.dghng36.eauction.modules.finance.enums.TransactionStatus;
import com.github.dghng36.eauction.modules.finance.enums.TransactionType;
import com.github.dghng36.eauction.modules.finance.helper.TransactionCodeGeneratorHelper;
import com.github.dghng36.eauction.modules.finance.transactions.dto.request.CreateTransactionRequest;
import com.github.dghng36.eauction.modules.finance.transactions.dto.response.TransactionAdminResponse;
import com.github.dghng36.eauction.modules.finance.transactions.dto.response.TransactionResponse;
import com.github.dghng36.eauction.modules.finance.transactions.model.Transaction;

@Component
public class TransactionMapper {
    public Transaction toTransactionEntity(
        String userId,
        CreateTransactionRequest createTransactionRequest
    ) {
        if (userId == null || createTransactionRequest == null) {
            return null;
        }

        TransactionType transactionType = TransactionType.fromString(createTransactionRequest.getTransactionType())
            .orElse(null);

        return Transaction.builder()
            .transactionCode(TransactionCodeGeneratorHelper.generateTransactionCode())
            .userId(userId)
            .type(transactionType)
            .status(TransactionStatus.PENDING)
            .amount(createTransactionRequest.getAmount())
            .availableBefore(createTransactionRequest.getAvailableBefore())
            .availableAfter(createTransactionRequest.getAvailableAfter())
            .holdBefore(createTransactionRequest.getHoldBefore())
            .holdAfter(createTransactionRequest.getHoldAfter())
            .referenceId(createTransactionRequest.getReferenceId())
            .description(createTransactionRequest.getDescription())
            .metadata(MetadataUtils.sanitizeDynamicMetadata(createTransactionRequest.getMetadata()))
            .build();
    }

    public TransactionResponse toTransactionResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        BigDecimal holdBefore = transaction.getHoldBefore() == null 
            ? BigDecimal.ZERO : transaction.getHoldBefore();
        BigDecimal holdAfter = transaction.getHoldAfter() == null 
            ? BigDecimal.ZERO : transaction.getHoldAfter();

        return TransactionResponse.builder()
            .id(transaction.getId())
            .transactionCode(transaction.getTransactionCode())
            .transactionType(transaction.getType().name())
            .transactionStatus(transaction.getStatus().name())
            .amount(transaction.getAmount())
            .availableBefore(transaction.getAvailableBefore())
            .availableAfter(transaction.getAvailableAfter())
            .holdBefore(holdBefore)
            .holdAfter(holdAfter)
            .totalBefore(transaction.getAvailableBefore().add(holdBefore))
            .totalAfter(transaction.getAvailableAfter().add(holdAfter))
            .referenceId(transaction.getReferenceId())
            .description(transaction.getDescription())
            .metadata(transaction.getMetadata())
            .build();
    }

    public List<TransactionResponse> toTransactionResponseList(List<Transaction> transactions) {
        if (transactions == null) {
            return List.of();
        }
        
        return transactions.stream()
            .map(this::toTransactionResponse)
            .toList();
    }

    public TransactionAdminResponse toTransactionAdminResponse(Transaction transaction, Map<String, String> userInfo) {
        if (transaction == null) {
            return null;
        }

        BigDecimal holdBefore = transaction.getHoldBefore() == null 
            ? BigDecimal.ZERO : transaction.getHoldBefore();
        BigDecimal holdAfter = transaction.getHoldAfter() == null 
            ? BigDecimal.ZERO : transaction.getHoldAfter();

        return TransactionAdminResponse.builder()
            .id(transaction.getId())
            .transactionCode(transaction.getTransactionCode())
            .transactionType(transaction.getType().name())
            .transactionStatus(transaction.getStatus().name())
            .amount(transaction.getAmount())
            .availableBefore(transaction.getAvailableBefore())
            .availableAfter(transaction.getAvailableAfter())
            .holdBefore(holdBefore)
            .holdAfter(holdAfter)
            .totalBefore(transaction.getAvailableBefore().add(holdBefore))
            .totalAfter(transaction.getAvailableAfter().add(holdAfter))
            .referenceId(transaction.getReferenceId())
            .description(transaction.getDescription())
            .metadata(transaction.getMetadata())
            .createdAt(transaction.getCreatedAt())
            .walletId(transaction.getWalletId())
            .userId(transaction.getUserId())
            .username(userInfo != null ? userInfo.get("username") : "N/A")
            .fullName(userInfo != null ? userInfo.get("fullName") : "N/A")
            .updatedAt(transaction.getUpdatedAt())
            .build();
    }

    public List<TransactionAdminResponse> toTransactionAdminResponseList(List<Transaction> transactions, Map<String, Map<String, String>> userIdToUsernameAndFullName) {
        if (transactions == null || userIdToUsernameAndFullName == null) {
            return List.of();
        }
        
        return transactions.stream()
            .map(transaction -> {
                return toTransactionAdminResponse(transaction, userIdToUsernameAndFullName.get(transaction.getUserId()));
            })
            .toList();
    }
}
