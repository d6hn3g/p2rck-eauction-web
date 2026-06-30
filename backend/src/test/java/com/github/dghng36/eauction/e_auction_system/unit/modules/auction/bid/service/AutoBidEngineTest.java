package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.bid.service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.dghng36.eauction.modules.auction.bid.dto.internal.AutoBidResult;
import com.github.dghng36.eauction.modules.auction.bid.model.AutoBidSetting;
import com.github.dghng36.eauction.modules.auction.bid.service.AutoBidEngine;

@ExtendWith(MockitoExtension.class)
public class AutoBidEngineTest {
    @InjectMocks private AutoBidEngine autoBidEngine;

    private final String auctionRoomId = "room-id-123";

    @BeforeEach
    void setUp() {}

    @Test
    void resolveWinner_noAutoBidSettings() {
        // Arrange
        Double currentBidAmount = 100.0;

        List<AutoBidSetting> settings = new ArrayList<>();

        // Act
        AutoBidResult result = autoBidEngine.resolverWinner(auctionRoomId, currentBidAmount, settings);

        // Assert
        assertFalse(result.getHasAutoBid());
        assertEquals(currentBidAmount, result.getFinalPrice());
    }

    @Test
    void resolveWinner_singleAutoBidSetting_belowMax() {
        // Arrange — current=100, increment=10, max=200 → nextPrice=110 <= max
        AutoBidSetting setting = AutoBidSetting.builder()
            .userId("user-1")
            .maxAutoBidPrice(200.0)
            .incrementAmount(10.0)
            .build();

        // Act
        AutoBidResult result = autoBidEngine.resolverWinner(auctionRoomId, 100.0, new ArrayList<>(List.of(setting)));

        // Assert
        assertTrue(result.getHasAutoBid());
        assertEquals("user-1", result.getWinnerUserId());
        assertEquals(110.0, result.getFinalPrice());
    }

    @Test
    void resolveWinner_singleAutoBidSetting_aboveMax() {
        // Arrange — current=100, increment=10, max=105 → nextPrice=110 > max
        AutoBidSetting setting = AutoBidSetting.builder()
            .userId("user-1")
            .maxAutoBidPrice(105.0)
            .incrementAmount(10.0)
            .build();

        // Act
        AutoBidResult result = autoBidEngine.resolverWinner(auctionRoomId, 100.0, new ArrayList<>(List.of(setting)));

        // Assert
        assertFalse(result.getHasAutoBid());
        assertEquals(100.0, result.getFinalPrice());
    }

    @Test
    void resolveWinner_multipleAutoBidSettings_topWins() {
        // Arrange — top bidder wins at price capped by second bidder's max
        AutoBidSetting topSetting = AutoBidSetting.builder()
            .userId("user-top")
            .maxAutoBidPrice(500.0)
            .incrementAmount(10.0)
            .build();
        AutoBidSetting secondSetting = AutoBidSetting.builder()
            .userId("user-second")
            .maxAutoBidPrice(300.0)
            .incrementAmount(10.0)
            .build();
        List<AutoBidSetting> settings = new ArrayList<>(List.of(secondSetting, topSetting));

        // Act
        AutoBidResult result = autoBidEngine.resolverWinner(auctionRoomId, 100.0, settings);

        // Assert — finalPrice = min(300 + 10, 500) = 310
        assertTrue(result.getHasAutoBid());
        assertEquals("user-top", result.getWinnerUserId());
        assertEquals(310.0, result.getFinalPrice());
    }

    @Test
    void resolveWinner_multipleAutoBidSettings_secondWins() {
        // Arrange — current price already exceeds computed auto-bid price, no auto bid
        AutoBidSetting topSetting = AutoBidSetting.builder()
            .userId("user-top")
            .maxAutoBidPrice(500.0)
            .incrementAmount(10.0)
            .build();
        AutoBidSetting secondSetting = AutoBidSetting.builder()
            .userId("user-second")
            .maxAutoBidPrice(300.0)
            .incrementAmount(10.0)
            .build();
        List<AutoBidSetting> settings = new ArrayList<>(List.of(secondSetting, topSetting));

        // Act — finalPrice = min(300 + 10, 500) = 310 <= current 350
        AutoBidResult result = autoBidEngine.resolverWinner(auctionRoomId, 350.0, settings);

        // Assert
        assertFalse(result.getHasAutoBid());
        assertEquals(350.0, result.getFinalPrice());
    }
}
