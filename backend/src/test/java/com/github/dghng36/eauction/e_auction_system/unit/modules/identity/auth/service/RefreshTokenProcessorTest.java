package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.auth.service;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.auth.model.RefreshToken;
import com.github.dghng36.eauction.modules.identity.auth.repository.RefreshTokenRepository;
import com.github.dghng36.eauction.modules.identity.auth.service.RefreshTokenProcessor;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenProcessorTest {
    @Mock private RefreshTokenRepository refreshTokenRepo;
    @InjectMocks private RefreshTokenProcessor refreshTokenProcessor;

    private RefreshToken mockRefreshToken;
    private final String targetTokenStr = "valid-refresh-token";

    @BeforeEach
    void setUp() {
        mockRefreshToken = RefreshToken.builder()
            .userId("user123")
            .refreshTokenStr("valid-refresh-token")
            .expiryTime(Instant.now().plusSeconds(3600)) // Expires in 1 hour
            .build();
    }

    /**
     * Test cases isRefreshTokenExpired
     * Tests:
     * - isRefreshTokenExpired_ValidToken_ShouldNotThrowException
     * - isRefreshTokenExpired_TokenNotFound_ShouldThrowNotFoundException
     * - isRefreshTokenExpired_TokenExpired_ShouldThrowUnauthorizedException
     */

    @Test
    void isRefreshTokenExpired_ValidToken_ShouldNotThrowException() {
        // Arrange
        when(refreshTokenRepo.findByRefreshTokenStr(targetTokenStr)).thenReturn(Optional.of(mockRefreshToken));

        // Act & Assert
        assertDoesNotThrow(() -> refreshTokenProcessor.isRefreshTokenExpired(targetTokenStr));
        verify(refreshTokenRepo, times(1)).findByRefreshTokenStr(targetTokenStr);
    }

    @Test
    void isRefreshTokenExpired_TokenNotFound_ShouldThrowNotFoundException() {
        // Arrange
        when(refreshTokenRepo.findByRefreshTokenStr(targetTokenStr)).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> refreshTokenProcessor.isRefreshTokenExpired(targetTokenStr));
        assertEquals("Refresh token not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void isRefreshTokenExpired_TokenExpired_ShouldThrowUnauthorizedException() {
        // Arrange
        mockRefreshToken.setExpiryTime(Instant.now().minusSeconds(600)); 
        when(refreshTokenRepo.findByRefreshTokenStr(targetTokenStr)).thenReturn(Optional.of(mockRefreshToken));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> 
            refreshTokenProcessor.isRefreshTokenExpired(targetTokenStr)
        );
        assertEquals("Refresh token expired", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    /**
     * Test cases invalidateRefreshToken
     * Tests:
     * - invalidateRefreshToken_Success_ShouldDeleteAndReturnToken
     * - invalidateRefreshToken_TokenNotFound_ShouldThrowNotFoundException
     */

    @Test
    void invalidateRefreshToken_Success_ShouldDeleteAndReturnToken() {
        // Arrange
        when(refreshTokenRepo.findByRefreshTokenStr(targetTokenStr)).thenReturn(Optional.of(mockRefreshToken));

        // Act
        RefreshToken result = refreshTokenProcessor.invalidateRefreshToken(targetTokenStr);

        // Assert
        assertNotNull(result);
        assertEquals(targetTokenStr, result.getRefreshTokenStr());
        verify(refreshTokenRepo, times(1)).delete(mockRefreshToken);
    }

    @Test
    void invalidateRefreshToken_TokenNotFound_ShouldThrowNotFoundException() {
        // Arrange
        when(refreshTokenRepo.findByRefreshTokenStr(targetTokenStr)).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> refreshTokenProcessor.invalidateRefreshToken(targetTokenStr));
        assertEquals("Refresh token not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());

        // Verify
        verify(refreshTokenRepo, never()).delete(mockRefreshToken);
    }

    /**
     * Test cases createRefreshToken
     * Tests:
     * - createRefreshToken_Success_ShouldSaveAndReturnToken
     */
    @Test
    void createRefreshToken_Success_ShouldSaveAndReturnToken() {
        // Arrange
        Instant expiry = Instant.now().plusSeconds(7200);
        
        when(refreshTokenRepo.save(any(RefreshToken.class))).thenReturn(mockRefreshToken);

        // Act
        RefreshToken result = refreshTokenProcessor.createRefreshToken("user123", targetTokenStr, expiry);

        // Assert
        assertNotNull(result);
        assertEquals(targetTokenStr, result.getRefreshTokenStr());

        // Verify
        verify(refreshTokenRepo, times(1)).save(any(RefreshToken.class));
    }
}
