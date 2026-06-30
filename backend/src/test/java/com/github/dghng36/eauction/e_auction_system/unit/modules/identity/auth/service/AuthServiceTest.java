package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.auth.service;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.auth.dto.internal.AuthResult;
import com.github.dghng36.eauction.modules.identity.auth.dto.internal.JwtGeneratedResult;
import com.github.dghng36.eauction.modules.identity.auth.dto.request.LoginRequest;
import com.github.dghng36.eauction.modules.identity.auth.model.RefreshToken;
import com.github.dghng36.eauction.modules.identity.auth.service.AuthService;
import com.github.dghng36.eauction.modules.identity.auth.service.InvalidatedTokenProcessor;
import com.github.dghng36.eauction.modules.identity.auth.service.RefreshTokenProcessor;
import com.github.dghng36.eauction.modules.identity.auth.service.jwt.JwtAdapter;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.identity.enums.UserStatus;
import com.github.dghng36.eauction.modules.identity.helper.PasswordHelper;
import com.github.dghng36.eauction.modules.identity.reputation.service.ReputationProcessor;
import com.github.dghng36.eauction.modules.identity.user.model.User;
import com.github.dghng36.eauction.modules.identity.user.repository.UserRepository;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private UserRepository userRepo;
    @Mock private RefreshTokenProcessor refreshTokenProcessor;
    @Mock private InvalidatedTokenProcessor invalidatedTokenProcessor;
    @Mock private ReputationProcessor reputationProcessor;
    @Mock private PasswordHelper passwordHelper;
    @Mock private JwtAdapter jwtAdapter;

    @InjectMocks private AuthService authService;

    // Public mock data user
    private User mockUser;
    private LoginRequest mockLoginRequest1, mockLoginRequest2, mockLoginRequest3, mockLoginRequest4, mockLoginRequest5;
    private JwtGeneratedResult mockJwtGeneratedResult;

    @BeforeEach
    void setUp() {
        // Build mock user
        mockUser = User.builder()
            .id("user-test-auth-id")
            .username("testAuthUser")
            .passwordHash("testAuthPasswordHashed")
            .email("testAuthUser@test.com")
            .phoneNumber("0123456789")
            .status(UserStatus.VERIFIED)
            .role(UserRole.USER)
            .lastLoginAt(Instant.now().minusSeconds(86400 * 10))
            .build();

        // Build mock login request
        mockLoginRequest1 = LoginRequest.builder()
            .identifier("testAuthUser")
            .password("testAuthPasswordHashed")
            .build();

        mockLoginRequest2 = LoginRequest.builder()
            .identifier("testAuthUser")
            .password("wrongPassword")
            .build();

        mockLoginRequest3 = LoginRequest.builder()
            .identifier("nonExistentUser")
            .password("anyPassword")
            .build();

        mockLoginRequest4 = LoginRequest.builder()
            .identifier("testAuthUser@test.com")
            .password("testAuthPasswordHashed")
            .build();

        mockLoginRequest5 = LoginRequest.builder()
            .identifier("0123456789")
            .password("testAuthPasswordHashed")
            .build();

        // Build mock JWT generated result
        mockJwtGeneratedResult = JwtGeneratedResult.builder()
            .accessToken("mockAccessToken")
            .refreshToken("mockRefreshToken")
            .refreshTokenExpiryDate(Instant.now().plusSeconds(3600))
            .build();
    }

    /**
     * Test cases for loginUser
     * Tests:
     * - loginUser_Success_ShouldReturnAuthResultAndAwardBonus
     * - loginUser_InvalidPassword_ShouldThrowUnauthorizedException
     * - loginUser_NonExistentUser_ShouldThrowUnauthorizedException
     * - loginUser_WithEmail_ShouldReturnAuthResult
     * - loginUser_WithPhoneNumber_ShouldReturnAuthResult
     */

    @Test
    void loginUser_Success_ShouldReturnAuthResultAndAwardBonus() {
        // Arrange
        when(userRepo.findByIdentifier(mockLoginRequest1.getIdentifier())).thenReturn(Optional.of(mockUser));
        when(passwordHelper.matchPassword(mockLoginRequest1.getPassword(), mockUser.getPasswordHash())).thenReturn(true);
        when(jwtAdapter.generateToken(
            mockUser.getId(), 
            mockUser.getUsername(), 
            mockUser.getEmail(), 
            mockUser.getRole().name()
        )).thenReturn(mockJwtGeneratedResult);

        com.mongodb.client.result.UpdateResult mockUpdateResult = com.mongodb.client.result.UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(mockUpdateResult);

        // Act
        AuthResult result = authService.loginUser(mockLoginRequest1);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsAuthenticated());
        assertEquals(mockJwtGeneratedResult.getAccessToken(), result.getAccessToken());
        assertEquals(mockJwtGeneratedResult.getRefreshToken(), result.getRefreshToken());

        // Verify
        verify(reputationProcessor, times(1)).awardWeeklyLoginBonus(mockUser.getId());
        verify(refreshTokenProcessor, times(1)).createRefreshToken(
            mockUser.getId(),
            mockJwtGeneratedResult.getRefreshToken(),
            mockJwtGeneratedResult.getRefreshTokenExpiryDate()
        );

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        
        verify(mongoTemplate, times(1)).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(User.class));
        
        assertThat(queryCaptor.getValue().getQueryObject().toJson()).contains(mockUser.getId());
        org.bson.Document updateDoc = updateCaptor.getValue().getUpdateObject();
        assertTrue(updateDoc.containsKey("$set"));
        assertTrue(((org.bson.Document) updateDoc.get("$set")).containsKey("lastLoginAt"));
    }

    @Test
    void loginUser_InvalidPassword_ShouldThrowUnauthorizedException() {
        // Arrange
        when(userRepo.findByIdentifier(mockLoginRequest2.getIdentifier())).thenReturn(Optional.of(mockUser));
        when(passwordHelper.matchPassword(mockLoginRequest2.getPassword(), mockUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> authService.loginUser(mockLoginRequest2));
        assertEquals("Invalid username or password", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        
        // Verify
        verify(reputationProcessor, never()).awardWeeklyLoginBonus(mockUser.getId());
        verify(refreshTokenProcessor, never()).createRefreshToken(anyString(), anyString(), any());
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(User.class));
    }

    @Test
    void loginUser_NonExistentUser_ShouldThrowUnauthorizedException() {
        // Arrange
        when(userRepo.findByIdentifier(mockLoginRequest3.getIdentifier())).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> authService.loginUser(mockLoginRequest3));
        assertEquals("Invalid username or password", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        
        // Verify
        verify(reputationProcessor, never()).awardWeeklyLoginBonus(anyString());
        verify(refreshTokenProcessor, never()).createRefreshToken(anyString(), anyString(), any());
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(User.class));
    }

    @Test
    void loginUser_WithEmail_ShouldReturnAuthResult() {
        // Arrange
        when(userRepo.findByIdentifier(mockLoginRequest4.getIdentifier())).thenReturn(Optional.of(mockUser));
        when(passwordHelper.matchPassword(mockLoginRequest4.getPassword(), mockUser.getPasswordHash())).thenReturn(true);
        when(jwtAdapter.generateToken(
            mockUser.getId(), 
            mockUser.getUsername(), 
            mockUser.getEmail(), 
            mockUser.getRole().name()
        )).thenReturn(mockJwtGeneratedResult);

        UpdateResult mockUpdateResult = com.mongodb.client.result.UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(mockUpdateResult);

        // Act
        AuthResult result = authService.loginUser(mockLoginRequest4);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsAuthenticated());
        assertEquals(mockJwtGeneratedResult.getAccessToken(), result.getAccessToken());
        assertEquals(mockJwtGeneratedResult.getRefreshToken(), result.getRefreshToken());

        // Verify
        verify(reputationProcessor, times(1)).awardWeeklyLoginBonus(mockUser.getId());
        verify(refreshTokenProcessor, times(1)).createRefreshToken(
            mockUser.getId(),
            mockJwtGeneratedResult.getRefreshToken(),
            mockJwtGeneratedResult.getRefreshTokenExpiryDate()
        );
        
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void loginUser_WithPhoneNumber_ShouldReturnAuthResult() {
        // Arrange
        when(userRepo.findByIdentifier(mockLoginRequest5.getIdentifier())).thenReturn(Optional.of(mockUser));
        when(passwordHelper.matchPassword(mockLoginRequest5.getPassword(), mockUser.getPasswordHash())).thenReturn(true);
        when(jwtAdapter.generateToken(
            mockUser.getId(), 
            mockUser.getUsername(), 
            mockUser.getEmail(), 
            mockUser.getRole().name()
        )).thenReturn(mockJwtGeneratedResult);

        com.mongodb.client.result.UpdateResult mockUpdateResult = com.mongodb.client.result.UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(mockUpdateResult);

        // Act
        AuthResult result = authService.loginUser(mockLoginRequest5);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsAuthenticated());
        assertEquals(mockJwtGeneratedResult.getAccessToken(), result.getAccessToken());
        assertEquals(mockJwtGeneratedResult.getRefreshToken(), result.getRefreshToken());

        // Verify
        verify(reputationProcessor, times(1)).awardWeeklyLoginBonus(mockUser.getId());
        verify(refreshTokenProcessor, times(1)).createRefreshToken(
            mockUser.getId(),
            mockJwtGeneratedResult.getRefreshToken(),
            mockJwtGeneratedResult.getRefreshTokenExpiryDate()
        );
        
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }
    /**
     * Test cases for logoutUser
     * Tests:
     * - logoutUser_WithValidToken_ShouldDeleteAndInvalidateToken
     * - logoutUser_NullRefreshToken_ShouldThrowBadRequestException
     * - logoutUser_TokenAlreadyInvalidated_ShouldThrowUnauthorizedException
     */

    @Test
    void logoutUserWithValidToken_ShouldDeleteAndInvalidateToken() {
        // Arrange
        String activeRefreshToken = "activeRefreshToken";
        RefreshToken mockExistingRefreshToken = RefreshToken.builder()
            .userId(mockUser.getId())
            .refreshTokenStr(activeRefreshToken)
            .expiryTime(Instant.now())
            .build();

        when(invalidatedTokenProcessor.isTokenInvalidated(activeRefreshToken)).thenReturn(false);
        when(refreshTokenProcessor.invalidateRefreshToken(activeRefreshToken)).thenReturn(mockExistingRefreshToken);

        // Act
        authService.logoutUser(activeRefreshToken);

        // Verify
        verify(invalidatedTokenProcessor, times(1)).isTokenInvalidated(activeRefreshToken);
        verify(refreshTokenProcessor, times(1)).invalidateRefreshToken(activeRefreshToken);
        verify(invalidatedTokenProcessor, times(1)).invalidateToken(eq(activeRefreshToken), any());
    }

    @Test
    void logoutUser_NullRefreshToken_ShouldThrowBadRequestException() {
        // Arrange
        String nullRefreshToken = null;

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> authService.logoutUser(nullRefreshToken));
        assertEquals("Refresh token is required", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

        // Verify
        verify(invalidatedTokenProcessor, never()).isTokenInvalidated(anyString());
        verify(refreshTokenProcessor, never()).invalidateRefreshToken(anyString());
        verify(invalidatedTokenProcessor, never()).invalidateToken(anyString(), any());
    }

    @Test
    void logoutUser_TokenAlreadyInvalidated_ShouldThrowUnauthorizedException() {
        // Arrange
        String alreadyInvalidatedToken = "alreadyInvalidatedToken";

        when(invalidatedTokenProcessor.isTokenInvalidated(alreadyInvalidatedToken)).thenReturn(true);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> authService.logoutUser(alreadyInvalidatedToken));
        assertEquals("Refresh token is already invalidated", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());

        // Verify
        verify(invalidatedTokenProcessor, times(1)).isTokenInvalidated(alreadyInvalidatedToken);
        verify(refreshTokenProcessor, never()).invalidateRefreshToken(anyString());
        verify(invalidatedTokenProcessor, never()).invalidateToken(anyString(), any());
    }
    /**
     * Test cases for refreshToken
     * Tests:
     * - refreshToken_WithValidTokens_ShouldReturnNewAuthResult
     * - refreshToken_NullTokens_ShouldThrowUnauthorizedException
     * - refreshToken_InvalidTokens_ShouldThrowUnauthorizedException
     * - refreshToken_InvalidAccessToken_ShouldThrowUnauthorizedException
     */

    @Test
    void refreshToken_WithValidTokens_ShouldReturnNewAuthResult() {
        // Arrange
        String oldValidAccessToken = "validAccessToken";
        String oldValidRefreshToken = "validRefreshToken";

        when(invalidatedTokenProcessor.isTokenInvalidated(anyString())).thenReturn(false);

        // Mock jwt adapter behavior
        when(jwtAdapter.validateAccessToken(oldValidAccessToken)).thenReturn(true);
        doReturn(mockUser.getId()).when(jwtAdapter).extractData(oldValidAccessToken, "userId");
        doReturn("validJti").when(jwtAdapter).extractData(oldValidAccessToken, "jti");

        when(userRepo.findByIdAndIsDeletedFalse(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(jwtAdapter.generateToken(
            mockUser.getId(),
            mockUser.getUsername(),
            mockUser.getEmail(),
            mockUser.getRole().name()
        )).thenReturn(mockJwtGeneratedResult);

        // Act
        AuthResult result = authService.refreshToken(oldValidRefreshToken, oldValidAccessToken);

        // Assert
        assertNotNull(result);

        assertTrue(result.getIsAuthenticated());
        assertEquals(mockJwtGeneratedResult.getAccessToken(), result.getAccessToken());
        assertEquals(mockJwtGeneratedResult.getRefreshToken(), result.getRefreshToken());

        // Verify
        verify(invalidatedTokenProcessor, times(2)).isTokenInvalidated(anyString());
        verify(jwtAdapter, times(1)).validateAccessToken(oldValidAccessToken);
        verify(jwtAdapter, times(1)).extractData(oldValidAccessToken, "userId");
        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(mockUser.getId());
        verify(jwtAdapter, times(1)).generateToken(anyString(), anyString(), anyString(), anyString());

        verify(refreshTokenProcessor, times(1)).invalidateRefreshToken(oldValidRefreshToken);
        verify(invalidatedTokenProcessor, times(2)).invalidateToken(anyString(), any(Instant.class));

        verify(refreshTokenProcessor, times(1)).createRefreshToken(anyString(), anyString(), any(Instant.class));
    }

    @Test
    void refreshToken_NullTokens_ShouldThrowUnauthorizedException() {
        // Arrange
        String nullAccessToken = null;
        String nullRefreshToken = null;

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(nullRefreshToken, nullAccessToken));
        assertEquals("Invalid token", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());

        // Verify
        verify(invalidatedTokenProcessor, never()).isTokenInvalidated(anyString());
        verify(jwtAdapter, never()).validateAccessToken(anyString());
        verify(jwtAdapter, never()).extractData(anyString(), anyString());
        verify(userRepo, never()).findByIdAndIsDeletedFalse(anyString());
        verify(jwtAdapter, never()).generateToken(anyString(), anyString(), anyString(), anyString());
        verify(refreshTokenProcessor, never()).invalidateRefreshToken(anyString());
        verify(invalidatedTokenProcessor, never()).invalidateToken(anyString(), any(Instant.class));
        verify(refreshTokenProcessor, never()).createRefreshToken(anyString(), anyString(), any(Instant.class));
    }

    @Test
    void refreshToken_InvalidTokens_ShouldThrowUnauthorizedException() {
        // Arrange
        String invalidAccessToken = "invalidAccessToken";
        String invalidRefreshToken = "invalidRefreshToken";

        when(invalidatedTokenProcessor.isTokenInvalidated(invalidRefreshToken)).thenReturn(true);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(invalidRefreshToken, invalidAccessToken));
        assertEquals("Invalid token", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());

        // Verify
        verify(invalidatedTokenProcessor, times(1)).isTokenInvalidated(invalidRefreshToken);
        verify(jwtAdapter, never()).validateAccessToken(anyString());
        verify(jwtAdapter, never()).extractData(anyString(), anyString());
        verify(userRepo, never()).findByIdAndIsDeletedFalse(anyString());
        verify(refreshTokenProcessor, never()).invalidateRefreshToken(anyString());
    }

    @Test
    void refreshToken_InvalidAccessToken_ShouldThrowUnauthorizedException() {
        // Arrange
        String validRefreshToken = "validRefreshToken";
        String invalidAccessToken = "invalidAccessToken";

        when(invalidatedTokenProcessor.isTokenInvalidated(validRefreshToken)).thenReturn(false);
        doReturn("invalidJti").when(jwtAdapter).extractData(invalidAccessToken, "jti");
        when(invalidatedTokenProcessor.isTokenInvalidated("invalidJti")).thenReturn(false);
        when(jwtAdapter.validateAccessToken(invalidAccessToken)).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(validRefreshToken, invalidAccessToken));
        assertEquals("Invalid access token", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());

        // Verify
        verify(invalidatedTokenProcessor, times(1)).isTokenInvalidated(validRefreshToken);
        verify(invalidatedTokenProcessor, times(1)).isTokenInvalidated("invalidJti");
        verify(jwtAdapter, times(1)).extractData(invalidAccessToken, "jti");
        verify(jwtAdapter, times(1)).validateAccessToken(invalidAccessToken);

        verify(userRepo, never()).findByIdAndIsDeletedFalse(anyString());
        verify(jwtAdapter, never()).generateToken(anyString(), anyString(), anyString(), anyString());
        verify(refreshTokenProcessor, never()).invalidateRefreshToken(anyString());
        verify(invalidatedTokenProcessor, never()).invalidateToken(anyString(), any(Instant.class));
        verify(refreshTokenProcessor, never()).createRefreshToken(anyString(), anyString(), any(Instant.class));
    }

}
