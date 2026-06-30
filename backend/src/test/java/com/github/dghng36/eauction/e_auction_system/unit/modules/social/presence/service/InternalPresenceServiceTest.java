package com.github.dghng36.eauction.e_auction_system.unit.modules.social.presence.service;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.dghng36.eauction.modules.social.enums.PresenceStatus;
import com.github.dghng36.eauction.modules.social.presence.mapper.PresenceMapper;
import com.github.dghng36.eauction.modules.social.presence.model.Presence;
import com.github.dghng36.eauction.modules.social.presence.repository.PresenceRepository;
import com.github.dghng36.eauction.modules.social.presence.service.InternalPresenceService;

@ExtendWith(MockitoExtension.class)
public class InternalPresenceServiceTest {
    @Mock private PresenceRepository presenceRepo;
    @Mock private PresenceMapper presenceMapper;

    @InjectMocks private InternalPresenceService internalPresenceService;

    private final String userId1 = "user-id-1";
    private final String userId2 = "user-id-2";
    private Presence mockPresence1;
    private Presence mockPresence2;

    @BeforeEach
    void setUp() {
        mockPresence1 = Presence.builder()
            .id("presence-1")
            .userId(userId1)
            .status(PresenceStatus.ONLINE)
            .build();

        mockPresence2 = Presence.builder()
            .id("presence-2")
            .userId(userId2)
            .status(PresenceStatus.OFFLINE)
            .build();
    }

    @Test
    void getUserPresencesByUserIds_ShouldReturnSuccessMap() {
        // Arrange
        List<String> userIds = List.of(userId1, userId2);
        when(presenceRepo.findAllByUserIdInAndIsDeletedFalse(userIds))
            .thenReturn(List.of(mockPresence1, mockPresence2));

        // Act
        Map<String, PresenceStatus> result = internalPresenceService.getUserPresencesByUserIds(userIds);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(PresenceStatus.ONLINE, result.get(userId1));
        assertEquals(PresenceStatus.OFFLINE, result.get(userId2));
        verify(presenceRepo, times(1)).findAllByUserIdInAndIsDeletedFalse(userIds);
    }
}
