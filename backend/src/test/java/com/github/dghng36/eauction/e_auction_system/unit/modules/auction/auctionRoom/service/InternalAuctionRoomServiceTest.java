package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.auctionRoom.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.internal.AuctionRoomInfo;
import com.github.dghng36.eauction.modules.auction.auctionRoom.mapper.AuctionRoomInfoMapper;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.auctionRoom.repository.AuctionRoomRepository;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomParticipantService;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.InternalAuctionRoomService;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;

@ExtendWith(MockitoExtension.class)
public class InternalAuctionRoomServiceTest {

    @Mock private MongoTemplate mongoTemplate;
    @Mock private AuctionRoomRepository auctionRoomRepo;
    @Mock private AuctionRoomParticipantService auctionRoomParticipantService;
    @Mock private AuctionRoomInfoMapper auctionRoomInfoMapper;

    @InjectMocks private InternalAuctionRoomService internalAuctionRoomService;

    private final String roomId = "room-id-123";
    private final String userId = "user-id-456";
    private AuctionRoom mockOngoingRoom;

    @BeforeEach
    void setUp() {
        AuctionProduct auctionProduct = AuctionProduct.builder()
            .productId("product-id-789")
            .currentPrice(100.0)
            .priceStep(10.0)
            .buyoutPrice(500.0)
            .build();

        mockOngoingRoom = AuctionRoom.builder()
            .id(roomId)
            .title("Test Auction Room")
            .status(AuctionRoomStatus.ONGOING)
            .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .auctionProduct(auctionProduct)
            .allowAutoExtend(false)
            .extensionTime(300)
            .build();
    }

