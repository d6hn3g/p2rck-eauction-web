package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.user.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.reputation.service.ReputationProcessor;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.mapper.UserInfoMapper;
import com.github.dghng36.eauction.modules.identity.user.model.User;
import com.github.dghng36.eauction.modules.identity.user.repository.UserRepository;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class InternalUserServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private UserRepository userRepo;
    @Mock private ReputationProcessor reputationProcessor;
    @Mock private UserInfoMapper userInfoMapper;

    @InjectMocks private InternalUserService internalUserService;

    private final String userId = "user-123";
    private final String auctionRoomId = "room-456";

    @BeforeEach
    void setUp() {}

    /** 
     * Test cases for getUserInfoByIds
     * Tests:
     * - getUserInfoByIds_WithEmptySetIds_ShouldReturnEmptyMap
     * - getUserInfoByIds_WithSetIds_ShouldReturnMapOfUserInfo
     * - getUserInfoByIds_WithSetIdsContainingNonExistingUser_ShouldReturnMapOfExistingUserInfoOnly
    */

    @Test
    void getUserInfoByIds_WithEmptySetIds_ShouldReturnEmptyMap() {
        // Arrange
        Set<String> userIds = Set.of();

        // When
        Map<String, UserInfo> result = internalUserService.getUserInfoByIds(userIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify
        verify(userRepo, never()).findAllByIdInAndIsDeletedFalse(userIds);
        verify(userInfoMapper, never()).toUserInfo(any(User.class));
    }

    @Test
    void getUserInfoByIds_WithSetIds_ShouldReturnMapOfUserInfo() {
        // Arrange
        Set<String> userIds = Set.of("user-id-123", "user-id-456");

        User user1 = User.builder()
            .id("user-id-123")
            .username("username123")
            .build();

        User user2 = User.builder()
            .id("user-id-456")
            .username("username456")
            .build();

        UserInfo info1 = UserInfo.builder()
            .id("user-id-123")
            .username("username123")
            .build();

        UserInfo info2 = UserInfo.builder()
            .id("user-id-456")
            .username("username456")
            .build();

        when(userRepo.findAllByIdInAndIsDeletedFalse(userIds)).thenReturn(
            List.of(user1, user2)
        );
        when(userInfoMapper.toUserInfo(user1)).thenReturn(info1);
        when(userInfoMapper.toUserInfo(user2)).thenReturn(info2);

        // Act
        Map<String, UserInfo> result = internalUserService.getUserInfoByIds(userIds);

        // Assert
        assertNotNull(result);

        assertTrue(result.containsKey("user-id-123"));
        assertTrue(result.containsKey("user-id-456"));

        assertEquals("username123", result.get("user-id-123").getUsername());
        assertEquals("username456", result.get("user-id-456").getUsername());

        // Verify
        verify(userRepo, times(1)).findAllByIdInAndIsDeletedFalse(userIds);
        verify(userInfoMapper, times(2)).toUserInfo(any(User.class));
    }

    @Test
    void getUserInfoByIds_WithSetIdsContainingNonExistingUser_ShouldReturnMapOfExistingUserInfoOnly() {
        // Arrange
        Set<String> userIds = Set.of("user-id-123", "user-id-456", "user-id-789");

        User user1 = User.builder()
            .id("user-id-123")
            .username("username123")
            .build();

        User user2 = User.builder()
            .id("user-id-456")
            .username("username456")
            .build();

        UserInfo info1 = UserInfo.builder()
            .id("user-id-123")
            .username("username123")
            .build();

        UserInfo info2 = UserInfo.builder()
            .id("user-id-456")
            .username("username456")
            .build();


        when(userRepo.findAllByIdInAndIsDeletedFalse(userIds)).thenReturn(
           List.of(user1, user2)
        );
        when(userInfoMapper.toUserInfo(user1)).thenReturn(info1);
        when(userInfoMapper.toUserInfo(user2)).thenReturn(info2);

        // Act
        Map<String, UserInfo> result = internalUserService.getUserInfoByIds(userIds);

        // Assert
        assertNotNull(result);

        assertTrue(result.containsKey("user-id-123"));
        assertTrue(result.containsKey("user-id-456"));
        assertFalse(result.containsKey("user-id-789"));

        assertEquals("username123", result.get("user-id-123").getUsername());
        assertEquals("username456", result.get("user-id-456").getUsername());

        // Verify
        verify(userRepo, times(1)).findAllByIdInAndIsDeletedFalse(userIds);
        verify(userInfoMapper, times(2)).toUserInfo(any(User.class));
    }

    /**
     * Test cases for incrementUserAuctionMetric
     * Tests:
     * - incrementUserAuctionMetric_Success_ShouldCallUpdateFirst
     * - incrementUserAuctionMetric_UserNotFound_ShouldThrowAppException
     */

    @Test
    void incrementUserAuctionMetric_Success_ShouldCallUpdateFirst() {
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(mockResult);

        assertDoesNotThrow(() -> internalUserService.incrementUserAuctionMetric(userId, "totalWins", 1L));
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void incrementUserAuctionMetric_UserNotFound_ShouldThrowAppException() {
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null); // 0 matching records
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(mockResult);

        AppException ex = assertThrows(AppException.class, () -> 
            internalUserService.incrementUserAuctionMetric(userId, "totalWins", 1L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
    /**
     * Test cases for searchUserIdsByUsername
     * Tests:
     * - searchUserIdsByUsername_WithEmptyKeyword_ShouldReturnEmptyList
     * - searchUserIdsByUsername_WithValidKeyword_ShouldReturnListOfUserIds
     */

    @Test
    void searchUserIdsByUsername_WithEmptyKeyword_ShouldReturnEmptyList() {
        List<String> result = internalUserService.searchUserIdsByUsername("");
        assertTrue(result.isEmpty());
        verify(mongoTemplate, never()).find(any(), any());
    }

    @Test
    void searchUserIdsByUsername_WithValidKeyword_ShouldReturnListOfUserIds() {
        User u1 = User.builder().id("id-1").build();
        User u2 = User.builder().id("id-2").build();
        when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(List.of(u1, u2));

        List<String> result = internalUserService.searchUserIdsByUsername("keyword");

        assertEquals(2, result.size());
        assertEquals("id-1", result.get(0));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(User.class));
    }

    /**
     * Test cases for getUsernameAndFullNameFromUserIds
     * Tests:
     * - getUsernameAndFullNameFromUserIds_WithEmptySet_ShouldReturnEmptyMap
     * - getUsernameAndFullNameFromUserIds_WithValidSet_ShouldReturnMapOfUsernameAndFullName
     */

    @Test
    void getUsernameAndFullNameFromUserIds_WithEmptySet_ShouldReturnEmptyMap() {
        Map<String, Map<String, String>> result = internalUserService.getUsernameAndFullNameFromUserIds(Set.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUsernameAndFullNameFromUserIds_WithValidSet_ShouldReturnMapOfUsernameAndFullName() {
        Set<String> userIds = Set.of("user-id-1", "user-id-2");

        User user1 = User.builder()
            .id("user-id-1")
            .username("username1")
            .fullName("Full Name 1")
            .build();

        User user2 = User.builder()
            .id("user-id-2")
            .username("username2")
            .fullName("Full Name 2")
            .build();

        when(userRepo.findAllByIdInAndIsDeletedFalse(userIds)).thenReturn(List.of(user1, user2));

        Map<String, Map<String, String>> result = internalUserService.getUsernameAndFullNameFromUserIds(userIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("user-id-1"));
        assertTrue(result.containsKey("user-id-2"));

        assertEquals("username1", result.get("user-id-1").get("username"));
        assertEquals("Full Name 1", result.get("user-id-1").get("fullName"));
        assertEquals("username2", result.get("user-id-2").get("username"));
        assertEquals("Full Name 2", result.get("user-id-2").get("fullName"));

        verify(userRepo, times(1)).findAllByIdInAndIsDeletedFalse(userIds);
    }

    /**
     * Test cases for Reputation Processor methods
     */

    @Test
    void incReputationCreated_ShouldCallProcessor() {
        assertDoesNotThrow(() -> internalUserService.incReputationCreated(userId, auctionRoomId));
        verify(reputationProcessor, times(1)).incUserCreatedAuctionRoom(userId, auctionRoomId);
    }

    @Test
    void incReputationJoined_ShouldCallProcessor() {
        assertDoesNotThrow(() -> internalUserService.incReputationJoined(userId, auctionRoomId));
        verify(reputationProcessor, times(1)).incUserParticipateAuctionRoom(userId, auctionRoomId);
    }

    @Test
    void incReputationWon_ShouldCallProcessor() {
        assertDoesNotThrow(() -> internalUserService.incReputationWon(userId, auctionRoomId));
        verify(reputationProcessor, times(1)).incUserWonAuction(userId, auctionRoomId);
    }

    @Test
    void decReputationLost_ShouldCallProcessor() {
        assertDoesNotThrow(() -> internalUserService.decReputationLost(userId, auctionRoomId));
        verify(reputationProcessor, times(1)).decUserLostAuction(userId, auctionRoomId);
    }

    @Test
    void decReputationLostForBatch_ShouldCallProcessor() {
        List<String> loserIds = List.of("loser-1", "loser-2");
        assertDoesNotThrow(() -> internalUserService.decReputationLostForBatch(loserIds, auctionRoomId));
        verify(reputationProcessor, times(1)).decUsersLostForBatch(loserIds, auctionRoomId);
    }

}
