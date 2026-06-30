package com.github.dghng36.eauction.modules.auction.bid.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.auction.bid.dto.internal.BidderInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "bids")
@CompoundIndexes({
    @CompoundIndex(name = "idx_auctionRoom_count", def = "{'auctionRoomId': 1, 'isDeleted': 1}")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Bid extends BaseEntity {
    @Indexed
    String auctionRoomId;
    
    @Indexed
    String auctionProductId;

    BidderInfo bidderInfo;

    Double bidAmount;

    Instant bidTime;

    // Extension utility fields
    Boolean isWinningBid;

    Map<String, Object> metadata;
}
