package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.reputation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.reputation.event.CreatedAuctionAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.LostAuctionPenaltyEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.ParticipatedAuctionAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.WeeklyAwardedBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.event.WonAuctionBonusEvent;
import com.github.dghng36.eauction.modules.identity.reputation.service.ReputationProcessor;
import com.github.dghng36.eauction.modules.identity.user.model.User;
import com.github.dghng36.eauction.modules.identity.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class ReputationProcessorTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private UserRepository userRepo;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ReputationProcessor reputationProcessor;

    private User mockUser;
    private final String mockUserId = "user-test-123";
    private final String auctionRoomId = "room-456";

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
            .id(mockUserId)
            .reputation(100.0)
            .build();
    }

    /**
     * Test cases for Reputation Processor
     * Tests:
     * - awardWeeklyLoginBonus_Success_ShouldUpdateMongoAndPublishEvent
     * - awardWeeklyLoginBonus_UserNotFoundInMongo_ShouldThrowAppException
     * All methods in ReputationProcessor have same behavior, 
     * so we can test one method and assume the others behave similarly.
     */

    @Test
    void awardWeeklyLoginBonus_Success_ShouldUpdateMongoAndPublishEvent() {
        // Arrange
        User updatedUser = User.builder()
            .id(mockUserId)
            .reputation(110.0)
            .build();

        when(mongoTemplate.findAndModify(
            any(Query.class), 
            any(Update.class), 
            any(FindAndModifyOptions.class), 
            eq(User.class)
        )).thenReturn(updatedUser);

        // Act
        reputationProcessor.awardWeeklyLoginBonus(mockUserId);

        // Assert
        verify(mongoTemplate, times(1)).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class));

        verify(eventPublisher, times(1)).publishEvent(any(WeeklyAwardedBonusEvent.class));
    }

    @Test
    void awardWeeklyLoginBonus_UserNotFoundInMongo_ShouldThrowAppException() {
        // Arrange
        when(mongoTemplate.findAndModify(
            any(Query.class), 
            any(Update.class), 
            any(FindAndModifyOptions.class), 
            eq(User.class)
        )).thenReturn(null);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> {
            reputationProcessor.awardWeeklyLoginBonus(mockUserId);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(eventPublisher, never()).publishEvent(any(WeeklyAwardedBonusEvent.class));
    }

    @Test
    void incUserCreatedAuctionRoom_Success_ShouldBonusAndPublishEvent() {
        when(userRepo.findByIdAndIsDeletedFalse(mockUserId)).thenReturn(Optional.of(mockUser));
        
        User updatedUser = User.builder().id(mockUserId).reputation(4.5).build();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(updatedUser);

        reputationProcessor.incUserCreatedAuctionRoom(mockUserId, auctionRoomId);

        verify(eventPublisher, times(1)).publishEvent(any(CreatedAuctionAwardedBonusEvent.class));
    }

    @Test
    void incUserCreatedAuctionRoom_LowReputation_ShouldThrowBadRequest() {
        mockUser.setReputation(1.5);
        when(userRepo.findByIdAndIsDeletedFalse(mockUserId)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> reputationProcessor.incUserCreatedAuctionRoom(mockUserId, auctionRoomId))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void incUserParticipateAuctionRoom_Success_ShouldBonusAndPublishEvent() {
        when(userRepo.findByIdAndIsDeletedFalse(mockUserId)).thenReturn(Optional.of(mockUser));
        
        User updatedUser = User.builder().id(mockUserId).reputation(4.2).build();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(updatedUser);

        reputationProcessor.incUserParticipateAuctionRoom(mockUserId, auctionRoomId);

        verify(eventPublisher, times(1)).publishEvent(any(ParticipatedAuctionAwardedBonusEvent.class));
    }


    @Test
    void incUserWonAuction_Success_ShouldBonusAndPublishEvent() {
        User updatedUser = User.builder().id(mockUserId).reputation(5.0).build();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(updatedUser);

        reputationProcessor.incUserWonAuction(mockUserId, auctionRoomId);

        verify(eventPublisher, times(1)).publishEvent(any(WonAuctionBonusEvent.class));
    }

    @Test
    void atomicUpdate_UserNotFound_ShouldThrowNotFoundException() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> reputationProcessor.incUserWonAuction(mockUserId, auctionRoomId))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void decUserLostAuction_Success_ShouldPenaltyAndPublishEvent() {
        User updatedUser = User.builder().id(mockUserId).reputation(3.5).build();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(updatedUser);

        reputationProcessor.decUserLostAuction(mockUserId, auctionRoomId);

        verify(eventPublisher, times(1)).publishEvent(any(LostAuctionPenaltyEvent.class));
    }

    @Test
    void decUsersLostForBatch_WithValidList_ShouldExecuteMultiUpdateAndPublishEvents() {
        List<String> loserIds = List.of("loser-1", "loser-2", "loser-3");
        
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        reputationProcessor.decUsersLostForBatch(loserIds, auctionRoomId);

        verify(mongoTemplate, times(1)).updateMulti(queryCaptor.capture(), updateCaptor.capture(), eq(User.class));

        String capturedQueryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertThat(capturedQueryJson).contains("loser-1").contains("loser-2").contains("loser-3");

        Document updateDoc = updateCaptor.getValue().getUpdateObject();
        assertThat(updateDoc.containsKey("$set")).isTrue();

        verify(eventPublisher, times(3)).publishEvent(any(LostAuctionPenaltyEvent.class));
    }

    @Test
    void decUsersLostForBatch_WithEmptyOrNullList_ShouldReturnImmediately() {
        reputationProcessor.decUsersLostForBatch(new ArrayList<>(), auctionRoomId);
        reputationProcessor.decUsersLostForBatch(null, auctionRoomId);

        verifyNoInteractions(mongoTemplate, eventPublisher);
    }
}
