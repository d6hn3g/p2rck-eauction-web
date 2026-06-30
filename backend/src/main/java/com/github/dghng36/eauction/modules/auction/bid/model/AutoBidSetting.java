package com.github.dghng36.eauction.modules.auction.bid.model;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "auto_bid_settings")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class AutoBidSetting extends BaseEntity{
    @Indexed
    String auctionRoomId;

    @Indexed
    String userId;

    Boolean enabled;

    Double maxAutoBidPrice;

    Double incrementAmount;

    Double amountBid;
}
