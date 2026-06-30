package com.github.dghng36.eauction.modules.auction.bid.mapper;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.auction.bid.dto.internal.BidderInfo;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;

@Component
public class BidderInfoMapper {
    public BidderInfo toBidderInfo(
        String bidderId,
        String bidderName,
        MediaFile bidderAvatar
    ) {
        return BidderInfo.builder()
            .bidderId(bidderId)
            .bidderName(bidderName)
            .bidderAvatar(bidderAvatar)
            .build();
    }
}
