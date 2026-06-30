package com.github.dghng36.eauction.modules.finance.wallet.service;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.CreditUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.DebitUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.response.WalletResponse;
import com.github.dghng36.eauction.modules.finance.wallet.mapper.WalletMapper;
import com.github.dghng36.eauction.modules.finance.wallet.model.Wallet;
import com.github.dghng36.eauction.modules.finance.wallet.repository.WalletRepository;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WalletService {
    MongoTemplate mongoTemplate;
    WalletRepository walletRepo;

    WalletMapper walletMapper;

    public WalletResponse getMyWallet(String userId) {
        // Find wallet by userId
        Wallet wallet = walletRepo.findByUserIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND));

        // Map to response
        return walletMapper.toWalletResponse(wallet);
    }

    public void creditUserWallet(String userId, CreditUserWalletRequest creditUserWalletRequest) {
        BigDecimal amount = creditUserWalletRequest.getAmount();
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid credit amount: [{}] for user: [{}]", amount, userId);
            throw new AppException("Credit amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        Query query = new Query(
            Criteria.where("userId").is(userId).and("isDeleted").is(false)
        );

        Update update = new Update().inc("availableBalance", amount);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Wallet.class);

        if (result.getMatchedCount() == 0) {
            throw new AppException("Wallet not found for user: " + userId, HttpStatus.NOT_FOUND);
        }

        log.info("Credited: [{}] to wallet for user: [{}]", amount, userId);
    }

    public void debitUserWallet(String userId, DebitUserWalletRequest debitUserWalletRequest) {
        BigDecimal amount = debitUserWalletRequest.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Debit amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        Query query = new Query(
            Criteria.where("userId").is(userId)
                    .and("isDeleted").is(false)
                    .and("availableBalance").gte(amount)
        );

        Update update = new Update().inc("availableBalance", amount.negate());

        UpdateResult result = mongoTemplate.updateFirst(query, update, Wallet.class);

        if (result.getMatchedCount() == 0) {
            boolean walletExists = mongoTemplate.exists(
                new Query(Criteria.where("userId").is(userId).and("isDeleted").is(false)), 
                Wallet.class
            );

            if (!walletExists) {
                throw new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND);
            }
            
            log.error("Insufficient balance for user: [{}] required: [{}]", userId, amount);
            throw new AppException("Insufficient balance in wallet", HttpStatus.BAD_REQUEST);
        }

        log.info("Debited: [{}] from wallet for userId: {}", amount, userId);
    }
}
