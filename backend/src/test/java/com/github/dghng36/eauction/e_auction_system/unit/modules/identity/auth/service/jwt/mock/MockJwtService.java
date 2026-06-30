package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.auth.service.jwt.mock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.http.HttpStatus;

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

public class MockJwtService {
    protected String jwtSecret = "mockSecretKeyForTestingPurposesOnly";

    public String jwtAlgorithm = "HS256";

    protected Long jwtAccessTokenExpirationMins = 15L; // Default to 15 minutes for testing

    protected Long jwtRefreshTokenExpirationDays = 7L; // Default to 7 days for testing

    public String generateAccessToken(String userId, String username, String email, String role) {
        JWSHeader header = switch (jwtAlgorithm) {
            case "HS256" -> new JWSHeader(JWSAlgorithm.HS256);
            case "HS384" -> new JWSHeader(JWSAlgorithm.HS384);
            case "HS512" -> new JWSHeader(JWSAlgorithm.HS512);
            default -> throw new AppException("Unsupported JWT algorithm: " + jwtAlgorithm, HttpStatus.INTERNAL_SERVER_ERROR);
        };

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

    public String generateRefreshToken(String userId) {
        // Generate a random header and signature
        String header = UUID.randomUUID().toString();
        String signature = UUID.randomUUID().toString();

        // Combine header, payload, and signature to create a refresh token
        String refreshToken = String.format("%s:%s:%s", header, userId, signature);

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(refreshToken.getBytes());
    }

    public JwtGeneratedResult generateToken(String userId, String username, String email, String role) {
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
