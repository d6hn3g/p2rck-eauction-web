package com.github.dghng36.eauction.modules.identity.auth.service;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.modules.identity.auth.model.InvalidatedToken;
import com.github.dghng36.eauction.modules.identity.auth.repository.InvalidatedTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class InvalidatedTokenProcessor {
    InvalidatedTokenRepository invalidatedTokenRepo;

    public boolean isTokenInvalidated(String jti) {
        return invalidatedTokenRepo.existsById(jti);
    }

    @Transactional
    public void invalidateToken(String jti, Instant expiryTime) {
        invalidatedTokenRepo.save(
            InvalidatedToken.builder()
                .id(jti)
                .expiryTime(expiryTime)
                .build()
        );
    }
}
