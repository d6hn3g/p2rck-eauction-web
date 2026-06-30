package com.github.dghng36.eauction.modules.identity.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.github.dghng36.eauction.modules.identity.user.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    Boolean existsByUsernameOrEmailOrPhoneNumber(String username, String email, String phoneNumber);

    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    Optional<User> findByIdAndIsDeletedFalse(String id);

    Page<User> findAllByIsDeletedFalse(Pageable pageable);

    List<User> findAllByIdInAndIsDeletedFalse(Set<String> ids);

    @Query("{ $and : [" +
            "{ 'isDeleted' : false }," +
            "{ 'status' : { $in : [ 'PENDING', 'VERIFIED' ] } }," +
            "{ $or : [ { 'username' : ?0 }, { 'email' : ?0 }, { 'phoneNumber' : ?0 } ] }" +
            "] }")
    Optional<User> findByIdentifier(String identifier);

}
