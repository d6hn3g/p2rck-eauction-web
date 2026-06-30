package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.bid.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.bson.Document;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import com.github.dghng36.eauction.modules.auction.bid.model.AutoBidSetting;
import com.github.dghng36.eauction.modules.auction.bid.repository.AutoBidSettingRepository;
import com.github.dghng36.eauction.modules.auction.bid.service.AutoBidProcessor;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class AutoBidProcessorTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private AutoBidSettingRepository autoBidSettingRepo;

    @InjectMocks private AutoBidProcessor autoBidProcessor;

    private final String auctionRoomId = "room-id-123";
    private final String userId = "user-id-456";

    @BeforeEach
    void setUp() {}

    @Test
    void processEnableAutoBid_ShouldCallMongoTemplateUpsertWithCorrectData() {
        // Arrange
        Double maxAutoBidPrice = 250.0;
        Double incrementAmount = 10.0;

        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.upsert(any(Query.class), any(Update.class), eq(AutoBidSetting.class)))
            .thenReturn(mockResult);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        // Act
        autoBidProcessor.processEnableAutoBid(auctionRoomId, userId, maxAutoBidPrice, incrementAmount);

        // Assert
        verify(mongoTemplate, times(1)).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(AutoBidSetting.class));

        String capturedQueryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertThat(capturedQueryJson).contains(auctionRoomId);
        assertThat(capturedQueryJson).contains(userId);

        Document updateDoc = updateCaptor.getValue().getUpdateObject();
        assertTrue(updateDoc.containsKey("$set"));
        
        Document setFields = (org.bson.Document) updateDoc.get("$set");
        assertEquals(true, setFields.get("enabled"));
        assertEquals(maxAutoBidPrice, setFields.get("maxAutoBidPrice"));
        assertEquals(incrementAmount, setFields.get("incrementAmount"));
        
        verify(autoBidSettingRepo, never()).save(any());
    }

    @Test
    void processDisableAutoBid_SettingExists_ShouldDisableAndSave() {
        // Arrange
        AutoBidSetting existingSetting = AutoBidSetting.builder()
            .id("setting-id-1")
            .auctionRoomId(auctionRoomId)
            .userId(userId)
            .enabled(true)
            .build();
        
        when(autoBidSettingRepo.findByAuctionRoomIdAndUserId(auctionRoomId, userId))
            .thenReturn(Optional.of(existingSetting));

        // Act
        autoBidProcessor.processDisableAutoBid(auctionRoomId, userId);

        // Assert
        assertFalse(existingSetting.getEnabled());
        verify(autoBidSettingRepo, times(1)).save(existingSetting);
    }

    @Test
    void processDisableAutoBid_SettingDoesNotExist_ShouldDoNothing() {
        // Arrange
        when(autoBidSettingRepo.findByAuctionRoomIdAndUserId(auctionRoomId, userId))
            .thenReturn(Optional.empty());

        // Act
        autoBidProcessor.processDisableAutoBid(auctionRoomId, userId);

        // Assert
        verify(autoBidSettingRepo, never()).save(any());
    }

    @Test
    void validateAutoBidPrice_ValidInputs_ShouldReturnTrue() {
        assertTrue(autoBidProcessor.validateAutoBidPrice(100.0, 5.0));
        assertTrue(autoBidProcessor.validateAutoBidPrice(0.1, 0.1));
    }

    @Test
    void validateAutoBidPrice_InvalidInputs_ShouldReturnFalse() {
        assertFalse(autoBidProcessor.validateAutoBidPrice(null, 5.0));
        assertFalse(autoBidProcessor.validateAutoBidPrice(100.0, null));
        assertFalse(autoBidProcessor.validateAutoBidPrice(0.0, 5.0));
        assertFalse(autoBidProcessor.validateAutoBidPrice(100.0, -1.0));
    }
}
