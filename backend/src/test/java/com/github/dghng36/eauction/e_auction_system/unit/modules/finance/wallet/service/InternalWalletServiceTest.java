package com.github.dghng36.eauction.e_auction_system.unit.modules.finance.wallet.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.finance.transactions.dto.request.CreateTransactionRequest;
import com.github.dghng36.eauction.modules.finance.transactions.service.InternalTransactionService;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.DepositUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.HoldBalanceUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.RefundUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.WithdrawUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.mapper.WalletMapper;
import com.github.dghng36.eauction.modules.finance.wallet.model.Wallet;
import com.github.dghng36.eauction.modules.finance.wallet.repository.WalletRepository;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class InternalWalletServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private WalletRepository walletRepo;
    @Mock private InternalTransactionService internalTransactionService;
    @Mock private WalletMapper walletMapper;

    @InjectMocks private InternalWalletService internalWalletService;

    private final String userId = "user-wallet-test-123";
    private final String walletId = "wallet-id-456";

    private Wallet mockWallet;
    private Wallet mockWalletWithHold;
    private WithdrawUserWalletRequest withdrawRequest;
    private HoldBalanceUserWalletRequest holdBalanceRequest;
    private DepositUserWalletRequest depositRequest;
    private RefundUserWalletRequest refundRequest;

    @BeforeEach
    void setUp() {
        mockWallet = Wallet.builder()
            .id(walletId)
            .userId(userId)
            .availableBalance(new BigDecimal("1000.00"))
            .holdBalance(new BigDecimal("100.00"))
            .isDeleted(false)
            .build();

        mockWalletWithHold = Wallet.builder()
            .id(walletId)
            .userId(userId)
            .availableBalance(new BigDecimal("900.00"))
            .holdBalance(new BigDecimal("100.00"))
            .isDeleted(false)
            .build();

        withdrawRequest = WithdrawUserWalletRequest.builder()
            .paymentId("payment-withdraw-123")
            .description("Withdraw success test")
            .withdrawAmount(new BigDecimal("50.00"))
            .build();

        holdBalanceRequest = HoldBalanceUserWalletRequest.builder()
            .paymentId("payment-hold-123")
            .description("Hold balance test")
            .holdAmount(new BigDecimal("50.00"))
            .build();

        depositRequest = DepositUserWalletRequest.builder()
            .paymentId("payment-deposit-123")
            .description("Deposit success test")
            .depositAmount(new BigDecimal("200.00"))
            .build();

        refundRequest = RefundUserWalletRequest.builder()
            .paymentId("payment-refund-123")
            .description("Refund test")
            .refundAmount(new BigDecimal("30.00"))
            .build();
    }

    /**
     * Test cases for holdBalance
     * Tests:
     * - holdBalance_Success
     * - holdBalance_InsufficientFunds
     * - holdBalance_WalletNotFound
     */

    @Test
    void holdBalance_Success() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(mockWallet);

        // Act
        internalWalletService.holdBalance(userId, 50.0);

        // Assert
        verify(mongoTemplate, times(1)).findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        );
    }

    @Test
    void holdBalance_InsufficientFunds() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(null);
        when(mongoTemplate.exists(any(Query.class), eq(Wallet.class))).thenReturn(true);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            internalWalletService.holdBalance(userId, 50.0));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Insufficient available balance for userId: " + userId, ex.getMessage());
    }

    @Test
    void holdBalance_WalletNotFound() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(null);
        when(mongoTemplate.exists(any(Query.class), eq(Wallet.class))).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            internalWalletService.holdBalance(userId, 50.0));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Wallet not found for userId: " + userId, ex.getMessage());
    }

     /**
     * Test cases for unHoldBalance
     * Tests:
     * - unHoldBalance_Success
     * - unHoldBalance_HoldNotFound
     * - unHoldBalance_WalletNotFound
     */

    @Test
    void unHoldBalance_Success() {
        // Arrange
        UpdateResult mockUpdateResult = Mockito.mock(UpdateResult.class);
        when(mockUpdateResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(
            any(Query.class),
            any(Update.class),
            eq(Wallet.class)
        )).thenReturn(mockUpdateResult);

        // Act
        internalWalletService.unHoldBalance(userId, 50.0);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(
            any(Query.class),
            any(Update.class),
            eq(Wallet.class)
        );
    }

    @Test
    void unHoldBalance_HoldNotFound() {
        // Arrange
        UpdateResult mockUpdateResult = Mockito.mock(UpdateResult.class);
        when(mockUpdateResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(
            any(Query.class),
            any(Update.class),
            eq(Wallet.class)
        )).thenReturn(mockUpdateResult);
        when(mongoTemplate.exists(any(Query.class), eq(Wallet.class))).thenReturn(true);

        // Act & Assert (no explicit exception message check as implementation doesn't throw for this case)
        internalWalletService.unHoldBalance(userId, 50.0);
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(Wallet.class));
    }

    @Test
    void unHoldBalance_WalletNotFound() {
        // Arrange
        UpdateResult mockUpdateResult = Mockito.mock(UpdateResult.class);
        when(mockUpdateResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(
            any(Query.class),
            any(Update.class),
            eq(Wallet.class)
        )).thenReturn(mockUpdateResult);
        when(mongoTemplate.exists(any(Query.class), eq(Wallet.class))).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            internalWalletService.unHoldBalance(userId, 50.0));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Wallet not found for userId: " + userId, ex.getMessage());
    }

    /**
     * Test cases for handleWithdrawSuccessUserWallet
     * Tests:
     * - handleWithdrawSuccessUserWallet_Success
     * - handleWithdrawSuccessUserWallet_WalletNotFound
     */
    @Test
    void handleWithdrawSuccessUserWallet_Success() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(mockWallet);

        // Act
        internalWalletService.handleWithdrawSuccessUserWallet(userId, withdrawRequest);

        // Assert
        verify(mongoTemplate, times(1)).findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        );
        verify(internalTransactionService, times(1)).createTransaction(
            eq(userId),
            any(CreateTransactionRequest.class)
        );
    }

    @Test
    void handleWithdrawSuccessUserWallet_WalletNotFound() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(null);
        when(walletRepo.existsByUserIdAndIsDeletedFalse(userId)).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            internalWalletService.handleWithdrawSuccessUserWallet(userId, withdrawRequest));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Wallet not found for userId: " + userId, ex.getMessage());
        verify(internalTransactionService, never()).createTransaction(eq(userId), any(CreateTransactionRequest.class));
    }

    /**
     * Test cases for handleHoldBalanceUserWallet
     * Tests:
     * - handleHoldBalanceUserWallet_Success
     * - handleHoldBalanceUserWallet_WalletNotFound
     */

    @Test
    void handleHoldBalanceUserWallet_Success() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(mockWallet);

        // Act
        internalWalletService.handleHoldBalanceUserWallet(userId, holdBalanceRequest);

        // Assert
        verify(mongoTemplate, times(1)).findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        );
        verify(internalTransactionService, times(1)).createTransaction(
            eq(userId),
            any(CreateTransactionRequest.class)
        );
    }

    @Test
    void handleHoldBalanceUserWallet_WalletNotFound() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(null);
        when(mongoTemplate.exists(any(Query.class), eq(Wallet.class))).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            internalWalletService.handleHoldBalanceUserWallet(userId, holdBalanceRequest));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Wallet not found for userId: " + userId, ex.getMessage());
        verify(internalTransactionService, never()).createTransaction(eq(userId), any(CreateTransactionRequest.class));
    }

    /**
     * Test cases for handleDepositSuccessUserWallet
     * Tests:
     * - handleDepositSuccessUserWallet_Success
     * - handleDepositSuccessUserWallet_WalletNotFound
     */

    @Test
    void handleDepositSuccessUserWallet_Success() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(mockWallet);

        // Act
        internalWalletService.handleDepositSuccessUserWallet(userId, depositRequest);

        // Assert
        verify(mongoTemplate, times(1)).findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        );
        verify(internalTransactionService, times(1)).createTransaction(
            eq(userId),
            any(CreateTransactionRequest.class)
        );
    }

    @Test
    void handleDepositSuccessUserWallet_WalletNotFound() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(null);
        when(walletRepo.existsByUserIdAndIsDeletedFalse(userId)).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            internalWalletService.handleDepositSuccessUserWallet(userId, depositRequest));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Wallet not found for userId: " + userId, ex.getMessage());
        verify(internalTransactionService, never()).createTransaction(eq(userId), any(CreateTransactionRequest.class));
    }

    /**
     * Test cases for handleRefundUserWallet
     * Tests:
     * - handleRefundUserWallet_Success
     * - handleRefundUserWallet_WalletNotFound
     */

    @Test
    void handleRefundUserWallet_Success() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(mockWallet);

        // Act
        internalWalletService.handleRefundUserWallet(userId, refundRequest);

        // Assert
        verify(mongoTemplate, times(1)).findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        );
        verify(internalTransactionService, times(1)).createTransaction(
            eq(userId),
            any(CreateTransactionRequest.class)
        );
    }

    @Test
    void handleRefundUserWallet_WalletNotFound() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class),
            any(Update.class),
            any(FindAndModifyOptions.class),
            eq(Wallet.class)
        )).thenReturn(null);
        when(walletRepo.existsByUserIdAndIsDeletedFalse(userId)).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            internalWalletService.handleRefundUserWallet(userId, refundRequest));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Wallet not found for userId: " + userId, ex.getMessage());
        verify(internalTransactionService, never()).createTransaction(eq(userId), any(CreateTransactionRequest.class));
    }
}
