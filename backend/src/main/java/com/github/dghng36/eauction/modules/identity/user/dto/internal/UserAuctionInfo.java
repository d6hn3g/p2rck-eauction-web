package com.github.dghng36.eauction.modules.identity.user.dto.internal;

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
public class UserAuctionInfo {
    Long totalBids;

    Long totalWins;

    Long totalAuctionRoomsCreated;

    Long totalAuctionRoomsJoined;
}
