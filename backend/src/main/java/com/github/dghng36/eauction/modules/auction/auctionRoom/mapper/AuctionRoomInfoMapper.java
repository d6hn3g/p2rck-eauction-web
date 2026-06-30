package com.github.dghng36.eauction.modules.auction.auctionRoom.mapper;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.internal.AuctionRoomInfo;

@Component
public class AuctionRoomInfoMapper {
    public AuctionRoomInfo toAuctionRoomInfo(String auctionRoomId, String auctionRoomTitle) {
        return AuctionRoomInfo.builder()
            .auctionRoomId(auctionRoomId)
            .auctionRoomTitle(auctionRoomTitle)
            .build();
    }
}
