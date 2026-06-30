package com.github.dghng36.eauction.modules.finance.transactions.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.modules.finance.transactions.dto.request.CreateTransactionRequest;
import com.github.dghng36.eauction.modules.finance.transactions.mapper.TransactionMapper;
import com.github.dghng36.eauction.modules.finance.transactions.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class InternalTransactionService {
    TransactionRepository transactionRepo;

    TransactionMapper transactionMapper;

    @Transactional
    public void createTransaction(
        String userId,
        CreateTransactionRequest createTransactionRequest
    ) {

        transactionRepo.save(transactionMapper.toTransactionEntity(userId, createTransactionRequest));

    }
}