    @Test
    void getProductId_Success_ShouldReturnProductId() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockOngoingRoom));
        String result = internalAuctionRoomService.getProductId(roomId);
        assertThat(result).isEqualTo("product-id-789");
    }

    @Test
    void existsAuctionRoom_ShouldReturnTrue() {
        when(auctionRoomRepo.existsByIdAndIsDeletedFalse(roomId)).thenReturn(true);
        assertTrue(internalAuctionRoomService.existsAuctionRoom(roomId));
    }

    @Test
    void getAuctionRoomInfoByIds_ShouldReturnMap() {
        Set<String> ids = Set.of(roomId);
        when(auctionRoomRepo.findAllByIdInAndIsDeletedFalse(ids)).thenReturn(List.of(mockOngoingRoom));
        
        AuctionRoomInfo info = AuctionRoomInfo.builder().auctionRoomId(roomId).auctionRoomTitle("Test Auction Room").build();
        when(auctionRoomInfoMapper.toAuctionRoomInfo(roomId, "Test Auction Room")).thenReturn(info);

        Map<String, AuctionRoomInfo> result = internalAuctionRoomService.getAuctionRoomInfoByIds(ids);
        assertThat(result).containsKey(roomId);
        assertThat(result.get(roomId).getAuctionRoomTitle()).isEqualTo("Test Auction Room");
    }

    @Test
    void validateAndGetForBidding_Valid_ShouldReturnAuctionRoom() {
        // Arrange
        when(mongoTemplate.findOne(any(Query.class), eq(AuctionRoom.class))).thenReturn(mockOngoingRoom);
        when(auctionRoomParticipantService.isParticipantInsideAuctionRoom(userId, roomId)).thenReturn(true);

        // Act
        AuctionRoom result = internalAuctionRoomService.validateAndGetForBidding(roomId, userId);

        // Assert
        assertNotNull(result);
        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(AuctionRoom.class));
    }

    @Test
    void validateAndGetForBidding_InvalidTimeRange_ShouldThrowAppException() {
        // Arrange
        AuctionRoom endedRoom = AuctionRoom.builder()
            .id(roomId)
            .status(AuctionRoomStatus.ONGOING)
            .startTime(Instant.now().minus(2, ChronoUnit.HOURS))
            .endTime(Instant.now().minus(1, ChronoUnit.MINUTES))
            .auctionProduct(mockOngoingRoom.getAuctionProduct())
            .build();
        when(mongoTemplate.findOne(any(Query.class), eq(AuctionRoom.class))).thenReturn(endedRoom);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            internalAuctionRoomService.validateAndGetForBidding(roomId, userId));

        assertTrue(exception.getMessage().contains("Auction room has ended"));
    }

    @Test
    void validateAndGetForBidding_InvalidAuctionRoomId_ShouldThrowAppException() {
        // Arrange — room is not ONGOING
        AuctionRoom pendingRoom = AuctionRoom.builder()
            .id(roomId)
            .status(AuctionRoomStatus.PENDING_VERIFIED)
            .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
            .auctionProduct(mockOngoingRoom.getAuctionProduct())
            .build();
        when(mongoTemplate.findOne(any(Query.class), eq(AuctionRoom.class))).thenReturn(pendingRoom);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            internalAuctionRoomService.validateAndGetForBidding(roomId, userId));
            
        assertTrue(exception.getMessage().contains("Auction room is not ongoing"));
    }

    @Test
    void validateBidAmount_Valid_ShouldReturnTrue() {
        internalAuctionRoomService.validateBidAmount(mockOngoingRoom, 120.0);
    }

    @Test
    void validateBidAmount_BidAmountLessThanCurrentHighest_ShouldThrowAppException() {
        AppException exception = assertThrows(AppException.class, () ->
            internalAuctionRoomService.validateBidAmount(mockOngoingRoom, 105.0));
        assertTrue(exception.getMessage().contains("Bid amount is less than the minimum required bid"));
    }

    @Test
    void validateBidAmount_InvalidBidAmount_ShouldThrowAppException() {
        // Arrange — null priceStep
        AuctionProduct badProduct = AuctionProduct.builder()
            .productId("p-1")
            .currentPrice(null)
            .priceStep(null)
            .build();
        AuctionRoom badRoom = AuctionRoom.builder()
            .id(roomId)
            .auctionProduct(badProduct)
            .build();

        AppException exception = assertThrows(AppException.class, () ->
            internalAuctionRoomService.validateBidAmount(badRoom, 200.0));

        assertTrue(exception.getMessage().contains("Current price or price step is not set for this auction product"));
    }

    @Test
    void isBuyoutReached_BuyoutPriceNotSet_ShouldReturnFalse() {
        AuctionProduct noBuyout = AuctionProduct.builder()
            .productId("p-1")
            .currentPrice(100.0)
            .buyoutPrice(null)
            .build();
        AuctionRoom room = AuctionRoom.builder().id(roomId).auctionProduct(noBuyout).build();

        boolean result = internalAuctionRoomService.isBuyoutReached(room, 200.0);
        assertFalse(result);
    }

    @Test
    void isBuyoutReached_BuyoutPriceSet_ShouldReturnTrue() {
        boolean result = internalAuctionRoomService.isBuyoutReached(mockOngoingRoom, 400.0);
        assertTrue(result);
    }

    @Test
    void isBuyoutReached_BuyoutPriceSet_ShouldReturnFalse() {
        boolean result = internalAuctionRoomService.isBuyoutReached(mockOngoingRoom, 200.0);
        assertFalse(result);
    }

    @Test
    void processNewBidSuccess_Valid_ShouldUpdateAuctionRoom() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(AuctionRoom.class)))
            .thenReturn(mockOngoingRoom);

        internalAuctionRoomService.processNewBidSuccess(mockOngoingRoom, userId, 200.0);

        verify(mongoTemplate, times(1)).findAndModify(any(), any(), any(FindAndModifyOptions.class), eq(AuctionRoom.class));
    }

    @Test
    void processNewBidSuccess_AuctionRoomNotFound_ShouldThrowAppException() {
        // Arrange
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(AuctionRoom.class)))
            .thenReturn(null);

        AppException exception = assertThrows(AppException.class, () ->
            internalAuctionRoomService.processNewBidSuccess(mockOngoingRoom, userId, 50.0));

        assertTrue(exception.getMessage().contains("Auction room has been updated by another bidder"));
        verify(mongoTemplate).findAndModify(any(), any(), any(FindAndModifyOptions.class), eq(AuctionRoom.class));
    }

    @Test
    void processNewBidSuccess_HasTimeExtend_ShouldUpdateAuctionRoom() {
        AuctionRoom extendRoom = AuctionRoom.builder()
            .id(roomId)
            .status(AuctionRoomStatus.ONGOING)
            .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().plus(30, ChronoUnit.SECONDS))
            .auctionProduct(mockOngoingRoom.getAuctionProduct())
            .allowAutoExtend(true)
            .extensionTime(300)
            .build();

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(AuctionRoom.class)))
            .thenReturn(extendRoom);

        internalAuctionRoomService.processNewBidSuccess(extendRoom, userId, 200.0);

        verify(mongoTemplate, times(1)).findAndModify(any(), any(), any(FindAndModifyOptions.class), eq(AuctionRoom.class));
    }

    @Test
    void updateAuctionRoomStatus_Valid_ShouldUpdateAuctionRoom() {
        internalAuctionRoomService.updateAuctionRoomStatus(roomId, "ONGOING");
        verify(mongoTemplate, times(1)).updateFirst(any(), any(), eq(AuctionRoom.class));
    }

    @Test
    void updateAuctionRoomStatus_InvalidStatus_ShouldThrowAppException() {
        AppException exception = assertThrows(AppException.class, () ->
            internalAuctionRoomService.updateAuctionRoomStatus(roomId, "INVALID_STATUS"));

        assertTrue(exception.getMessage().contains("Invalid auction room status"));
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(AuctionRoom.class));
    }
}