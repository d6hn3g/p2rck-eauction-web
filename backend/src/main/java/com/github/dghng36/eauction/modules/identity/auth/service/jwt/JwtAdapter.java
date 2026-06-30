package com.github.dghng36.eauction.modules.identity.auth.service.jwt;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.identity.auth.dto.internal.JwtGeneratedResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class JwtAdapter {
    JwtExtractor jwtExtractor;

    JwtService jwtService;

    JwtValidator jwtValidator;
    
    public JwtGeneratedResult generateToken(String userId, String username, String email, String role) {
        return jwtService.generateToken(userId, username, email, role);
    }

    public String extractData(String accessToken, String dataType) {
        return jwtExtractor.extractData(accessToken, dataType);
    }

    public boolean validateAccessToken(String accessToken) {
        return jwtValidator.validateAccessToken(accessToken);
    }
}
