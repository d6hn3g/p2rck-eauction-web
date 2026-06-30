package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.auth.service.jwt.mock;

import java.text.ParseException;

import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.nimbusds.jwt.SignedJWT;

public class MockJwtExtractor {
     private SignedJWT parseAccessToken(String accessToken) {
        try {
            return SignedJWT.parse(accessToken);
        } catch (ParseException e) {
            throw new AppException("Invalid JWT token "+ e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private String extractJti(String accessToken) {
        SignedJWT signedJwt = parseAccessToken(accessToken);
        try {
            return signedJwt.getJWTClaimsSet().getJWTID();
        } catch (ParseException e) {
            throw new AppException("Failed to extract jti from JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private String extractUserId(String accessToken) {
        SignedJWT signedJwt = parseAccessToken(accessToken);
        try {
            return signedJwt.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new AppException("Failed to extract user ID from JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private String extractUsername(String accessToken) {
        SignedJWT signedJwt = parseAccessToken(accessToken);
        try {
            return (String) signedJwt.getJWTClaimsSet().getClaim("username");
        } catch (ParseException e) {
            throw new AppException("Failed to extract username from JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private String extractUserRole(String accessToken) {
        SignedJWT signedJwt = parseAccessToken(accessToken);
        try {
            return (String) signedJwt.getJWTClaimsSet().getClaim("role");
        } catch (ParseException e) {
            throw new AppException("Failed to extract user role from JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

     private String extractExpiration(String accessToken) {
        SignedJWT signedJwt = parseAccessToken(accessToken);
        try {
            return signedJwt.getJWTClaimsSet().getExpirationTime().toString();
        } catch (ParseException e) {
            throw new AppException("Failed to extract expiration time from JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    public String extractData(String accessToken, String claim) {
        return switch (claim) {
            case "jti" -> extractJti(accessToken);
            case "userId" -> extractUserId(accessToken);
            case "username" -> extractUsername(accessToken);
            case "role" -> extractUserRole(accessToken);
            case "expiration" -> extractExpiration(accessToken);
            default -> throw new AppException("Unsupported claim: " + claim, HttpStatus.BAD_REQUEST);
        };
    }
}
