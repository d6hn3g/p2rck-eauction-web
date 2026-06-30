package com.github.dghng36.eauction.e_auction_system.unit.modules.finance.wallet.service;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import org.bson.Document;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.CreditUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.DebitUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.mapper.WalletMapper;
import com.github.dghng36.eauction.modules.finance.wallet.model.Wallet;
import com.github.dghng36.eauction.modules.finance.wallet.repository.WalletRepository;
import com.github.dghng36.eauction.modules.finance.wallet.service.WalletService;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private WalletRepository walletRepo;
    @Mock private WalletMapper walletMapper;

    @InjectMocks private WalletService walletService;

    private final String userId = "user-wallet-test-123";
    private Wallet mockWallet;

    @BeforeEach
    void setUp() {
        mockWallet = Wallet.builder()
            .id("wallet-id-456")
            .userId(userId)
            .availableBalance(new BigDecimal("1000.00"))
            .holdBalance(BigDecimal.ZERO)
            .isDeleted(false)
            .build();
    }

    /**
     * Test cases for creditUserWallet
     */

    @Test
    void creditUserWallet_Success() {
        // Arrange
        CreditUserWalletRequest request = CreditUserWalletRequest.builder()
            .amount(new BigDecimal("100.00"))
            .build();

        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        final Update[] capturedUpdateHolder = new Update[1];
        final Query[] capturedQueryHolder = new Query[1];

        when(mongoTemplate.updateFirst(any(Query.class), any(), eq(Wallet.class)))
            .thenAnswer(invocation -> {
                capturedQueryHolder[0] = invocation.getArgument(0, Query.class);
                capturedUpdateHolder[0] = invocation.getArgument(1, Update.class);
                return mockResult;
            });

        // Act
        walletService.creditUserWallet(userId, request);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(Wallet.class));
        
        assertNotNull(capturedQueryHolder[0]);
        String queryJson = capturedQueryHolder[0].getQueryObject().toJson();
        assertThat(queryJson).contains(userId).contains("\"isDeleted\": false");

        assertNotNull(capturedUpdateHolder[0]);
        UpdateDefinition updateDef = capturedUpdateHolder[0];
        Document updateDoc = updateDef.getUpdateObject();

        assertTrue(updateDoc.containsKey("$inc"));
        Document incFields = (Document) updateDoc.get("$inc");
        assertEquals(new BigDecimal("100.00"), incFields.get("availableBalance"));
    }

    @Test
    void creditUserWallet_WalletNotFound() {
        // Arrange
        CreditUserWalletRequest request = CreditUserWalletRequest.builder()
            .amount(new BigDecimal("100.00"))
            .build();

        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(), any(), eq(Wallet.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            walletService.creditUserWallet(userId, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Wallet not found for user: " + userId, ex.getMessage());
    }

    @Test
    void creditUserWallet_NegativeAmount() {
        // Arrange
        CreditUserWalletRequest request = CreditUserWalletRequest.builder()
            .amount(new BigDecimal("-10.00"))
            .build();

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            walletService.creditUserWallet(userId, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Credit amount must be greater than zero", ex.getMessage());
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(Wallet.class));
    }

    /**
     * Test cases for debitUserWallet
     */

    @Test
    void debitUserWallet_Success() {
        // Arrange
        DebitUserWalletRequest request = DebitUserWalletRequest.builder()
            .amount(new BigDecimal("200.00"))
            .build();

        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        final Update[] capturedUpdateHolder = new Update[1];
        final Query[] capturedQueryHolder = new Query[1];

        when(mongoTemplate.updateFirst(any(Query.class), any(), eq(Wallet.class)))
            .thenAnswer(invocation -> {
                capturedQueryHolder[0] = invocation.getArgument(0, Query.class);
                capturedUpdateHolder[0] = invocation.getArgument(1, Update.class);
                return mockResult;
            });

        // Act
        walletService.debitUserWallet(userId, request);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(Wallet.class));
        
        assertNotNull(capturedQueryHolder[0]);
        String queryJson = capturedQueryHolder[0].getQueryObject().toJson();
        assertThat(queryJson).contains(userId).contains("availableBalance").contains("\"$gte\"");

        assertNotNull(capturedUpdateHolder[0]);
        UpdateDefinition updateDef = capturedUpdateHolder[0];
        Document debitUpdateDoc = updateDef.getUpdateObject();

        assertTrue(debitUpdateDoc.containsKey("$inc"));
        Document debitIncFields = (Document) debitUpdateDoc.get("$inc");
        assertEquals(new BigDecimal("-200.00"), debitIncFields.get("availableBalance"));
    }

    @Test
    void debitUserWallet_WalletNotFound() {
        // Arrange
        DebitUserWalletRequest request = DebitUserWalletRequest.builder()
            .amount(new BigDecimal("200.00"))
            .build();

        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(), any(), eq(Wallet.class)))
            .thenReturn(mockResult);
        
        when(mongoTemplate.exists(any(Query.class), eq(Wallet.class))).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            walletService.debitUserWallet(userId, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Wallet not found for userId: " + userId, ex.getMessage());
    }

    @Test
    void debitUserWallet_InsufficientBalance() {
        // Arrange
        DebitUserWalletRequest request = DebitUserWalletRequest.builder()
            .amount(new BigDecimal("200.00"))
            .build();

        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(), any(), eq(Wallet.class)))
            .thenReturn(mockResult);
        
        when(mongoTemplate.exists(any(Query.class), eq(Wallet.class))).thenReturn(true);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            walletService.debitUserWallet(userId, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Insufficient balance in wallet", ex.getMessage());
    }
}
