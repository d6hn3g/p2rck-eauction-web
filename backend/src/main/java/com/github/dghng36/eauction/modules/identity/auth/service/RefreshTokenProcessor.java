package com.github.dghng36.eauction.modules.identity.auth.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.auth.model.RefreshToken;
import com.github.dghng36.eauction.modules.identity.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenProcessor {
    RefreshTokenRepository refreshTokenRepo;

    public void isRefreshTokenExpired(String refreshToken) {
        RefreshToken token = refreshTokenRepo.findByRefreshTokenStr(refreshToken)
            .orElseThrow(() -> new AppException("Refresh token not found", HttpStatus.NOT_FOUND));

        if (token.getExpiryTime().isBefore(Instant.now())) {
            throw new AppException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional
    public RefreshToken invalidateRefreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepo.findByRefreshTokenStr(refreshToken)
            .orElseThrow(() -> new AppException("Refresh token not found", HttpStatus.NOT_FOUND));
        refreshTokenRepo.delete(token);
        return token;
    }

    @Transactional
    public RefreshToken createRefreshToken(String userId, String refreshTokenStr, Instant expiryTime) {
        return refreshTokenRepo.save(RefreshToken.builder()
            .userId(userId)
            .refreshTokenStr(refreshTokenStr)
            .expiryTime(expiryTime)
            .build()
        );
    }

}
