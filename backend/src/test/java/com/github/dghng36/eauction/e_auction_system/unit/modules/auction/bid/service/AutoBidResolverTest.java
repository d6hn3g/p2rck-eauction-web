package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.bid.service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.auction.bid.dto.internal.AutoBidResult;
import com.github.dghng36.eauction.modules.auction.bid.model.AutoBidSetting;
import com.github.dghng36.eauction.modules.auction.bid.repository.AutoBidSettingRepository;
import com.github.dghng36.eauction.modules.auction.bid.service.AutoBidEngine;
import com.github.dghng36.eauction.modules.auction.bid.service.AutoBidResolver;
import com.github.dghng36.eauction.modules.auction.bid.service.BidService;

@ExtendWith(MockitoExtension.class)
public class AutoBidResolverTest {
    @Mock private AutoBidSettingRepository autoBidSettingRepo;
    @Mock private BidService bidService;
    @Mock private AutoBidEngine autoBidEngine;

    @InjectMocks private AutoBidResolver autoBidResolver;

    @BeforeEach
    void setUp() {}

    @Test
    void processAutoBid_NoActiveSettings_ShouldReturnImmediately() {
        // Arrange
        String roomId = "room-123";
        String currentBidderId = "user-current";
        Double currentAmount = 100.0;

        when(autoBidSettingRepo.findByAuctionRoomIdAndEnabledTrueAndUserIdNot(roomId, currentBidderId))
            .thenReturn(new ArrayList<>());

        // Act
        autoBidResolver.processAutoBid(roomId, currentBidderId, currentAmount);

        // Assert & Verify
        verify(autoBidSettingRepo, times(1)).findByAuctionRoomIdAndEnabledTrueAndUserIdNot(roomId, currentBidderId);
        verifyNoInteractions(autoBidEngine, bidService);
    }

    @Test
    void processAutoBid_EngineReturnsNoAutoBid_ShouldReturnImmediately() {
        // Arrange
        String roomId = "room-123";
        String currentBidderId = "user-current";
        Double currentAmount = 100.0;

        List<AutoBidSetting> settings = List.of(AutoBidSetting.builder().userId("user-robot").build());
        when(autoBidSettingRepo.findByAuctionRoomIdAndEnabledTrueAndUserIdNot(roomId, currentBidderId))
            .thenReturn(settings);

        AutoBidResult mockResult = AutoBidResult.builder().hasAutoBid(false).build();
        when(autoBidEngine.resolverWinner(roomId, currentAmount, settings)).thenReturn(mockResult);

        // Act
        autoBidResolver.processAutoBid(roomId, currentBidderId, currentAmount);

        // Assert
        verify(bidService, never()).placeBid(any(), any(), any(), any(), anyBoolean(), any(), any(), anyBoolean());
    }

    @Test
    void processAutoBid_RobotGuardTriggered_PriceNotHigher_ShouldAbortToPreventInfiniteLoop() {
        // Arrange
        String roomId = "room-123";
        String currentBidderId = "user-current";
        Double currentAmount = 100.0;

        List<AutoBidSetting> settings = List.of(AutoBidSetting.builder().userId("user-robot").build());
        when(autoBidSettingRepo.findByAuctionRoomIdAndEnabledTrueAndUserIdNot(roomId, currentBidderId))
            .thenReturn(settings);

        AutoBidResult mockResult = AutoBidResult.builder()
            .hasAutoBid(true)
            .winnerUserId("user-robot")
            .finalPrice(100.0)
            .build();
        when(autoBidEngine.resolverWinner(roomId, currentAmount, settings)).thenReturn(mockResult);

        // Act
        autoBidResolver.processAutoBid(roomId, currentBidderId, currentAmount);

        // Assert
        verify(bidService, never()).placeBid(any(), any(), any(), any(), anyBoolean(), any(), any(), anyBoolean());
    }

