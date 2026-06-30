package com.github.dghng36.eauction.e_auction_system.unit.config.security;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.infra.config.security.JwtAuthFilter;
import com.github.dghng36.eauction.modules.identity.auth.service.InvalidatedTokenProcessor;
import com.github.dghng36.eauction.modules.identity.auth.service.jwt.JwtAdapter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class JwtAuthFilterTest {
    @Mock private InvalidatedTokenProcessor invalidatedTokenProcessor;
    @Mock private FilterChain filterChain;
    @Mock private JwtAdapter jwtAdapter;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Test cases for JwtAuthFilter
     * Tests:
     * - doFilterInternal_WithValidToken_ShouldSetAuthenticationInContext
     * - doFilterInternal_WithInvalidToken_ShouldNotSetAuthenticationInContext
     * - doFilterInternal_WithoutAuthHeader_ShouldNotSetAuthenticationInContext
     */
    @Test
    void doFilterInternal_WithValidToken_ShouldSetAuthenticationInContext() throws ServletException, IOException{
        // Arrange
        when(request.getHeader(ConstantsUtils.AuthenticationConstants.AUTH_HEADER)).thenReturn(
            ConstantsUtils.AuthenticationConstants.TOKEN_PREFIX + "validToken"
        );
        
        // Mocking the behavior of jwtAdapter and invalidatedTokenProcessor
        when(jwtAdapter.validateAccessToken("validToken")).thenReturn(true);
        when(jwtAdapter.extractData("validToken", "jti")).thenReturn("jti-test");
        when(invalidatedTokenProcessor.isTokenInvalidated("jti-test")).thenReturn(false);

        // Mocking extracting user details from the token
        when(jwtAdapter.extractData("validToken", "userId")).thenReturn("userId-123");
        when(jwtAdapter.extractData("validToken", "username")).thenReturn("username-123");
        when(jwtAdapter.extractData("validToken", "role")).thenReturn("ROLE_USER");

        // Act
        ReflectionTestUtils.invokeMethod(
            jwtAuthFilter, 
            "doFilterInternal",
            request, response, filterChain
        );

        // Assert
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());

        Object principal = authentication.getPrincipal();
        if(!(principal instanceof Map<?, ?> map)) {
            throw new AssertionError("Principal is not of type Map");
        }

        assertEquals("userId-123", map.get("userId"));
        assertEquals("username-123", map.get("username"));
        assertEquals("ROLE_USER", authentication.getAuthorities().iterator().next().getAuthority());

        // Verify
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidToken_ShouldNotSetAuthenticationInContext() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(ConstantsUtils.AuthenticationConstants.AUTH_HEADER)).thenReturn(
            ConstantsUtils.AuthenticationConstants.TOKEN_PREFIX + "blacklisted-token"
        );

        when(jwtAdapter.validateAccessToken("blacklisted-token")).thenReturn(true);
        when(jwtAdapter.extractData("blacklisted-token", "jti")).thenReturn("jti-banned");
        when(invalidatedTokenProcessor.isTokenInvalidated("jti-banned")).thenReturn(true);

        // Act
        ReflectionTestUtils.invokeMethod(
            jwtAuthFilter, 
            "doFilterInternal",
            request, response, filterChain
        );

        // Assert
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(authentication == null || !authentication.isAuthenticated());

        // Verify that filter chain is called and response status is set to 401
        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithoutAuthHeader_ShouldNotSetAuthenticationInContext() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(ConstantsUtils.AuthenticationConstants.AUTH_HEADER)).thenReturn(null);

        // Act
        ReflectionTestUtils.invokeMethod(
            jwtAuthFilter, 
            "doFilterInternal",
            request, response, filterChain
        );

        // Assert
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(authentication == null || !authentication.isAuthenticated());

        // Verify that filter chain is called and no authentication is set
        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
