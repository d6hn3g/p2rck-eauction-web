package com.github.dghng36.eauction.modules.identity.auth.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.identity.auth.model.RefreshToken;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {    
    Optional<RefreshToken> findByRefreshTokenStr(String refreshTokenStr);
}
