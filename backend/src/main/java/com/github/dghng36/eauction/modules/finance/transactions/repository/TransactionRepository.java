package com.github.dghng36.eauction.modules.finance.transactions.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.finance.transactions.model.Transaction;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    Page<Transaction> findAllByUserId(String userId, Pageable pageable);
}
