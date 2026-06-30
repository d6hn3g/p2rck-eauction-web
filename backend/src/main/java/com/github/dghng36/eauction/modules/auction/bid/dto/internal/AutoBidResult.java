package com.github.dghng36.eauction.modules.auction.bid.dto.internal;

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
public class AutoBidResult {
    Boolean hasAutoBid;

    String winnerUserId;

    Double finalPrice;
}
