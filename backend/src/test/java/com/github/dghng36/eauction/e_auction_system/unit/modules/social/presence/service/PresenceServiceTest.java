package com.github.dghng36.eauction.e_auction_system.unit.modules.social.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.social.enums.PresenceStatus;
import com.github.dghng36.eauction.modules.social.presence.dto.request.UpdatePresenceRequest;
import com.github.dghng36.eauction.modules.social.presence.dto.response.PresenceResponse;
import com.github.dghng36.eauction.modules.social.presence.mapper.PresenceMapper;
import com.github.dghng36.eauction.modules.social.presence.model.Presence;
import com.github.dghng36.eauction.modules.social.presence.repository.PresenceRepository;
import com.github.dghng36.eauction.modules.social.presence.service.PresenceService;

@ExtendWith(MockitoExtension.class)
public class PresenceServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private PresenceRepository presenceRepo;
    @Mock private PresenceMapper presenceMapper;

    @InjectMocks private PresenceService presenceService;

    private final String userId = "user-id-123";
    private Presence mockPresence;
    private PresenceResponse mockPresenceResponse;

    @BeforeEach
    void setUp() {
        mockPresence = Presence.builder()
            .id("presence-id-1")
            .userId(userId)
            .status(PresenceStatus.OFFLINE)
            .build();

        mockPresenceResponse = PresenceResponse.builder()
            .userId(userId)
            .status("ONLINE")
            .build();
    }

    @Test
    void updateUserPresence_Success_ShouldAtomicUpsertAndReturnResponse() {
        // Arrange
        UpdatePresenceRequest request = UpdatePresenceRequest.builder()
            .newStatus("ONLINE")
            .build();
            
        Presence realUpdatedPresence = Presence.builder()
            .userId(userId)
            .status(PresenceStatus.ONLINE)
            .build();

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Presence.class)))
            .thenReturn(realUpdatedPresence);
        when(presenceMapper.toPresenceResponse(realUpdatedPresence)).thenReturn(mockPresenceResponse);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<FindAndModifyOptions> optionsCaptor = ArgumentCaptor.forClass(FindAndModifyOptions.class);

        // Act
        PresenceResponse result = presenceService.updateUserPresence(userId, request);

        // Assert
        assertNotNull(result);
        verify(mongoTemplate, times(1)).findAndModify(queryCaptor.capture(), updateCaptor.capture(), optionsCaptor.capture(), eq(Presence.class));
        verify(presenceMapper, times(1)).toPresenceResponse(realUpdatedPresence);

        String queryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains(userId).contains("\"isDeleted\": false");

        org.bson.Document updateDoc = updateCaptor.getValue().getUpdateObject();
        assertTrue(updateDoc.containsKey("$set"));
        org.bson.Document setFields = (org.bson.Document) updateDoc.get("$set");
        assertEquals(userId, setFields.get("userId"));
        assertEquals(PresenceStatus.ONLINE, setFields.get("status"));

        FindAndModifyOptions capturedOptions = optionsCaptor.getValue();
        assertTrue(capturedOptions.isUpsert());
        assertTrue(capturedOptions.isReturnNew());
    }

    @Test
    void updateUserPresence_InvalidStatus_ShouldThrowBadRequest() {
        // Arrange
        UpdatePresenceRequest request = UpdatePresenceRequest.builder()
            .newStatus("INVALID_STATUS")
            .build();

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            presenceService.updateUserPresence(userId, request));
            
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid presence status"));
        
        verify(mongoTemplate, never()).findAndModify(any(), any(), any(), eq(Presence.class));
    }

    @Test
    void updateUserPresence_ConcurrentModification_ShouldThrowConflict() {
        // Arrange
        UpdatePresenceRequest request = UpdatePresenceRequest.builder()
            .newStatus("ONLINE")
            .build();

        
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Presence.class)))
            .thenReturn(null);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            presenceService.updateUserPresence(userId, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Failed to update user presence due to concurrent modification", ex.getMessage());
        verify(presenceMapper, never()).toPresenceResponse(any());
    }
}
