package com.github.dghng36.eauction.modules.auction.bid.event;

import java.time.Instant;

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
public class BidPlacedEvent {
    String bidId;

    String auctionRoomId;

    String userId;

    String username;

    Double bidAmount;

    Double totalBidAmount;
    
    Instant bidTime;

    Boolean isAutoBid;
}
