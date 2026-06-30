package com.github.dghng36.eauction.e_auction_system.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.simp.stomp.StompSession;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.e_auction_system.integration.support.AbstractIntegrationTest;
import com.github.dghng36.eauction.e_auction_system.integration.support.TestUserSession;
import com.github.dghng36.eauction.e_auction_system.integration.support.WebSocketTestHelper;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.CreateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.ParticipateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.UpdateAuctionRoomStatusRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.repository.AuctionRoomRepository;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.PlaceBidSocketRequest;
import com.github.dghng36.eauction.modules.auction.bid.repository.BidRepository;
import com.github.dghng36.eauction.modules.auction.product.dto.request.CreateProductRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.response.ProductResponse;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.media.dto.request.MediaFileUploadRequest;

class FullAuctionWorkflowE2ETest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AuctionRoomRepository auctionRoomRepository;

    @Autowired
    private BidRepository bidRepository;

    private TestUserSession admin;
    private TestUserSession manager;
    private TestUserSession seller;
    private TestUserSession bidderOne;
    private TestUserSession bidderTwo;

    @BeforeEach
    void setUpScenario() {
        mongoTemplate.getDb().drop();
        admin = seedPrivilegedUser("e2e_admin", UserRole.ADMIN);
        manager = seedPrivilegedUser("e2e_manager", UserRole.MANAGER);
        seller = registerAndLogin("e2e_seller");
        bidderOne = registerAndLogin("e2e_bidder1");
        bidderTwo = registerAndLogin("e2e_bidder2");

        creditWallet(admin, seller.getUserId(), 1_000_000);
        creditWallet(admin, bidderOne.getUserId(), 1_000_000);
        creditWallet(admin, bidderTwo.getUserId(), 1_000_000);
    }

    @Test
    void fullAuctionUserWorkflow_FromRegistrationToBidding_ShouldSucceed() throws Exception {
        String mediaCode = seedMediaCode();

        ApiResponse<ProductResponse> productResponse = postAuthenticated(
            "/api/v1/users/me/products",
            new CreateProductRequest(
                "E2E Vintage Watch",
                "End-to-end workflow product for auction scenario",
                List.of(MediaFileUploadRequest.builder()
                    .mediaCode(mediaCode)
                    .objectKey(mediaCode + ".jpg")
                    .build()),
                null
            ),
            seller.getAccessToken(),
            new ParameterizedTypeReference<>() {}
        );
        String productId = productResponse.getData().getId();

        ApiResponse<AuctionRoomResponse> roomResponse = postAuthenticated(
            "/api/v1/users/me/auctions/rooms",
            CreateAuctionRoomRequest.builder()
                .title("E2E Auction Room")
                .description("Full user workflow end-to-end auction")
                .startTime(Instant.now().plusSeconds(300))
                .durationMinutes(120)
                .productId(productId)
                .startPrice(200.0)
                .priceStep(20.0)
                .buyoutPrice(2000.0)
                .build(),
            seller.getAccessToken(),
            new ParameterizedTypeReference<>() {}
        );
        String roomId = roomResponse.getData().getId();

        patchAuthenticated(
            "/api/v1/manager/management/auctions/rooms/" + roomId,
            null,
            manager.getAccessToken(),
            new ParameterizedTypeReference<ApiResponse<AuctionRoomResponse>>() {}
        );
        patchAuthenticated(
            "/api/v1/manager/management/auctions/rooms/" + roomId + "/status",
            UpdateAuctionRoomStatusRequest.builder().newAuctionRoomStatus("ONGOING").build(),
            manager.getAccessToken(),
            new ParameterizedTypeReference<ApiResponse<AuctionRoomResponse>>() {}
        );
        prepareAuctionRoomForBidding(roomId);

        ParticipateAuctionRoomRequest participateRequest = ParticipateAuctionRoomRequest.builder()
            .participatedReason("E2E bid interest")
            .build();

        postAuthenticated(
            "/api/v1/auctions/rooms/" + roomId + "/participate",
            participateRequest,
            bidderOne.getAccessToken(),
            new ParameterizedTypeReference<ApiResponse<Object>>() {}
        );
        postAuthenticated(
            "/api/v1/auctions/rooms/" + roomId + "/participate",
            participateRequest,
            bidderTwo.getAccessToken(),
            new ParameterizedTypeReference<ApiResponse<Object>>() {}
        );

        String baseUrl = "http://localhost:" + port;
        StompSession bidderSession = WebSocketTestHelper.connect(baseUrl, bidderOne.getAccessToken());
        WebSocketTestHelper.send(
            bidderSession,
            "/app/auction.bid.place",
            PlaceBidSocketRequest.builder()
                .auctionRoomId(roomId)
                .bidAmount(220.0)
                .build()
        );
        WebSocketTestHelper.disconnect(bidderSession);

        StompSession secondBidSession = WebSocketTestHelper.connect(baseUrl, bidderTwo.getAccessToken());
        WebSocketTestHelper.send(
            secondBidSession,
            "/app/auction.bid.place",
            PlaceBidSocketRequest.builder()
                .auctionRoomId(roomId)
                .bidAmount(240.0)
                .build()
        );
        WebSocketTestHelper.disconnect(secondBidSession);

        assertThat(auctionRoomRepository.findById(roomId)).isPresent();
        assertThat(bidRepository.findAll()).isNotEmpty();
    }
}
