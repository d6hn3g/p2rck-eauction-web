package com.github.dghng36.eauction.modules.identity.auth.service.jwt;

import java.text.ParseException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.exception.AppException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

@Component
class JwtValidator {
    @Value("${app.jwt.secret}")
    protected String jwtSecret;

    @Value("${app.jwt.algorithm}")
    protected String jwtAlgorithm;

    private SignedJWT parseToken(String accessToken) {
        try {
            return SignedJWT.parse(accessToken);
        } catch(ParseException e) {
            throw new AppException("Invalid JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    boolean validateSignedAccessToken(String accessToken) {
        try {
            // Parse the JWT token
            SignedJWT signedJwt = parseToken(accessToken);
            
            // Verify signature
            boolean isVerified = switch (jwtAlgorithm) {
                case "HS256" -> signedJwt.verify(new MACVerifier(jwtSecret.getBytes()));
                case "HS384" -> signedJwt.verify(new MACVerifier(jwtSecret.getBytes()));
                case "HS512" -> signedJwt.verify(new MACVerifier(jwtSecret.getBytes()));
                default -> throw new RuntimeException("Unsupported JWT algorithm");
            };

            return isVerified;

        } catch(JOSEException | IllegalArgumentException e) {
            throw new AppException("Invalid signature JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    boolean validateExpiredAccessToken(String accessToken) {
        try {
            // Parse the JWT token
            SignedJWT signedJwt = parseToken(accessToken);

            // Check if the token is expired
            return signedJwt.getJWTClaimsSet().getExpirationTime()
                .before(new Date());

        } catch(ParseException e) {
            throw new AppException("Token is expired: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    boolean validateAccessToken(String accessToken) {
        return validateSignedAccessToken(accessToken) && !validateExpiredAccessToken(accessToken);
    }
}
