package com.github.dghng36.eauction.modules.auction.bid.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SearchUserBidsRequest extends SearchBidsRequest {
    String auctionRoomTitle;
    String auctionProductName;

    Boolean isWinningBid;
}
