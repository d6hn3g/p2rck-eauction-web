package com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response;

import java.time.Instant;
import java.util.Map;

import com.github.dghng36.eauction.modules.auction.product.dto.response.AuctionProductResponse;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class AuctionRoomResponse {
    String id;

    String title;
    String description;

    AuctionProductResponse auctionProductResponse;

    UserInfo owner;
    UserInfo manager;

    Double currentMaxPrice;

    Instant startTime;
    Instant endTime;

    Boolean isExtended;
    Integer extensionTime;

    String status;

    Map<String, Object> metadata;

    boolean chatEnabled;

    String conversationId;

    Integer totalParticipants;
    Integer currentParticipants;

    // For canceled auction rooms
    String cancelReason;
    Instant canceledAt;
}


