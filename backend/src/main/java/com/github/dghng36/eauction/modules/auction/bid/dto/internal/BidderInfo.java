package com.github.dghng36.eauction.modules.auction.bid.dto.internal;

import org.springframework.data.mongodb.core.index.Indexed;

import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class BidderInfo {
    @Indexed
    String bidderId;
    String bidderName;

    MediaFile bidderAvatar;
}