    @Test
    void processAutoBid_Success_ShouldExecutePlaceBidByRobot() {
        // Arrange
        String roomId = "room-123";
        String currentBidderId = "user-current";
        Double currentAmount = 100.0;
        Double robotFinalPrice = 120.0;
        String robotUserId = "user-robot";

        List<AutoBidSetting> settings = List.of(AutoBidSetting.builder().userId(robotUserId).build());
        when(autoBidSettingRepo.findByAuctionRoomIdAndEnabledTrueAndUserIdNot(roomId, currentBidderId))
            .thenReturn(settings);

        AutoBidResult mockResult = AutoBidResult.builder()
            .hasAutoBid(true)
            .winnerUserId(robotUserId)
            .finalPrice(robotFinalPrice)
            .build();
        when(autoBidEngine.resolverWinner(roomId, currentAmount, settings)).thenReturn(mockResult);

        doNothing().when(bidService).placeBid(
            eq(robotUserId), eq(roomId), eq(robotFinalPrice), 
            eq(null), eq(false), eq(null), eq(null), eq(true)
        );

        // Act & Assert
        assertDoesNotThrow(() -> autoBidResolver.processAutoBid(roomId, currentBidderId, currentAmount));
        
        verify(bidService, times(1)).placeBid(
            eq(robotUserId), eq(roomId), eq(robotFinalPrice), 
            eq(null), eq(false), eq(null), eq(null), eq(true)
        );
    }

    @Test
    void processAutoBid_PlaceBidThrowsAppException_ShouldCatchAndLogWarning() {
        // Arrange
        String roomId = "room-123";
        String currentBidderId = "user-current";
        Double currentAmount = 100.0;

        List<AutoBidSetting> settings = List.of(AutoBidSetting.builder().userId("user-robot").build());
        when(autoBidSettingRepo.findByAuctionRoomIdAndEnabledTrueAndUserIdNot(roomId, currentBidderId))
            .thenReturn(settings);

        AutoBidResult mockResult = AutoBidResult.builder()
            .hasAutoBid(true)
            .winnerUserId("user-robot")
            .finalPrice(120.0)
            .build();
        when(autoBidEngine.resolverWinner(roomId, currentAmount, settings)).thenReturn(mockResult);

        doThrow(new AppException("Bid amount is less than the minimum required bid", HttpStatus.BAD_REQUEST))
            .when(bidService).placeBid(anyString(), anyString(), anyDouble(), any(), anyBoolean(), any(), any(), anyBoolean());

        // Act & Assert
        assertDoesNotThrow(() -> autoBidResolver.processAutoBid(roomId, currentBidderId, currentAmount));
        
        verify(bidService, times(1)).placeBid(anyString(), anyString(), anyDouble(), any(), anyBoolean(), any(), any(), anyBoolean());
    }

    @Test
    void processAutoBid_PlaceBidThrowsGenericException_ShouldCatchAndLogError() {
        // Arrange
        String roomId = "room-123";
        String currentBidderId = "user-current";
        Double currentAmount = 100.0;

        List<AutoBidSetting> settings = List.of(AutoBidSetting.builder().userId("user-robot").build());
        when(autoBidSettingRepo.findByAuctionRoomIdAndEnabledTrueAndUserIdNot(roomId, currentBidderId))
            .thenReturn(settings);

        AutoBidResult mockResult = AutoBidResult.builder()
            .hasAutoBid(true)
            .winnerUserId("user-robot")
            .finalPrice(120.0)
            .build();
        when(autoBidEngine.resolverWinner(roomId, currentAmount, settings)).thenReturn(mockResult);

        doThrow(new RuntimeException("Database timeout error"))
            .when(bidService).placeBid(anyString(), anyString(), anyDouble(), any(), anyBoolean(), any(), any(), anyBoolean());

        // Act & Assert
        assertDoesNotThrow(() -> autoBidResolver.processAutoBid(roomId, currentBidderId, currentAmount));
        
        verify(bidService, times(1)).placeBid(anyString(), anyString(), anyDouble(), any(), anyBoolean(), any(), any(), anyBoolean());
    }
}
