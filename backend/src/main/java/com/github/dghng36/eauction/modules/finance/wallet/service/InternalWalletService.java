package com.github.dghng36.eauction.modules.finance.wallet.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.finance.enums.TransactionType;
import com.github.dghng36.eauction.modules.finance.transactions.dto.request.CreateTransactionRequest;
import com.github.dghng36.eauction.modules.finance.transactions.service.InternalTransactionService;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.DepositUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.HoldBalanceUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.RefundUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.WithdrawUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.mapper.WalletMapper;
import com.github.dghng36.eauction.modules.finance.wallet.model.Wallet;
import com.github.dghng36.eauction.modules.finance.wallet.repository.WalletRepository;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InternalWalletService {
    MongoTemplate mongoTemplate;
    WalletRepository walletRepo;

    InternalTransactionService internalTransactionService;

    WalletMapper walletMapper;

    public void createUserWallet(String userId) {
        try {
            Wallet walletEntity = walletMapper.toWalletEntity(userId);
            
            mongoTemplate.insert(walletEntity);
            
            log.info("Wallet created successfully for userId: [{}]", userId);
        } catch (DuplicateKeyException ex) {
            log.warn("Wallet already exists for userId: [{}] (Caught concurrent creation request)", userId);
        }
    }

    public boolean validateAvailableBalance(String userId, Double requiredAmount) {
       if (requiredAmount == null || requiredAmount <= 0) {
            return false;
        }

        Query existQuery = new Query().addCriteria(
            Criteria.where("userId").is(userId)
                    .and("isDeleted").is(false)
        );
        if (!mongoTemplate.exists(existQuery, Wallet.class)) {
            throw new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND);
        }

        Query balanceQuery = new Query().addCriteria(
            Criteria.where("userId").is(userId)
                    .and("isDeleted").is(false)
                    .and("availableBalance").gte(BigDecimal.valueOf(requiredAmount))
        );

        return mongoTemplate.exists(balanceQuery, Wallet.class);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void holdBalance(String userId, Double holdAmount) {
        if (holdAmount == null || holdAmount <= 0) {
            throw new AppException("Hold amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        Query query = new Query(Criteria.where("userId").is(userId)
            .and("availableBalance").gte(holdAmount)
            .and("isDeleted").is(false)
        );

        Update update = new Update()
            .inc("availableBalance", BigDecimal.valueOf(holdAmount).negate())
            .inc("holdBalance", BigDecimal.valueOf(holdAmount));

        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(false);
        Wallet oldWallet = mongoTemplate.findAndModify(query, update, options, Wallet.class);

        if (oldWallet == null) {
            boolean walletExists = mongoTemplate.exists(new Query(Criteria.where("userId").is(userId)), Wallet.class);
            if (!walletExists) {
                throw new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND);
            } else {
                throw new AppException("Insufficient available balance for userId: " + userId, HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unHoldBalance(String userId, Double unHoldBidAmount) {
        Query query = new Query().addCriteria(
            Criteria.where("userId").is(userId)
                .and("isDeleted").is(false)
                .and("holdBalance").gt(BigDecimal.ZERO)
        );

        Update update = new Update()
            .inc("availableBalance", BigDecimal.valueOf(unHoldBidAmount))
            .inc("holdBalance", BigDecimal.valueOf(unHoldBidAmount).negate());

        UpdateResult result = mongoTemplate.updateFirst(query, update, Wallet.class);

        if (result.getMatchedCount() == 0) {
            boolean isWalletExist = mongoTemplate.exists(
                new Query().addCriteria(Criteria.where("userId").is(userId).and("isDeleted").is(false)), 
                Wallet.class
            );
            if (!isWalletExist) {
                throw new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND);
            }
        }
    }

    public void deleteUserWallet(String userId) {
        Query query = new Query(
            Criteria.where("userId").is(userId).and("isDeleted").is(false)
        );

        Update update = new Update()
            .set("isDeleted", true)
            .set("deletedAt", Instant.now());

        UpdateResult result = mongoTemplate.updateFirst(query, update, Wallet.class);

        if (result.getMatchedCount() == 0) {
            log.warn("Attempted to delete a non-existent or already deleted wallet for user: [{}]", userId);
            throw new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND);
        }

        log.info("Wallet for user: [{}] has been successfully soft-deleted", userId);
    }

    public void handleWithdrawSuccessUserWallet(
        String userId,
        WithdrawUserWalletRequest withdrawUserWalletRequest
    ) {
        BigDecimal withdrawAmount = withdrawUserWalletRequest.getWithdrawAmount();

        if (withdrawAmount == null || withdrawAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Withdraw amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        Query query = new Query(Criteria.where("userId").is(userId)
            .and("holdBalance").gte(withdrawAmount)
            .and("isDeleted").is(false)
        );

        Update update = new Update()
            .inc("holdBalance", withdrawAmount.negate());

        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(false);
        Wallet oldWallet = mongoTemplate.findAndModify(query, update, options, Wallet.class);

        if (oldWallet == null) {
            if (!walletRepo.existsByUserIdAndIsDeletedFalse(userId)) {
                throw new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND);
            } else {
                throw new AppException("Insufficient hold balance for userId: " + userId, HttpStatus.BAD_REQUEST);
            }
        }

        BigDecimal availableBefore = oldWallet.getAvailableBalance();
        BigDecimal availableAfter = oldWallet.getAvailableBalance();

        BigDecimal holdBefore = oldWallet.getHoldBalance();
        BigDecimal holdAfter = oldWallet.getHoldBalance().subtract(withdrawAmount);

        internalTransactionService.createTransaction(
            userId,
            CreateTransactionRequest.builder()
                .walletId(oldWallet.getId())
                .transactionType(TransactionType.WITHDRAW_SUCCESS.name())
                .amount(withdrawUserWalletRequest.getWithdrawAmount())
                .availableBefore(availableBefore)
                .availableAfter(availableAfter)
                .holdBefore(holdBefore)
                .holdAfter(holdAfter)
                .referenceId(withdrawUserWalletRequest.getPaymentId())
                .description(withdrawUserWalletRequest.getDescription())
                .metadata(withdrawUserWalletRequest.getMetadata())
                .build()
        );
        
    }

    public void handleHoldBalanceUserWallet(
        String userId,
        HoldBalanceUserWalletRequest holdBalanceUserWalletRequest
    ) {
        BigDecimal holdAmount = holdBalanceUserWalletRequest.getHoldAmount();

        if (holdAmount == null || holdAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        Query query = new Query(Criteria.where("userId").is(userId)
            .and("availableBalance").gte(holdAmount)
            .and("isDeleted").is(false)
        );

        Update update = new Update()
            .inc("availableBalance", holdAmount.negate())
            .inc("holdBalance", holdAmount);

        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(false);
        Wallet oldWallet = mongoTemplate.findAndModify(query, update, options, Wallet.class);

        if (oldWallet == null) {
            boolean walletExists = mongoTemplate.exists(new Query(Criteria.where("userId").is(userId)), Wallet.class);
            if (!walletExists) {
                throw new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND);
            } else {
                throw new AppException("Insufficient available balance for userId: " + userId, HttpStatus.BAD_REQUEST);
            }
        }

        BigDecimal availableBefore = oldWallet.getAvailableBalance();
        BigDecimal availableAfter = oldWallet.getAvailableBalance().subtract(holdAmount);

        BigDecimal holdBefore = oldWallet.getHoldBalance();
        BigDecimal holdAfter = oldWallet.getHoldBalance().add(holdAmount);

        internalTransactionService.createTransaction(
            userId,
            CreateTransactionRequest.builder()
                .walletId(oldWallet.getId())
                .transactionType(TransactionType.WITHDRAW_HOLD.name())
                .amount(holdAmount)
                .availableBefore(availableBefore)
                .availableAfter(availableAfter)
                .holdBefore(holdBefore)
                .holdAfter(holdAfter)
                .referenceId(holdBalanceUserWalletRequest.getPaymentId())
                .description(holdBalanceUserWalletRequest.getDescription())
                .metadata(holdBalanceUserWalletRequest.getMetadata())
                .build()
        );
    }

    public void handleDepositSuccessUserWallet(
        String userId,
        DepositUserWalletRequest depositUserWalletRequest
    ) {
        BigDecimal depositAmount = depositUserWalletRequest.getDepositAmount();
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Deposit amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        Query query = new Query(Criteria.where("userId").is(userId)
            .and("isDeleted").is(false)
        );

        Update update = new Update()
            .inc("availableBalance", depositAmount);

        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(false);
        Wallet oldWallet = mongoTemplate.findAndModify(query, update, options, Wallet.class);
        
        if (oldWallet == null) {
            if (!walletRepo.existsByUserIdAndIsDeletedFalse(userId)) {
                throw new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND);
            } else {
                throw new AppException("Failed to update wallet for userId: " + userId, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        BigDecimal availableBefore = oldWallet.getAvailableBalance();
        BigDecimal availableAfter = oldWallet.getAvailableBalance().add(depositAmount);

        BigDecimal holdBefore = oldWallet.getHoldBalance();
        BigDecimal holdAfter = oldWallet.getHoldBalance();

        internalTransactionService.createTransaction(
            userId,
            CreateTransactionRequest.builder()
                .walletId(oldWallet.getId())
                .transactionType(TransactionType.DEPOSIT_SUCCESS.name())
                .amount(depositUserWalletRequest.getDepositAmount())
                .availableBefore(availableBefore)
                .availableAfter(availableAfter)
                .holdBefore(holdBefore)
                .holdAfter(holdAfter)
                .referenceId(depositUserWalletRequest.getPaymentId())
                .description(depositUserWalletRequest.getDescription())
                .metadata(depositUserWalletRequest.getMetadata())
                .build()
        );
    }

    public void handleRefundUserWallet(
        String userId,
        RefundUserWalletRequest refundUserWalletRequest
    ) {
        BigDecimal refundAmount = refundUserWalletRequest.getRefundAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Refund amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        Query query = new Query(Criteria.where("userId").is(userId)
            .and("holdBalance").gte(refundAmount)
            .and("isDeleted").is(false)
        );
        Update update = new Update()
            .inc("holdBalance", refundAmount.negate())
            .inc("availableBalance", refundAmount);

        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(false);
        Wallet oldWallet = mongoTemplate.findAndModify(query, update, options, Wallet.class);
        if (oldWallet == null) {
            if (!walletRepo.existsByUserIdAndIsDeletedFalse(userId)) {
                throw new AppException("Wallet not found for userId: " + userId, HttpStatus.NOT_FOUND);
            } else {
                throw new AppException("Insufficient hold balance for userId: " + userId, HttpStatus.BAD_REQUEST);
            }
        }

        BigDecimal availableBefore = oldWallet.getAvailableBalance();
        BigDecimal availableAfter = oldWallet.getAvailableBalance().add(refundAmount);

        BigDecimal holdBefore = oldWallet.getHoldBalance();
        BigDecimal holdAfter = oldWallet.getHoldBalance().subtract(refundAmount);
        

        internalTransactionService.createTransaction(
            userId,
            CreateTransactionRequest.builder()
                .walletId(oldWallet.getId())
                .transactionType(TransactionType.WITHDRAW_REFUND.name())
                .amount(refundUserWalletRequest.getRefundAmount())
                .availableBefore(availableBefore)
                .availableAfter(availableAfter)
                .holdBefore(holdBefore)
                .holdAfter(holdAfter)
                .referenceId(refundUserWalletRequest.getPaymentId())
                .description(refundUserWalletRequest.getDescription())
                .metadata(refundUserWalletRequest.getMetadata())
                .build()
        );
    }
}
