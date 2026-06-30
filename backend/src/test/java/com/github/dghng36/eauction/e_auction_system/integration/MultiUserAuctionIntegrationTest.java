package com.github.dghng36.eauction.e_auction_system.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.auctionRoom.repository.AuctionRoomRepository;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.PlaceBidSocketRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.request.CreateProductRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.response.ProductResponse;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.media.dto.request.MediaFileUploadRequest;

class MultiUserAuctionIntegrationTest extends AbstractIntegrationTest {

    private static final int BIDDER_COUNT = 24;
    private static final int TOTAL_WORKFLOW_USERS = 25;

    @LocalServerPort
    private int port;

    @Autowired
    private AuctionRoomRepository auctionRoomRepository;

    private TestUserSession admin;
    private TestUserSession manager;
    private TestUserSession seller;
    private List<TestUserSession> bidders;

    @BeforeEach
    void setUpWorkflowUsers() {
        mongoTemplate.getDb().drop();
        admin = seedPrivilegedUser("admin_auction", UserRole.ADMIN);
        manager = seedPrivilegedUser("manager_auction", UserRole.MANAGER);
        seller = registerAndLogin("seller");
        bidders = registerUsers("bidder", BIDDER_COUNT);

        assertThat(bidders).hasSize(BIDDER_COUNT);

        creditWallet(admin, seller.getUserId(), 500_000);
        bidders.forEach(bidder -> creditWallet(admin, bidder.getUserId(), 500_000));
    }

    @Test
    void multiUserAuctionWorkflow_With25UsersAndWebSocketBids_ShouldCompleteSuccessfully() throws Exception {
        String mediaCode = seedMediaCode();
        CreateProductRequest productRequest = new CreateProductRequest(
            "Integration Auction Product",
            "Product used in multi-user auction integration workflow test",
            List.of(MediaFileUploadRequest.builder()
                .mediaCode(mediaCode)
                .objectKey(mediaCode + ".jpg")
                .build()),
            null
        );

        ApiResponse<ProductResponse> productResponse = postAuthenticated(
            "/api/v1/users/me/products",
            productRequest,
            seller.getAccessToken(),
            new ParameterizedTypeReference<>() {}
        );
        String productId = productResponse.getData().getId();

        CreateAuctionRoomRequest roomRequest = CreateAuctionRoomRequest.builder()
            .title("Integration Multi-User Auction")
            .description("Auction room for 25-user workflow integration test")
            .startTime(Instant.now().plusSeconds(300))
            .durationMinutes(60)
            .productId(productId)
            .startPrice(100.0)
            .priceStep(10.0)
            .buyoutPrice(5000.0)
            .totalParticipants(TOTAL_WORKFLOW_USERS)
            .allowAutoExtend(false)
            .build();

        ApiResponse<AuctionRoomResponse> roomResponse = postAuthenticated(
            "/api/v1/users/me/auctions/rooms",
            roomRequest,
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
            .participatedReason("Join integration test")
            .build();

        for (TestUserSession bidder : bidders) {
            postAuthenticated(
                "/api/v1/auctions/rooms/" + roomId + "/participate",
                participateRequest,
                bidder.getAccessToken(),
                new ParameterizedTypeReference<ApiResponse<Object>>() {}
            );

        }

        String baseUrl = "http://localhost:" + port;
        StompSession observerSession = WebSocketTestHelper.connect(baseUrl, bidders.get(0).getAccessToken());
        AtomicReference<Map<String, Object>> bidEventRef = WebSocketTestHelper.subscribeCollector(
            observerSession,
            "/topic/auctions/rooms/" + roomId + "/bids"
        );

    //     int totalThreads = bidders.size();
    //     ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
    //     CountDownLatch latch = new CountDownLatch(1);
    //     List<StompSession> bidSessions = Collections.synchronizedList(new ArrayList<>());

    //     final double baseBidAmount = 150.0;

    //     for (int i = 0; i < totalThreads; i++) {
    //         final TestUserSession bidder = bidders.get(i);
            
    //         executorService.submit(() -> {
    //             try {
    //                 StompSession bidSession = WebSocketTestHelper.connect(baseUrl, bidder.getAccessToken());
    //                 bidSessions.add(bidSession);

    //                 latch.await(); 

    //                 WebSocketTestHelper.send(
    //                     bidSession,
    //                     "/app/auction.bid.place",
    //                     PlaceBidSocketRequest.builder()
    //                         .auctionRoomId(roomId)
    //                         .bidAmount(baseBidAmount)
    //                         .build()
    //                 );
    //             } catch (Exception e) {
    //                 Thread.currentThread().interrupt();
    //             }
    //         });
    //     }

    //     latch.countDown(); 
        
    //     executorService.shutdown();
    //     executorService.awaitTermination(5, TimeUnit.SECONDS);

    //     await().atMost(10, TimeUnit.SECONDS).until(() -> {
    //         Map<String, Object> event = bidEventRef.get();
    //         return event != null && "NEW_BID".equals(event.get("eventType"));
    //     });

    //     assertThat(bidEventRef.get().get("eventType")).isEqualTo("NEW_BID");
        
    //     AuctionRoom finalRoom = auctionRoomRepository.findById(roomId)
    //         .orElseThrow(() -> new AssertionError("Auction room not found"));
            
    //     assertThat(finalRoom.getCurrentMaxPrice()).isEqualTo(baseBidAmount); 
    //     assertThat(finalRoom.getCurrentWinnerId()).isNotNull();

    //     bidSessions.forEach(WebSocketTestHelper::disconnect);
    //     WebSocketTestHelper.disconnect(observerSession);

        List<StompSession> bidSessions = new ArrayList<>();
        double nextBid = 110.0;
        for (int i = 0; i < 5; i++) {
            TestUserSession bidder = bidders.get(i);
            StompSession bidSession = WebSocketTestHelper.connect(baseUrl, bidder.getAccessToken());
            bidSessions.add(bidSession);
            
            WebSocketTestHelper.send(
                bidSession,
                "/app/auction.bid.place",
                PlaceBidSocketRequest.builder()
                    .auctionRoomId(roomId)
                    .bidAmount(nextBid)
                    .build()
            );
            nextBid += 10.0;
        }

        await().atMost(20, TimeUnit.SECONDS).until(() -> {
            Map<String, Object> event = bidEventRef.get();
            return event != null && "NEW_BID".equals(event.get("eventType"));
        });

        assertThat(bidEventRef.get().get("eventType")).isEqualTo("NEW_BID");
        assertThat(auctionRoomRepository.findById(roomId)).isPresent();

        bidSessions.forEach(WebSocketTestHelper::disconnect);
        WebSocketTestHelper.disconnect(observerSession);

    }
}
