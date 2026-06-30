package com.github.dghng36.eauction.modules.auction.auctionRoom.event;

import java.util.List;

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
public class AuctionUpcomingEvent {
    String auctionRoomId;

    String auctionTitle;

    List<String> participantIds;
}
