package com.github.dghng36.eauction.modules.auction.product.mapper;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProductInfo;

@Component
public class AuctionProductInfoMapper {
    public AuctionProductInfo toAuctionProductInfo(
        String auctionProductId,
        String auctionProductName
    ) {
        if (auctionProductId == null || auctionProductName == null) {
            return null;
        }

        return AuctionProductInfo.builder()
            .auctionProductId(auctionProductId)
            .auctionProductName(auctionProductName)
            .build();
    }
}
