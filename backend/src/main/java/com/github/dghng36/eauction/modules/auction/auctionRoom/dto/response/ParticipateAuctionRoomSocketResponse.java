package com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response;

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
public class ParticipateAuctionRoomSocketResponse {
    String auctionRoomId;

    String userId;

    Boolean hasParticipated;
}
