package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.auctionRoom.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoomParticipant;
import com.github.dghng36.eauction.modules.auction.auctionRoom.repository.AuctionRoomParticipantRepository;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomParticipantService;
import com.github.dghng36.eauction.modules.auction.enums.ParticipantStatus;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class AuctionRoomParticipantServiceTest {

    @Mock MongoTemplate mongoTemplate;
    @Mock AuctionRoomParticipantRepository auctionRoomParticipantRepo;

    @InjectMocks AuctionRoomParticipantService auctionRoomParticipantService;

    private final String userId = "user-id-123";
    private final String auctionRoomId = "room-id-456";
    private final String managerId = "manager-id-789";

    private AuctionRoomParticipant mockParticipant;

    @BeforeEach
    void setUp() {
        mockParticipant = AuctionRoomParticipant.builder()
            .auctionRoomId(auctionRoomId)
            .userId(userId)
            .status(ParticipantStatus.PENDING)
            .joinReason("I want to join")
            .build();
    }

    /**
     * Test cases for createAuctionRoomParticipant
     * Tests:
     * - createAuctionRoomParticipant_Success_ShouldSavedDatabase
     */

    @Test
    void createAuctionRoomParticipant_Success_ShouldSavedDatabase() {
        // Arrange
        when(auctionRoomParticipantRepo.save(any(AuctionRoomParticipant.class))).thenReturn(mockParticipant);

        // Act
        auctionRoomParticipantService.createParticipant(auctionRoomId, userId, "I want to join");

        // Assert
        verify(auctionRoomParticipantRepo, times(1)).save(any(AuctionRoomParticipant.class));
    }

    /**
     * Test cases for existsParticipant
     * Tests:
     * - existsParticipant_Exists_ShouldReturnTrue
     * - existsParticipant_NotExists_ShouldReturnFalse
     */

    @Test
    void existsParticipant_Exists_ShouldReturnTrue() {
        // Arrange
        when(auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserId(auctionRoomId, userId)).thenReturn(true);

        // Act
        boolean result = auctionRoomParticipantService.existsParticipant(userId, auctionRoomId);

        // Assert
        assertTrue(result);
        verify(auctionRoomParticipantRepo, times(1)).existsByAuctionRoomIdAndUserId(auctionRoomId, userId);
    }

    @Test
    void existsParticipant_NotExists_ShouldReturnFalse() {
        // Arrange
        when(auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserId(auctionRoomId, userId)).thenReturn(false);

        // Act
        boolean result = auctionRoomParticipantService.existsParticipant(userId, auctionRoomId);

        // Assert
        assertFalse(result);
    }

    /**
     * Test cases for isParticipantApproved
     * Tests:
     * - isParticipantApproved_Approved_ShouldReturnTrue
     * - isParticipantApproved_NotApproved_ShouldReturnFalse
     */

    @Test
    void isParticipantApproved_Approved_ShouldReturnTrue() {
        // Arrange
        when(auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserIdAndStatus(
            auctionRoomId, userId, ParticipantStatus.APPROVED)).thenReturn(true);

        // Act
        boolean result = auctionRoomParticipantService.isParticipantApproved(userId, auctionRoomId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isParticipantApproved_NotApproved_ShouldReturnFalse() {
        // Arrange
        when(auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserIdAndStatus(
            auctionRoomId, userId, ParticipantStatus.APPROVED)).thenReturn(false);

        // Act
        boolean result = auctionRoomParticipantService.isParticipantApproved(userId, auctionRoomId);

        // Assert
        assertFalse(result);
    }

    /**
     * Test cases for isParticipantInsideAuctionRoom
     * Tests:
     * - isParticipantInsideAuctionRoom_Inside_ShouldReturnTrue
     * - isParticipantInsideAuctionRoom_NotInside_ShouldReturnFalse
     */

    @Test
    void isParticipantInsideAuctionRoom_Inside_ShouldReturnTrue() {
        // Arrange
        when(auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserIdAndStatusIn(
            eq(auctionRoomId), eq(userId), any())).thenReturn(true);

        // Act
        boolean result = auctionRoomParticipantService.isParticipantInsideAuctionRoom(userId, auctionRoomId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isParticipantInsideAuctionRoom_NotInside_ShouldReturnFalse() {
        // Arrange
        when(auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserIdAndStatusIn(
            eq(auctionRoomId), eq(userId), any())).thenReturn(false);

        // Act
        boolean result = auctionRoomParticipantService.isParticipantInsideAuctionRoom(userId, auctionRoomId);

        // Assert
        assertFalse(result);
    }

    /**
     * Test cases for isParticipantLeftAuctionRoom
     * Tests:
     * - isParticipantLeftAuctionRoom_Left_ShouldReturnTrue
     * - isParticipantLeftAuctionRoom_NotLeft_ShouldReturnFalse
     */

    @Test
    void isParticipantLeftAuctionRoom_Left_ShouldReturnTrue() {
        // Arrange
        when(auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserIdAndStatus(
            auctionRoomId, userId, ParticipantStatus.LEFT)).thenReturn(true);

        // Act
        boolean result = auctionRoomParticipantService.isParticipantLeftAuctionRoom(userId, auctionRoomId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isParticipantLeftAuctionRoom_NotLeft_ShouldReturnFalse() {
        // Arrange
        when(auctionRoomParticipantRepo.existsByAuctionRoomIdAndUserIdAndStatus(
            auctionRoomId, userId, ParticipantStatus.LEFT)).thenReturn(false);

        // Act
        boolean result = auctionRoomParticipantService.isParticipantLeftAuctionRoom(userId, auctionRoomId);

        // Assert
        assertFalse(result);
    }

    /**
     * Test cases for requestToJoin
     * Tests:
     * - requestToJoin_ValidRequest_ShouldUpdateParticipantStatus
     * - requestToJoin_ParticipantNotFound_ShouldThrowException
     */

    @Test
    void requestToJoin_ValidRequest_ShouldUpdateParticipantStatus() {
        // Arrange
        UpdateResult mockUpdateResult = Mockito.mock(UpdateResult.class);
        when(mockUpdateResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(), any(), eq(AuctionRoomParticipant.class)))
            .thenReturn(mockUpdateResult);

        // Act
        auctionRoomParticipantService.requestToJoin(userId, auctionRoomId, "Updated reason");

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(AuctionRoomParticipant.class));
    }

    @Test
    void requestToJoin_ParticipantNotFound_ShouldThrowException() {
        // Arrange
        UpdateResult mockUpdateResult = Mockito.mock(UpdateResult.class);
        when(mockUpdateResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(), any(), eq(AuctionRoomParticipant.class)))
            .thenReturn(mockUpdateResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            auctionRoomParticipantService.requestToJoin(userId, auctionRoomId, "reason"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Participant not found", exception.getMessage());

        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(AuctionRoomParticipant.class));
    }

    @Test
    void requestToJoin_InvalidRequest_ShouldThrowException() {
        // Arrange — simulate MongoDB update with no match when participant doesn't exist
        UpdateResult mockUpdateResult = Mockito.mock(UpdateResult.class);
        when(mockUpdateResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(), any(), eq(AuctionRoomParticipant.class)))
            .thenReturn(mockUpdateResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            auctionRoomParticipantService.requestToJoin(userId, auctionRoomId, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Participant not found", exception.getMessage());
    }

    /**
     * Test cases for processParticipantStatus
     * Tests:
     * - processParticipantStatus_ValidStatus_ShouldUpdateParticipantStatus
     * - processParticipantStatus_ParticipantNotFound_ShouldThrowException
     * - processParticipantStatus_InvalidStatus_ShouldThrowException
     */

    @Test
    void processParticipantStatus_ValidStatus_ShouldUpdateParticipantStatus() {
        // Arrange
        UpdateResult mockUpdateResult = Mockito.mock(UpdateResult.class);
        when(mockUpdateResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(), any(), eq(AuctionRoomParticipant.class)))
            .thenReturn(mockUpdateResult);

        // Act
        boolean approved = auctionRoomParticipantService.processParticipantStatus(managerId, auctionRoomId, userId, "APPROVED");

        // Assert
        assertTrue(approved);
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(AuctionRoomParticipant.class));
    }

    @Test
    void processParticipantStatus_ParticipantNotFound_ShouldThrowException() {
        // Arrange
        UpdateResult mockUpdateResult = Mockito.mock(UpdateResult.class);
        when(mockUpdateResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(), any(), eq(AuctionRoomParticipant.class)))
            .thenReturn(mockUpdateResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            auctionRoomParticipantService.processParticipantStatus(managerId, auctionRoomId, userId, "APPROVED"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Participant not found", exception.getMessage());
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(AuctionRoomParticipant.class));
    }

    @Test
    void processParticipantStatus_InvalidStatus_ShouldThrowException() {
        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            auctionRoomParticipantService.processParticipantStatus(managerId, auctionRoomId, userId, "INVALID_STATUS")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Invalid participant status: INVALID_STATUS", exception.getMessage());

        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(AuctionRoomParticipant.class));
    }

    /**
     * Test cases for getActiveParticipantIds
     * Tests:
     * - getActiveParticipantIds_ShouldReturnApprovedUserIds
     */

    @Test
    void getActiveParticipantIds_ShouldReturnApprovedUserIds() {
        // Arrange
        AuctionRoomParticipant approvedParticipant = AuctionRoomParticipant.builder()
            .auctionRoomId(auctionRoomId)
            .userId(userId)
            .status(ParticipantStatus.APPROVED)
            .build();
        when(auctionRoomParticipantRepo.findAllByAuctionRoomIdAndStatus(auctionRoomId, ParticipantStatus.APPROVED))
            .thenReturn(List.of(approvedParticipant));

        // Act
        List<String> ids = auctionRoomParticipantService.getActiveParticipantIds(auctionRoomId);

        // Assert
        assertTrue(ids.contains(userId));
        verify(auctionRoomParticipantRepo, times(1)).findAllByAuctionRoomIdAndStatus(auctionRoomId, ParticipantStatus.APPROVED);
    }
}
