package com.github.dghng36.eauction.modules.auction.auctionRoom.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "auction_rooms")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class AuctionRoom extends BaseEntity {
    String title;
    String description;

    AuctionProduct auctionProduct;

    @Indexed
    String ownerId;

    @Indexed
    String managerId;

    String currentWinnerId;
    String lastWinnerId;

    Double currentMaxPrice;

    Instant startTime;
    Instant endTime;

    Boolean allowAutoExtend;
    Integer extensionTime;

    @Indexed
    AuctionRoomStatus status;

    Map<String, Object> metadata;

    // Extension field for chatting
    boolean chatEnabled;

    String conversationId; // ID of the conversation in the chat service

    Integer totalParticipants;
    Integer currentParticipants;

    // Cancel time and reason
    String cancelReason;
    Instant canceledAt;
}
