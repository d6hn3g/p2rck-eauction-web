package com.github.dghng36.eauction.modules.identity.auth.service.jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.auth.dto.internal.JwtGeneratedResult;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;

@Component
class JwtService {
    @Value("${app.jwt.secret}")
    protected String jwtSecret;

    @Value("${app.jwt.algorithm}")
    protected String jwtAlgorithm;

    @Value("${app.jwt.access-token-expiration-mins}")
    protected Long jwtAccessTokenExpirationMins;

    @Value ("${app.jwt.refresh-token-expiration-days}")
    protected Long jwtRefreshTokenExpirationDays;

    private String generateAccessToken(String userId, String username, String email, String role) {
        JWSHeader header = switch (jwtAlgorithm) {
            case "HS256" -> new JWSHeader(JWSAlgorithm.HS256);
            case "HS384" -> new JWSHeader(JWSAlgorithm.HS384);
            case "HS512" -> new JWSHeader(JWSAlgorithm.HS512);
            default -> throw new AppException("Unsupported JWT algorithm", HttpStatus.INTERNAL_SERVER_ERROR);
        };
        
        // Validate and convert the role string to UserRole enum
        UserRole userRole = UserRole.fromString(role).
            orElseThrow(() -> new AppException("Invalid user role: " + role, HttpStatus.BAD_REQUEST));

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .subject(userId)
            .issuer("e-auction-system.com")
            .issueTime(new Date())
            .expirationTime(new Date(
                Instant.now().plus(jwtAccessTokenExpirationMins, ChronoUnit.MINUTES).toEpochMilli()
            ))
            .jwtID(UUID.randomUUID().toString())
            .claim("username", username)
            .claim("email", email)
            .claim("role", "ROLE_" + userRole.name())
            .build();
        
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        // Combine header and payload to create JWSObject
        JWSObject jwsObject = new JWSObject(header, payload);

        // Sign the JWSObject using the secret key
        try {
            jwsObject.sign(new MACSigner(jwtSecret.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException | IllegalStateException e) {
            throw new AppException("Failed to generate JWT token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateRefreshToken(String userId) {
        // Generate a random header and signature
        String header = UUID.randomUUID().toString();
        String signature = UUID.randomUUID().toString();

        // Combine header, payload, and signature to create a refresh token
        String refreshToken = String.format("%s:%s:%s", header, userId, signature);

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(refreshToken.getBytes());
    }

    JwtGeneratedResult generateToken(String userId, String username, String email, String role) {
        String accessToken = generateAccessToken(userId, username, email, role);
        String refreshToken = generateRefreshToken(userId);

        Instant refreshTokenExpiryDate = Instant.now().plus(jwtRefreshTokenExpirationDays, ChronoUnit.DAYS);
        return JwtGeneratedResult.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .refreshTokenExpiryDate(refreshTokenExpiryDate)
            .build();
    }   
}
