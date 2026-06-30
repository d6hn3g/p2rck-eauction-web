package com.github.dghng36.eauction.modules.social.presence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.social.presence.model.Presence;

public interface PresenceRepository extends MongoRepository<Presence, String> {
    Optional<Presence> findByUserIdAndIsDeletedFalse(String userId);

    List<Presence> findAllByUserIdInAndIsDeletedFalse(Iterable<String> userIds);
}
