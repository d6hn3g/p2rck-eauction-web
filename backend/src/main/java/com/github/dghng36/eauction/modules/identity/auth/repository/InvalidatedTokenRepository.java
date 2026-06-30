package com.github.dghng36.eauction.modules.identity.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.identity.auth.model.InvalidatedToken;


public interface InvalidatedTokenRepository extends MongoRepository<InvalidatedToken, String> {}
