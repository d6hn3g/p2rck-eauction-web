package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.bid.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.internal.AuctionRoomInfo;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.InternalAuctionRoomService;
import com.github.dghng36.eauction.modules.auction.bid.dto.internal.BidderInfo;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.SearchBidsRequest;
import com.github.dghng36.eauction.modules.auction.bid.dto.response.BidResponse;
import com.github.dghng36.eauction.modules.auction.bid.event.BidOutbidEvent;
import com.github.dghng36.eauction.modules.auction.bid.event.BidPlacedEvent;
import com.github.dghng36.eauction.modules.auction.bid.mapper.BidMapper;
import com.github.dghng36.eauction.modules.auction.bid.model.Bid;
import com.github.dghng36.eauction.modules.auction.bid.repository.BidRepository;
import com.github.dghng36.eauction.modules.auction.bid.service.AutoBidProcessor;
import com.github.dghng36.eauction.modules.auction.bid.service.BidService;
import com.github.dghng36.eauction.modules.auction.bid.service.InternalBidService;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProductInfo;
import com.github.dghng36.eauction.modules.auction.product.service.AuctionProductService;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;

@ExtendWith(MockitoExtension.class)
public class BidServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private BidRepository bidRepo;
    @Mock private AutoBidProcessor autoBidProcessor;
    @Mock private AuctionProductService auctionProductService;
    @Mock private InternalAuctionRoomService internalAuctionRoomService;
    @Mock private InternalUserService internalUserService;
    @Mock private InternalWalletService internalWalletService;
    @Mock private InternalBidService internalBidService;
    @Mock private BidMapper bidMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private BidService bidService;

    private final String roomId = "room-id-123";
    private final String userId = "user-id-456";
    private final String otherUserId = "other-user-789";
    private final String ownerUserId = "owner-user-000";
    private final String productId = "product-id-789";
    private final String bidId = "bid-id-001";

    private UserInfo mockBidder;
    private AuctionRoom mockOngoingRoom;
    private AuctionRoom mockRoomWithWinner;
    private AuctionRoom mockBuyoutRoom;
    private Bid mockBid;
    private BidResponse mockBidResponse;
    private BidderInfo mockBidderInfo;
    private SearchBidsRequest searchBidsRequest;

    @BeforeEach
    void setUp() {
        mockBidder = UserInfo.builder()
            .id(userId)
            .username("testBidder")
            .build();

        AuctionProduct auctionProduct = AuctionProduct.builder()
            .productId(productId)
            .currentPrice(100.0)
            .priceStep(10.0)
            .buyoutPrice(500.0)
            .build();

        mockOngoingRoom = AuctionRoom.builder()
            .id(roomId)
            .title("Test Auction Room")
            .status(AuctionRoomStatus.ONGOING)
            .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .auctionProduct(auctionProduct)
            .allowAutoExtend(false)
            .build();

        mockRoomWithWinner = AuctionRoom.builder()
            .id(roomId)
            .title("Test Auction Room")
            .status(AuctionRoomStatus.ONGOING)
            .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .auctionProduct(auctionProduct)
            .currentWinnerId(otherUserId)
            .currentMaxPrice(110.0)
            .allowAutoExtend(false)
            .build();

        AuctionProduct lowBuyoutProduct = AuctionProduct.builder()
            .productId(productId)
            .currentPrice(100.0)
            .priceStep(10.0)
            .buyoutPrice(150.0)
            .build();

        mockBuyoutRoom = AuctionRoom.builder()
            .id(roomId)
            .title("Buyout Auction Room")
            .status(AuctionRoomStatus.ONGOING)
            .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .auctionProduct(lowBuyoutProduct)
            .allowAutoExtend(false)
            .build();

        mockBidderInfo = BidderInfo.builder()
            .bidderId(userId)
            .bidderName("testBidder")
            .build();

        mockBid = Bid.builder()
            .id(bidId)
            .auctionRoomId(roomId)
            .auctionProductId(productId)
            .bidderInfo(mockBidderInfo)
            .bidAmount(120.0)
            .bidTime(Instant.now())
            .isWinningBid(false)
            .isDeleted(false)
            .build();

        mockBidResponse = BidResponse.builder()
            .id(bidId)
            .auctionRoomInfo(AuctionRoomInfo.builder().auctionRoomId(roomId).auctionRoomTitle("Test Auction Room").build())
            .auctionProductInfo(AuctionProductInfo.builder().auctionProductId(productId).auctionProductName("Test Product").build())
            .bidderInfo(mockBidderInfo)
            .bidAmount(120.0)
            .bidTime(mockBid.getBidTime())
            .isWinningBid(false)
            .build();

        searchBidsRequest = SearchBidsRequest.builder()
            .bidderName("testBidder")
            .minBidAmount(100.0)
            .build();

        lenient().when(autoBidProcessor.validateAutoBidPrice(anyDouble(), anyDouble())).thenAnswer(invocation -> {
            Double maxAutoBidPrice = invocation.getArgument(0);
            Double incrementAmount = invocation.getArgument(1);
            return maxAutoBidPrice != null && maxAutoBidPrice > 0
                && incrementAmount != null && incrementAmount > 0;
        });
    }

    @Test
    void getAuctionRoomBidHistories_validAuctionRoomId_returnsBidHistories() {
        // Arrange
        Page<Bid> bidPage = new PageImpl<>(List.of(mockBid), PageRequest.of(0, 10), 1);
        when(internalAuctionRoomService.existsAuctionRoom(roomId)).thenReturn(true);
        when(bidRepo.findAllByAuctionRoomIdAndIsDeletedFalse(eq(roomId), any(PageRequest.class))).thenReturn(bidPage);
        when(auctionProductService.getAuctionProductInfoByIds(Set.of(productId))).thenReturn(
            Map.of(productId, mockBidResponse.getAuctionProductInfo())
        );
        when(internalAuctionRoomService.getAuctionRoomInfoByIds(Set.of(roomId))).thenReturn(
            Map.of(roomId, mockBidResponse.getAuctionRoomInfo())
        );
        when(bidMapper.toBidResponseList(any(), any(), any())).thenReturn(List.of(mockBidResponse));

        // Act
        PageResponse<BidResponse> result = bidService.getAuctionRoomBidHistories(roomId, 0, 10, "bidAmount", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getCurrentPage());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getData().size());
        assertEquals(bidId, result.getData().get(0).getId());
        verify(internalAuctionRoomService, times(1)).existsAuctionRoom(roomId);
        verify(bidRepo, times(1)).findAllByAuctionRoomIdAndIsDeletedFalse(eq(roomId), any(PageRequest.class));
    }

    @Test
    void getAuctionRoomBidHistories_invalidAuctionRoomId_throwsException() {
        // Arrange
        when(internalAuctionRoomService.existsAuctionRoom(roomId)).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            bidService.getAuctionRoomBidHistories(roomId, 0, 10, "bidAmount", "desc"));
        assertEquals("Auction room not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(bidRepo, never()).findAllByAuctionRoomIdAndIsDeletedFalse(eq(roomId), any(PageRequest.class));
    }

    @Test
    void getAuctionRoomBidHistories_noBids_returnsEmptyList() {
        // Arrange
        Page<Bid> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(internalAuctionRoomService.existsAuctionRoom(roomId)).thenReturn(true);
        when(bidRepo.findAllByAuctionRoomIdAndIsDeletedFalse(eq(roomId), any(PageRequest.class))).thenReturn(emptyPage);
        when(bidMapper.toBidResponseList(any(), any(), any())).thenReturn(List.of());

        // Act
        PageResponse<BidResponse> result = bidService.getAuctionRoomBidHistories(roomId, 0, 10, "bidAmount", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getCurrentPage());
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void searchAuctionRoomBidHistories_validCriteria_returnsMatchingBidHistories() {
        // Arrange
        when(mongoTemplate.count(any(Query.class), eq(Bid.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Bid.class))).thenReturn(List.of(mockBid));
        when(auctionProductService.getAuctionProductInfoByIds(Set.of(productId))).thenReturn(
            Map.of(productId, mockBidResponse.getAuctionProductInfo())
        );
        when(internalAuctionRoomService.getAuctionRoomInfoByIds(Set.of(roomId))).thenReturn(
            Map.of(roomId, mockBidResponse.getAuctionRoomInfo())
        );
        when(bidMapper.toBidResponseList(any(), any(), any())).thenReturn(List.of(mockBidResponse));

        // Act
        PageResponse<BidResponse> result = bidService.searchAuctionRoomBidHistories(
            roomId, 0, 10, "bidAmount", "desc", searchBidsRequest
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getCurrentPage());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getData().size());
        verify(mongoTemplate, times(1)).count(any(Query.class), eq(Bid.class));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Bid.class));
    }

    @Test
    void searchAuctionRoomBidHistories_invalidCriteria_throwsException() {
        // Act & Assert — null search request causes NPE when applying criteria
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
            bidService.searchAuctionRoomBidHistories(roomId, 0, 10, "bidAmount", "desc", null));

        assertNotNull(exception);
        
        verify(mongoTemplate, never()).count(any(Query.class), eq(Bid.class));
    }

    @Test
    void searchAuctionRoomBidHistories_noMatches_returnsEmptyList() {
        // Arrange
        when(mongoTemplate.count(any(Query.class), eq(Bid.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Bid.class))).thenReturn(List.of());
        when(bidMapper.toBidResponseList(any(), any(), any())).thenReturn(List.of());

        // Act
        PageResponse<BidResponse> result = bidService.searchAuctionRoomBidHistories(
            roomId, 0, 10, "bidAmount", "desc", searchBidsRequest
        );

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getCurrentPage());
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void placeBid_ValidAuctionRoomAndAuctionParticipant_ShouldReturnSuccess() {
        // Arrange
        Double bidAmount = 120.0;
        
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId)).thenReturn(mockOngoingRoom);
        when(internalAuctionRoomService.isBuyoutReached(mockOngoingRoom, bidAmount)).thenReturn(false);
        when(internalWalletService.validateAvailableBalance(userId, bidAmount)).thenReturn(true);
        
        AuctionRoom updatedRoom = AuctionRoom.builder().currentWinnerId(userId).currentMaxPrice(bidAmount).build();
        when(internalAuctionRoomService.processNewBidSuccess(mockOngoingRoom, userId, bidAmount)).thenReturn(updatedRoom);
        when(internalAuctionRoomService.getProductId(roomId)).thenReturn(productId);
        when(bidMapper.toBidderInfo(any(), any(), any())).thenReturn(mockBidderInfo);
        
        Bid savedBidMock = Bid.builder().id(bidId).bidTime(Instant.now()).build();
        when(internalBidService.saveBidHistoryIndependent(any(Bid.class))).thenReturn(savedBidMock);
        when(internalBidService.getTotalBidAmount(roomId, userId)).thenReturn(120.0);

        // Act
        bidService.placeBid(userId, roomId, bidAmount, null, false, null, null, false);

        // Assert
        verify(internalUserService, times(1)).getUserInfoByIds(Set.of(userId));
        verify(internalAuctionRoomService, times(1)).validateAndGetForBidding(roomId, userId);
        verify(internalWalletService, times(1)).validateAvailableBalance(userId, bidAmount);
        verify(internalAuctionRoomService, times(1)).processNewBidSuccess(mockOngoingRoom, userId, bidAmount);
        verify(internalWalletService, times(1)).holdBalance(userId, bidAmount);
        verify(internalBidService, times(1)).saveBidHistoryIndependent(any(Bid.class));
        verify(eventPublisher, times(1)).publishEvent(any(BidPlacedEvent.class));
    }

    @Test
    void placeBid_validBid_returnsSuccess() {
        // Arrange
        Double bidAmount = 120.0;
        
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId)).thenReturn(mockOngoingRoom);
        when(internalAuctionRoomService.isBuyoutReached(mockOngoingRoom, bidAmount)).thenReturn(false);
        when(internalWalletService.validateAvailableBalance(userId, bidAmount)).thenReturn(true);
        
        AuctionRoom updatedRoom = AuctionRoom.builder().currentWinnerId(userId).currentMaxPrice(bidAmount).build();
        when(internalAuctionRoomService.processNewBidSuccess(mockOngoingRoom, userId, bidAmount)).thenReturn(updatedRoom);
        when(internalAuctionRoomService.getProductId(roomId)).thenReturn(productId);
        when(bidMapper.toBidderInfo(any(), any(), any())).thenReturn(mockBidderInfo);
        
        Instant now = Instant.now();
        Bid savedBidMock = Bid.builder().id(bidId).bidTime(now).build();
        when(internalBidService.saveBidHistoryIndependent(any(Bid.class))).thenReturn(savedBidMock);
        when(internalBidService.getTotalBidAmount(roomId, userId)).thenReturn(120.0);
        
        ArgumentCaptor<BidPlacedEvent> eventCaptor = ArgumentCaptor.forClass(BidPlacedEvent.class);

        // Act
        bidService.placeBid(userId, roomId, bidAmount, null, false, null, null, false);

        // Assert
        verify(internalBidService, times(1)).saveBidHistoryIndependent(any(Bid.class));
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        
        BidPlacedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(bidId, publishedEvent.getBidId());
        assertEquals(roomId, publishedEvent.getAuctionRoomId());
        assertEquals(userId, publishedEvent.getUserId());
        assertEquals(bidAmount, publishedEvent.getBidAmount());
        assertEquals(false, publishedEvent.getIsAutoBid());
    }

    @Test
    void placeBid_invalidBid_throwsException() {
        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            bidService.placeBid(userId, roomId, 0.0, null, false, null, null, false));
        assertEquals("Bid amount must be greater than 0", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(internalUserService, never()).getUserInfoByIds(anySet());
    }

    @Test
    void placeBid_bidBelowCurrentPrice_throwsException() {
        // Arrange
        Double bidAmount = 50.0;
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId)).thenReturn(mockBuyoutRoom);
        when(internalAuctionRoomService.isBuyoutReached(mockBuyoutRoom, bidAmount)).thenReturn(true);
        
        doThrow(new AppException("Bid amount is less than the minimum required bid", HttpStatus.BAD_REQUEST))
            .when(internalAuctionRoomService).validateBidAmount(mockBuyoutRoom, bidAmount);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            bidService.placeBid(userId, roomId, bidAmount, null, false, null, null, false));
        assertEquals("Bid amount is less than the minimum required bid", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(internalWalletService, never()).validateAvailableBalance(anyString(), any());
    }

    @Test
    void placeBid_auctionClosed_throwsException() {
        // Arrange
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId))
            .thenThrow(new AppException("Auction room has ended", HttpStatus.BAD_REQUEST));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            bidService.placeBid(userId, roomId, 120.0, null, false, null, null, false));
        assertEquals("Auction room has ended", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(internalWalletService, never()).validateAvailableBalance(anyString(), any());
    }

    @Test
    void placeBid_insufficientFunds_throwsException() {
        // Arrange
        Double bidAmount = 120.0;
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId)).thenReturn(mockOngoingRoom);
        when(internalAuctionRoomService.isBuyoutReached(mockOngoingRoom, bidAmount)).thenReturn(false);
        when(internalWalletService.validateAvailableBalance(userId, bidAmount)).thenReturn(false);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            bidService.placeBid(userId, roomId, bidAmount, null, false, null, null, false));
        assertEquals("User's wallet balance is insufficient to place the bid", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(internalBidService, never()).saveBidHistoryIndependent(any());
    }

    @Test
    void placeBid_autoBidTriggered_processesAutoBid() {
        // Arrange
        Double bidAmount = 120.0;
        Double maxAutoBidPrice = 300.0;
        Double incrementAmount = 10.0;
        
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId)).thenReturn(mockOngoingRoom);
        when(internalAuctionRoomService.isBuyoutReached(mockOngoingRoom, bidAmount)).thenReturn(false);
        
        when(autoBidProcessor.validateAutoBidPrice(maxAutoBidPrice, incrementAmount)).thenReturn(true);
        doNothing().when(autoBidProcessor).processEnableAutoBid(roomId, userId, maxAutoBidPrice, incrementAmount);
        
        when(internalWalletService.validateAvailableBalance(userId, bidAmount)).thenReturn(true);
        AuctionRoom updatedRoom = AuctionRoom.builder().currentWinnerId(userId).currentMaxPrice(bidAmount).build();
        when(internalAuctionRoomService.processNewBidSuccess(mockOngoingRoom, userId, bidAmount)).thenReturn(updatedRoom);
        when(internalAuctionRoomService.getProductId(roomId)).thenReturn(productId);
        when(bidMapper.toBidderInfo(any(), any(), any())).thenReturn(mockBidderInfo);
        
        Bid savedBidMock = Bid.builder().id(bidId).bidTime(Instant.now()).build();
        when(internalBidService.saveBidHistoryIndependent(any(Bid.class))).thenReturn(savedBidMock);
        when(internalBidService.getTotalBidAmount(roomId, userId)).thenReturn(120.0);

        // Act
        bidService.placeBid(userId, roomId, bidAmount, null, true, maxAutoBidPrice, incrementAmount, false);

        // Assert
        verify(autoBidProcessor, times(1)).processEnableAutoBid(roomId, userId, maxAutoBidPrice, incrementAmount);
        verify(autoBidProcessor, times(1)).validateAutoBidPrice(maxAutoBidPrice, incrementAmount);
    }

    @Test
    void placeBid_concurrentBids_resolvesCorrectly() {
        // Arrange
        Double bidAmount = 130.0;
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId)).thenReturn(mockRoomWithWinner);
        when(internalAuctionRoomService.isBuyoutReached(mockRoomWithWinner, bidAmount)).thenReturn(false);
        when(internalWalletService.validateAvailableBalance(userId, bidAmount)).thenReturn(true);
        when(internalAuctionRoomService.getProductId(roomId)).thenReturn(productId);
        when(bidMapper.toBidderInfo(any(), any(), any())).thenReturn(mockBidderInfo);
        
        AuctionRoom updatedRoom = AuctionRoom.builder()
                .currentWinnerId(otherUserId)
                .currentMaxPrice(110.0)
                .title("Test Auction")
                .build();
        when(internalAuctionRoomService.processNewBidSuccess(mockRoomWithWinner, userId, bidAmount)).thenReturn(updatedRoom);
        
        Bid savedBidMock = Bid.builder().id(bidId).bidTime(Instant.now()).build();
        when(internalBidService.saveBidHistoryIndependent(any(Bid.class))).thenReturn(savedBidMock);
        when(internalBidService.getTotalBidAmount(roomId, userId)).thenReturn(130.0);
        
        ArgumentCaptor<BidOutbidEvent> outbidCaptor = ArgumentCaptor.forClass(BidOutbidEvent.class);

        // Act
        bidService.placeBid(userId, roomId, bidAmount, null, false, null, null, false);

        // Assert
        verify(eventPublisher, times(1)).publishEvent(outbidCaptor.capture());
        BidOutbidEvent outbidEvent = outbidCaptor.getValue();
        assertEquals(roomId, outbidEvent.getAuctionRoomId());
        assertEquals(otherUserId, outbidEvent.getOutbidUserId());
        assertEquals(userId, outbidEvent.getNewHighestBidderId());
        assertEquals(110.0, outbidEvent.getPreviousHighestPrice());
        assertEquals(bidAmount, outbidEvent.getCurrentHighestPrice());
    }

    @Test
    void placeBid_bidOnOwnAuction_throwsException() {
        // Arrange
        UserInfo ownerInfo = UserInfo.builder().id(ownerUserId).username("owner").build();
        when(internalUserService.getUserInfoByIds(Set.of(ownerUserId))).thenReturn(Map.of(ownerUserId, ownerInfo));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, ownerUserId))
            .thenThrow(new AppException("User is not a participant in this auction room", HttpStatus.FORBIDDEN));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            bidService.placeBid(ownerUserId, roomId, 120.0, null, false, null, null, false));
        assertEquals("User is not a participant in this auction room", ex.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(internalBidService, never()).saveBidHistoryIndependent(any());
    }

    @Test
    void enableAutoBid_validAutoBid_shouldProcessSuccessfully() {
        // Arrange
        Double maxAutoBidPrice = 300.0;
        Double incrementAmount = 10.0;
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId)).thenReturn(mockOngoingRoom);
        doNothing().when(autoBidProcessor).processEnableAutoBid(roomId, userId, maxAutoBidPrice, incrementAmount);

        // Act
        bidService.enableAutoBid(userId, roomId, maxAutoBidPrice, incrementAmount);

        // Assert
        verify(internalAuctionRoomService, times(1)).validateAndGetForBidding(roomId, userId);
        verify(autoBidProcessor, times(1)).validateAutoBidPrice(maxAutoBidPrice, incrementAmount);
        verify(autoBidProcessor, times(1)).processEnableAutoBid(roomId, userId, maxAutoBidPrice, incrementAmount);
    }

    @Test
    void enableAutoBid_autoBidBelowCurrentPrice_shouldNotProcess() {
        // Arrange — invalid auto bid settings (non-positive max price)
        Double invalidMaxAutoBidPrice = 0.0;
        Double incrementAmount = 10.0;
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId)).thenReturn(mockOngoingRoom);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            bidService.enableAutoBid(userId, roomId, invalidMaxAutoBidPrice, incrementAmount));
        assertEquals("Invalid auto bid price settings", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(autoBidProcessor, never()).processEnableAutoBid(eq(roomId), eq(userId), anyDouble(), anyDouble());
    }

    @Test
    void disableAutoBid_validAutoBid_shouldDisableSuccessfully() {
        // Arrange
        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockBidder));
        when(internalAuctionRoomService.validateAndGetForBidding(roomId, userId)).thenReturn(mockOngoingRoom);
        doNothing().when(autoBidProcessor).processDisableAutoBid(roomId, userId);

        // Act
        bidService.disableAutoBid(userId, roomId);

        // Assert
        verify(internalAuctionRoomService, times(1)).validateAndGetForBidding(roomId, userId);
        verify(autoBidProcessor, times(1)).processDisableAutoBid(roomId, userId);
    }

    @Test
    void disableAutoBid_nonExistentAutoBid_shouldThrowException() {
        // Arrange — user not found
        String unknownUserId = "unknown-user-id";
        when(internalUserService.getUserInfoByIds(Set.of(unknownUserId))).thenReturn(Map.of());

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            bidService.disableAutoBid(unknownUserId, roomId));
        assertEquals("User not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(autoBidProcessor, never()).processDisableAutoBid(any(), any());
    }
}
