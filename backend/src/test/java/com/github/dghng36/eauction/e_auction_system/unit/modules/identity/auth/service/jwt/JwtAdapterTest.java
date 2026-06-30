package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.auth.service.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.e_auction_system.unit.modules.identity.auth.service.jwt.mock.MockJwtExtractor;
import com.github.dghng36.eauction.e_auction_system.unit.modules.identity.auth.service.jwt.mock.MockJwtService;
import com.github.dghng36.eauction.e_auction_system.unit.modules.identity.auth.service.jwt.mock.MockJwtValidator;
import com.github.dghng36.eauction.modules.identity.auth.dto.internal.JwtGeneratedResult;

/**
 * Mock Jwt methods have behavior of Jwt methods 
 */

@ExtendWith(MockitoExtension.class)
public class JwtAdapterTest {
    @InjectMocks
    private MockJwtService mockJwtService;
    
    @InjectMocks
    private MockJwtValidator mockJwtValidator;

    @InjectMocks
    private MockJwtExtractor mockJwtExtractor;

    @BeforeEach
    void setUp() {
        mockJwtService = new MockJwtService();
        mockJwtValidator = new MockJwtValidator();
        mockJwtExtractor = new MockJwtExtractor();
    }

    /**
     * Test cases for JwtAdapter interact with MockJwtService
     * Tests:
     * - jwtAdapter_GenerateAccessToken_ShouldReturnValidToken
     * - jwtAdapter_GenerateAccessTokenWithInvalidAlgorithm_ShouldThrowAppException
     * - jwtAdapter_GenerateAccessTokenWithInvalidRole_ShouldThrowAppException
     * - jwtAdapter_GenerateToken_ShouldReturnJwtGeneratedResult
     */

    @Test
    void jwtAdapter_GenerateAccessToken_ShouldReturnValidToken() {
        String userId = "jwt-user-id";
        String username = "jwtUserTest";
        String email = "jwtUserTest@test.com";
        String role = "uSeR";

        String accessToken = mockJwtService.generateAccessToken(userId, username, email, role);

        // Validate the generated access token
        boolean isValid = mockJwtValidator.validateAccessToken(accessToken);
        
        // Extract data from the access token
        String extractedUserId = mockJwtExtractor.extractData(accessToken, "userId");
        String extractedUsername = mockJwtExtractor.extractData(accessToken, "username");
        String extractedUserRole = mockJwtExtractor.extractData(accessToken, "role");
        String extractedExpiration = mockJwtExtractor.extractData(accessToken, "expiration");

        // Assertions
        assertTrue(isValid);

        assertTrue(extractedUserId.equals(userId));
        assertTrue(extractedUsername.equals(username));
        assertTrue(extractedUserRole.equals("ROLE_USER"));
        assertTrue(extractedExpiration != null && !extractedExpiration.isEmpty());
    }

    @Test
    void jwtAdapter_GenerateAccessTokenWithInvalidAlgorithm_ShouldThrowAppException() {
        String userId = "jwt-user-id";
        String username = "jwtUserTest";
        String email = "jwtUserTest@test.com";
        String role = "USER";

        // Temporarily set an invalid algorithm for testing
        mockJwtService.jwtAlgorithm = "INVALID_ALGO";

        // Act
        try {
            mockJwtService.generateAccessToken(userId, username, email, role);
        } catch (Exception e) {
            // Assert
            assertTrue(e instanceof AppException);
            assertEquals("Unsupported JWT algorithm: INVALID_ALGO", e.getMessage());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((AppException) e).getStatus());
        } finally {
            // Restore the valid algorithm after the test
            mockJwtService.jwtAlgorithm = "HS256";
        }
    }

    @Test
    void jwtAdapter_GenerateTokenInvalidRole_ShouldThrowAppException() {
        String userId = "jwt-user-id";
        String username = "jwtUserTest";
        String email = "jwtTestUser@test.com";
        String invalidRole = "INVALID_ROLE";

        // Act
        try {
            mockJwtService.generateAccessToken(userId, username, email, invalidRole);
        } catch (Exception e) {
            // Assert
            assertTrue(e instanceof AppException);
            
            assertEquals("Invalid user role: " + invalidRole, e.getMessage());
            assertEquals(HttpStatus.BAD_REQUEST, ((AppException) e).getStatus());
        }
    }

    @Test
    void jwtAdapter_GenerateToken_ShouldReturnJwtGeneratedResult() {
        String userId = "jwt-user-id";
        String username = "jwtUserTest";
        String email = "jwtTestUser@test.com";
        String role = "USER";

        JwtGeneratedResult jwtGeneratedResult = mockJwtService.generateToken(userId, username, email, role);

        // Assert
        assertTrue(jwtGeneratedResult.getAccessToken() != null && !jwtGeneratedResult.getAccessToken().isEmpty());
        assertTrue(jwtGeneratedResult.getRefreshToken() != null && !jwtGeneratedResult.getRefreshToken().isEmpty());
    }

    /**
     * Test cases for JwtAdapter interact with MockJwtExtractor
     * Tests:
     * - jwtAdapter_ExtractData_ShouldReturnCorrectData
     * - jwtAdapter_ExtractDataWithInvalidClaim_ShouldThrowAppException
     */
    @Test
    void jwtAdapter_ExtractData_ShouldReturnCorrectData() {
        String userId = "jwt-user-id";
        String username = "jwtUserTest";
        String email = "jwtUserTest@test.com";
        String role = "USER";

        // Act
        String accessToken = mockJwtService.generateAccessToken(userId, username, email, role);

        String extractedJti = mockJwtExtractor.extractData(accessToken, "jti");
        String extractedUserId = mockJwtExtractor.extractData(accessToken, "userId");
        String extractedUsername = mockJwtExtractor.extractData(accessToken, "username");
        String extractedUserRole = mockJwtExtractor.extractData(accessToken, "role");
        String extractedExpiration = mockJwtExtractor.extractData(accessToken, "expiration");

        // Assert
        assertTrue(extractedJti != null && !extractedJti.isEmpty());

        assertTrue(extractedUserId.equals(userId));
        assertTrue(extractedUsername.equals(username));
        assertTrue(extractedUserRole.equals("ROLE_USER"));
        assertTrue(extractedExpiration != null && !extractedExpiration.isEmpty());
    }

    @Test
    void jwtAdapter_ExtractDataWithInvalidClaim_ShouldThrowAppException() {
        String userId = "jwt-user-id";
        String username = "jwtUserTest";
        String email = "jwtUserTest@test.com";
        String role = "USER";

        // Act
        String accessToken = mockJwtService.generateAccessToken(userId, username, email, role);

        try {
            mockJwtExtractor.extractData(accessToken, "invalidClaim");
        } catch (Exception e) {
            // Assert
            assertTrue(e instanceof AppException);
            assertEquals("Unsupported claim: invalidClaim", e.getMessage());

            assertEquals(HttpStatus.BAD_REQUEST, ((AppException) e).getStatus());
        }
    }

}
