package com.github.dghng36.eauction.modules.finance.wallet.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.finance.wallet.model.Wallet;

public interface WalletRepository extends MongoRepository<Wallet, String> {
    Optional<Wallet> findByUserIdAndIsDeletedFalse(String userId);

    boolean existsByUserIdAndIsDeletedFalse(String userId);
}
