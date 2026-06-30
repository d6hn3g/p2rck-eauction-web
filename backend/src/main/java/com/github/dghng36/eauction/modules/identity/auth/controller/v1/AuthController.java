package com.github.dghng36.eauction.modules.identity.auth.controller.v1;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.modules.identity.auth.dto.internal.AuthResult;
import com.github.dghng36.eauction.modules.identity.auth.dto.request.LoginRequest;
import com.github.dghng36.eauction.modules.identity.auth.dto.response.AuthResponse;
import com.github.dghng36.eauction.modules.identity.auth.service.AuthService;
import com.github.dghng36.eauction.modules.identity.helper.CookieHelper;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthController {
    AuthService authService;
    
    CookieHelper cookieHelper;

    @PostMapping("/login")
    ResponseEntity<ApiResponse<AuthResponse>> loginUser(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletResponse response) {

        log.info("Login request received for user: [{}]", loginRequest.getIdentifier());

        AuthResult authResult = authService.loginUser(loginRequest);

        // Build auth response
        AuthResponse authResp = buildAuthResponse(response, authResult);

        // Return response with cookie  
        return ResponseEntity
            .ok(ApiResponse.success("User login successfully", authResp));
    }
    
    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> logoutUser(
        @CookieValue(name = "${app.cookie.name}", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken != null) {
            authService.logoutUser(refreshToken);
        }

        // Set max age to 0 to delete cookie
        cookieHelper.deleteRefreshTokenCookie(response);
        
        return ResponseEntity.ok(ApiResponse.success("User logged out successfully", null));
    }
    
    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
        @CookieValue(name = "${app.cookie.name}", required = false) String oldRefreshToken,
        @RequestHeader("Authorization") String authHeader,
        HttpServletResponse response
    ) {
        log.info("Refresh token request received");
        
        // Extract old access token from Authorization header
        String oldAccessToken = validateAuthenticationHeader(authHeader);

        AuthResult refreshResult = authService.refreshToken(oldRefreshToken, oldAccessToken);

        // Build auth response
        AuthResponse authResp = buildAuthResponse(response, refreshResult);

        return ResponseEntity.ok(ApiResponse.success("User refreshed successfully", authResp));
    }

    // Helper method to build auth response and set refresh token cookie
    private AuthResponse buildAuthResponse(
        HttpServletResponse response,
        AuthResult authResult
    ) {
        if (authResult == null) {
            return null;
        }

        // Set refresh token cookie if authentication
        if (authResult.getIsAuthenticated() != null && authResult.getIsAuthenticated()
            && authResult.getRefreshToken() != null
        ) {
            ResponseCookie refreshTokenCookie = cookieHelper.createRefreshTokenCookie(authResult.getRefreshToken());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        }

        return AuthResponse.builder()
            .accessToken(authResult.getAccessToken())
            .isAuthenticated(authResult.getIsAuthenticated())
            .build();
    }

    // Helper method to validate Authorization header and extract token
    private String validateAuthenticationHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(ConstantsUtils.AuthenticationConstants.AUTH_HEADER)) {
            return null;
        }
        return authHeader.substring(ConstantsUtils.AuthenticationConstants.AUTH_HEADER.length());
    }
    
}
