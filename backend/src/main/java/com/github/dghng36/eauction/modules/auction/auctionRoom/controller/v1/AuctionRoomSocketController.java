package com.github.dghng36.eauction.modules.auction.auctionRoom.controller.v1;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.ParticipateAuctionRoomSocketResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomSocketService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@Slf4j
public class AuctionRoomSocketController {
    AuctionRoomSocketService auctionRoomSocketService;

    @MessageMapping("/auctions/rooms/{auctionRoomId}/participate")
    public ParticipateAuctionRoomSocketResponse participateAuctionRoomSocket(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @DestinationVariable String auctionRoomId
    ) {
        log.info("User [{}] is requesting to participate in socket auction room [{}]", userId, auctionRoomId);

        return auctionRoomSocketService.participateAuctionRoomSocket(
            userId,
            auctionRoomId
        );
    }
}
