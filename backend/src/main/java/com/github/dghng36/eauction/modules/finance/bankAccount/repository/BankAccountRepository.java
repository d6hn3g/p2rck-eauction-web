package com.github.dghng36.eauction.modules.finance.bankAccount.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.finance.bankAccount.model.BankAccount;

public interface BankAccountRepository extends MongoRepository<BankAccount, String> {
    Page<BankAccount> findAllByUserIdAndIsDeletedFalse(String userId, Pageable pageable);

    Optional<BankAccount> findByIdAndUserIdAndIsDeletedFalse(String id, String userId);

    Page<BankAccount> findAllByIsDeletedFalse(Pageable pageable);

    Optional<BankAccount> findByIdAndIsDeletedFalse(String id);

    Optional<BankAccount> findByUserIdAndIsDefaultTrueAndIsDeletedFalse(String userId);
}
