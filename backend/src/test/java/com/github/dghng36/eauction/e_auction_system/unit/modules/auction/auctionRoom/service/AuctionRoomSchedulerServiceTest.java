package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.auctionRoom.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.github.dghng36.eauction.e_auction_system.unit.support.JobExecutorTasksMockHelper;
import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionStartedEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomParticipantService;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomSchedulerService;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;
import com.github.dghng36.eauction.modules.auction.product.service.AuctionProductService;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class AuctionRoomSchedulerServiceTest {

    @Mock private MongoTemplate mongoTemplate;
    @Mock private AuctionRoomParticipantService auctionRoomParticipantService;
    @Mock private AuctionProductService auctionProductService;
    @Mock private InternalUserService internalUserService;
    @Mock private InternalWalletService internalWalletService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private JobExecutorTasks jobExecutorTasks;

    @InjectMocks private AuctionRoomSchedulerService auctionRoomSchedulerService;

    private AuctionRoom mockUpcomingRoom;
    private AuctionRoom mockOngoingRoom;
    private final String roomId = "room-id-123";
    private final String winnerId = "winner-id-456";

    private AuctionProduct mockAuctionProduct;

    @BeforeEach
    void setUp() {
        JobExecutorTasksMockHelper.runSynchronously(jobExecutorTasks);

        mockAuctionProduct = AuctionProduct.builder()
            .productId("product-id-789")
            .currentPrice(100.0)
            .priceStep(10.0)
            .build();

        mockUpcomingRoom = AuctionRoom.builder()
            .id(roomId)
            .title("Test Auction Room")
            .status(AuctionRoomStatus.UPCOMING)
            .startTime(Instant.now().minus(1, ChronoUnit.MINUTES))
            .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .auctionProduct(mockAuctionProduct)
            .build();

        mockOngoingRoom = AuctionRoom.builder()
            .id(roomId)
            .title("Test Ongoing Room")
            .status(AuctionRoomStatus.ONGOING)
            .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().minus(1, ChronoUnit.MINUTES))
            .lastWinnerId(winnerId)
            .currentMaxPrice(500.0)
            .auctionProduct(mockAuctionProduct)
            .build();
    }

    /**
     * Test cases for notifyAllUpcomingAuctionRooms
     * Tests:
     * - notifyAllUpcomingAuctionRooms_Success
     */

    @Test
    void notifyAllUpcomingAuctionRooms_Success() {
        // Arrange
        when(mongoTemplate.find(any(), eq(AuctionRoom.class))).thenReturn(List.of(mockUpcomingRoom));
        when(auctionRoomParticipantService.getActiveParticipantIds(roomId)).thenReturn(List.of("user-1", "user-2"));

        // Act
        auctionRoomSchedulerService.notifyAllUpcomingAuctionRooms();

        // Assert
        verify(eventPublisher, times(1)).publishEvent(any(AuctionStartedEvent.class));
    }

    /**
     * Test cases for startAllUpcomingAuctionRooms
     * Tests:
     * - startAllUpcomingAuctionRooms_Success
     * - startAllUpcomingAuctionRooms_NoUpcomingAuctions
     */

    @Test
    void startAllUpcomingAuctionRooms_Success() {
        // Arrange
        when(mongoTemplate.find(any(), eq(AuctionRoom.class))).thenReturn(List.of(mockUpcomingRoom));
        when(mongoTemplate.updateMulti(any(), any(), eq(AuctionRoom.class)))
            .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(auctionRoomParticipantService.getActiveParticipantIds(roomId)).thenReturn(List.of("user-1"));

        // Act
        auctionRoomSchedulerService.startAllUpcomingAuctionRooms();

        // Assert
        verify(mongoTemplate, times(1)).updateMulti(any(), any(), eq(AuctionRoom.class));
        verify(eventPublisher, times(1)).publishEvent(any(AuctionStartedEvent.class));
    }

    @Test
    void startAllUpcomingAuctionRooms_NoUpcomingAuctions() {
        // Arrange
        when(mongoTemplate.find(any(), eq(AuctionRoom.class))).thenReturn(List.of());

        // Act
        auctionRoomSchedulerService.startAllUpcomingAuctionRooms();

        // Assert
        verify(mongoTemplate, never()).updateMulti(any(), any(), eq(AuctionRoom.class));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    /**
     * Test cases for endAllOngoingAuctionRooms
     * Tests:
     * - endAllOngoingAuctionRooms_WithWinner_ShouldPublishWinnerAndLostEvents
     * - endAllOngoingAuctionRooms_NoOngoingAuctions_ShouldDoNothing
     * - endAllOngoingAuctionRooms_NoWinner_ShouldPublishCanceledEvent
     */

    @Test
    void endAllOngoingAuctionRooms_WithWinner_ShouldPublishWinnerAndLostEvents() {
        // Arrange
        when(mongoTemplate.find(any(), eq(AuctionRoom.class))).thenReturn(List.of(mockOngoingRoom));
        when(mongoTemplate.updateMulti(any(), any(), eq(AuctionRoom.class)))
            .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(auctionRoomParticipantService.getActiveParticipantIds(roomId)).thenReturn(List.of(winnerId, "user-2"));

        // Act
        auctionRoomSchedulerService.endAllOngoingAuctionRooms();

        // Assert — ended + winner + lost events published
        verify(eventPublisher, times(3)).publishEvent(any(Object.class));
    }

    @Test
    void endAllOngoingAuctionRooms_NoOngoingAuctions_ShouldDoNothing() {
        // Arrange
        when(mongoTemplate.find(any(), eq(AuctionRoom.class))).thenReturn(List.of());

        // Act
        auctionRoomSchedulerService.endAllOngoingAuctionRooms();

        // Assert
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void endAllOngoingAuctionRooms_NoWinner_ShouldPublishCanceledEvent() {
        // Arrange
        AuctionRoom noWinnerRoom = AuctionRoom.builder()
            .id(roomId)
            .title("No Winner Room")
            .status(AuctionRoomStatus.ONGOING)
            .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().minus(1, ChronoUnit.MINUTES))
            .lastWinnerId(null)
            .auctionProduct(mockAuctionProduct)
            .build();
        when(mongoTemplate.find(any(), eq(AuctionRoom.class))).thenReturn(List.of(noWinnerRoom));
        when(mongoTemplate.updateMulti(any(), any(), eq(AuctionRoom.class)))
            .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(auctionRoomParticipantService.getActiveParticipantIds(roomId)).thenReturn(List.of());

        // Act
        auctionRoomSchedulerService.endAllOngoingAuctionRooms();

        // Assert — canceled + ended events
        verify(eventPublisher, times(2)).publishEvent(any(Object.class));
    }
}
