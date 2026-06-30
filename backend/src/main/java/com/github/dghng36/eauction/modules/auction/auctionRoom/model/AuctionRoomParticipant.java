package com.github.dghng36.eauction.modules.auction.auctionRoom.model;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.auction.enums.ParticipantStatus;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "auction_room_participants")
@CompoundIndexes({
    @CompoundIndex(name = "idx_room_user_status", def = "{'auctionRoomId': 1, 'userId': 1, 'status': 1}")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class AuctionRoomParticipant extends BaseEntity{
    String auctionRoomId;

    String userId;

    ParticipantStatus status;

    Instant joinedAt;
    Instant approvedAt;
    
    String approvedBy;

    @Size(max = 500, message = "Join reason must be less than 500 characters")
    String joinReason;
    
    @Size(max = 500, message = "Approved reason must be less than 500 characters")
    String approvedReason;

}
