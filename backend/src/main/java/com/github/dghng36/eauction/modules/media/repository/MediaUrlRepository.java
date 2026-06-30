package com.github.dghng36.eauction.modules.media.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.github.dghng36.eauction.modules.media.model.MediaUrl;

@Repository
public interface MediaUrlRepository extends MongoRepository<MediaUrl, String> {
    Optional<MediaUrl> findByMediaCodeAndActiveTrue(String mediaCode);

    boolean existsByMediaCode(String mediaCode);
    
}
