package com.github.dghng36.eauction.modules.identity.user.mapper;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserAuctionInfo;

@Component
public class UserAuctionInfoMapper {
    public UserAuctionInfo defaultUserAuctionInfo() {
        return UserAuctionInfo.builder()
            .totalBids(0L)
            .totalWins(0L)
            .totalAuctionRoomsCreated(0L)
            .totalAuctionRoomsJoined(0L)
            .build();
    }
}
