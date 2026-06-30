package com.github.dghng36.eauction.modules.identity.user.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.reputation.service.ReputationProcessor;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.mapper.UserInfoMapper;
import com.github.dghng36.eauction.modules.identity.user.model.User;
import com.github.dghng36.eauction.modules.identity.user.repository.UserRepository;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/*
 * This file is intended to contain utility methods related to UserService. 
 * It can be used for other modules.
 */

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class InternalUserService {
    MongoTemplate mongoTemplate;
    UserRepository userRepo;

    ReputationProcessor reputationProcessor;

    UserInfoMapper userInfoMapper;

    public Map<String, UserInfo> getUserInfoByIds(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<User> users = userRepo.findAllByIdInAndIsDeletedFalse(userIds);

        return users.stream()
            .collect(Collectors.toMap(user -> user.getId(), userInfoMapper::toUserInfo));
    }
    
    public void incrementUserAuctionMetric(
        String userId, String fieldName,
        long amount
    ) {
        Query query = new Query()
            .addCriteria(Criteria.where("_id").is(userId))
            .addCriteria(Criteria.where("isDeleted").is(false));

        String updateField = "userAuctionInfo." + fieldName;
        Update update = new Update()
            .inc(updateField, amount);
        UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);

        if (result.getMatchedCount() == 0) {
            throw new AppException("User not found", HttpStatus.NOT_FOUND);
        }
    }

    public List<String> searchUserIdsByUsername(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("isDeleted").is(false));

        String regex = ".*" + Pattern.quote(keyword) + ".*";
        query.addCriteria(
            new Criteria().orOperator(
                Criteria.where("username").regex(regex, "i"),
                Criteria.where("fullName").regex(regex, "i")
            )
        );

        query.fields().include("_id");

        query.limit(20);

        List<User> users = mongoTemplate.find(query, User.class);

        return users.stream()
            .map(user -> user.getId())
            .toList();
    }

    public Map<String, Map<String, String>> getUsernameAndFullNameFromUserIds(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<User> users = userRepo.findAllByIdInAndIsDeletedFalse(userIds);

        return users.stream()
            .collect(Collectors.toMap(
                user -> user.getId(),
                user -> Map.of(
                    "username", user.getUsername(),
                    "fullName", user.getFullName()
                )
            ));
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void incReputationCreated(String userId, String auctionRoomId) {
        reputationProcessor.incUserCreatedAuctionRoom(userId, auctionRoomId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void incReputationJoined(String userId, String auctionRoomId) {
        reputationProcessor.incUserParticipateAuctionRoom(userId, auctionRoomId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void incReputationWon(String userId, String auctionRoomId) {
        reputationProcessor.incUserWonAuction(userId, auctionRoomId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void decReputationLost(String userId, String auctionRoomId) {
        reputationProcessor.decUserLostAuction(userId, auctionRoomId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void decReputationLostForBatch(List<String> loserIds, String auctionRoomId) {
        reputationProcessor.decUsersLostForBatch(loserIds, auctionRoomId);
    }
}
