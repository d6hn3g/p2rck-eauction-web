package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.auth.service.jwt.mock;

import com.github.dghng36.eauction.modules.identity.auth.dto.internal.JwtGeneratedResult;

public class MockJwtAdapter {
    MockJwtExtractor jwtExtractor;

    MockJwtService jwtService;

    MockJwtValidator jwtValidator;

    public MockJwtAdapter(MockJwtExtractor jwtExtractor, MockJwtService jwtService, MockJwtValidator jwtValidator) {
        this.jwtExtractor = jwtExtractor;
        this.jwtService = jwtService;
        this.jwtValidator = jwtValidator;
    }
    
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
