package com.github.dghng36.eauction.modules.auction.bid.dto.response;

import java.time.Instant;
import java.util.Map;

import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.internal.AuctionRoomInfo;
import com.github.dghng36.eauction.modules.auction.bid.dto.internal.BidderInfo;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProductInfo;

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
public class BidResponse {
    String id;

    AuctionProductInfo auctionProductInfo;
    AuctionRoomInfo auctionRoomInfo;

    BidderInfo bidderInfo;

    Double bidAmount;

    Instant bidTime;

    Boolean isWinningBid;

    Map<String, Object> metadata;
}
