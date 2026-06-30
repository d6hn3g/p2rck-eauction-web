package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.auctionRoom.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.e_auction_system.unit.support.JobExecutorTasksMockHelper;
import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.CancelAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.CreateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.ParticipateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.SearchAuctionRoomsRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.UpdateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.UpdateAuctionRoomStatusRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomParticipantResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionCanceledEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.mapper.AuctionRoomMapper;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.auctionRoom.repository.AuctionRoomRepository;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomParticipantService;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomService;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.enums.ParticipantStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;
import com.github.dghng36.eauction.modules.auction.product.service.AuctionProductService;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.github.dghng36.eauction.modules.social.conversation.service.InternalConversationService;

@ExtendWith(MockitoExtension.class)
public class AuctionRoomServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private AuctionRoomRepository auctionRoomRepo;
    @Mock private AuctionRoomParticipantService auctionRoomParticipantService;
    @Mock private AuctionProductService auctionProductService;
    @Mock private InternalUserService internalUserService;
    @Mock private InternalConversationService internalConversationService;
    @Mock private InternalWalletService internalWalletService;
    @Mock private AuctionRoomMapper auctionRoomMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private JobExecutorTasks jobExecutorTasks;

    @InjectMocks private AuctionRoomService auctionRoomService;

    private static final Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
        "title",
        "status",
        "startTime",
        "endTime",
        "currentMaxPrice",
        "totalParticipants",
        "currentParticipants",
        "createdAt",
        "updatedAt"
    );

    private final String roomId = "room-id-123";
    private final String ownerId = "owner-id-456";
    private final String managerId = "manager-id-789";
    private final String participantId = "participant-id-101";
    private final String productId = "product-id-202";

    private AuctionProduct mockAuctionProduct;
    private AuctionRoom mockAuctionRoom;
    private AuctionRoomResponse mockAuctionRoomResponse;
    private UserInfo mockOwnerInfo;
    private UserInfo mockParticipantInfo;
    private CreateAuctionRoomRequest createAuctionRoomRequest;
    private SearchAuctionRoomsRequest emptySearchRequest;

    @BeforeEach
    void setUp() {
        JobExecutorTasksMockHelper.runSynchronously(jobExecutorTasks);

        mockAuctionProduct = AuctionProduct.builder()
            .productId(productId)
            .currentPrice(100.0)
            .priceStep(10.0)
            .buyoutPrice(500.0)
            .build();

        mockAuctionRoom = AuctionRoom.builder()
            .id(roomId)
            .title("Test Auction Room")
            .description("Test auction room description")
            .ownerId(ownerId)
            .managerId(managerId)
            .status(AuctionRoomStatus.UPCOMING)
            .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
            .auctionProduct(mockAuctionProduct)
            .currentMaxPrice(100.0)
            .totalParticipants(10)
            .currentParticipants(2)
            .allowAutoExtend(false)
            .chatEnabled(false)
            .isDeleted(false)
            .build();

        mockOwnerInfo = UserInfo.builder()
            .id(ownerId)
            .username("owner_user")
            .build();

        mockParticipantInfo = UserInfo.builder()
            .id(participantId)
            .username("participant_user")
            .build();

        mockAuctionRoomResponse = AuctionRoomResponse.builder()
            .id(roomId)
            .title(mockAuctionRoom.getTitle())
            .owner(mockOwnerInfo)
            .status(AuctionRoomStatus.UPCOMING.name())
            .build();

        createAuctionRoomRequest = CreateAuctionRoomRequest.builder()
            .title("New Auction Room")
            .description("New auction room description")
            .productId(productId)
            .startPrice(100.0)
            .priceStep(10.0)
            .buyoutPrice(500.0)
            .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .durationMinutes(60)
            .totalParticipants(50)
            .chatEnabled(false)
            .build();

        emptySearchRequest = SearchAuctionRoomsRequest.builder().build();
    }

    // getAuctionRooms

    @Test
    void getAuctionRooms_Success_ShouldReturnPageResponse() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        Sort expectedSort = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(page, size, expectedSort);
        List<AuctionRoom> rooms = List.of(mockAuctionRoom);

        when(auctionRoomRepo.findAllByIsDeletedFalse(expectedPageRequest))
            .thenReturn(new PageImpl<>(rooms, expectedPageRequest, rooms.size()));
        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of(ownerId, mockOwnerInfo));
        when(auctionRoomMapper.toAuctionRoomResponseList(rooms, Map.of(ownerId, mockOwnerInfo)))
            .thenReturn(List.of(mockAuctionRoomResponse));

        PageResponse<AuctionRoomResponse> result = auctionRoomService.getAuctionRooms(page, size, sortBy, sortDirection);

        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(size);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getData()).containsExactly(mockAuctionRoomResponse);

        verify(auctionRoomRepo, times(1)).findAllByIsDeletedFalse(expectedPageRequest);
    }

    @Test
    void getAuctionRooms_EmptyResult_ShouldReturnCurrentPageZero() {
        Sort expectedSort = SortUtils.buildSort("createdAt", "desc", ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(0, 10, expectedSort);

        when(auctionRoomRepo.findAllByIsDeletedFalse(expectedPageRequest)).thenReturn(Page.empty());
        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of());
        when(auctionRoomMapper.toAuctionRoomResponseList(any(), any())).thenReturn(List.of());

        PageResponse<AuctionRoomResponse> result = auctionRoomService.getAuctionRooms(0, 10, "createdAt", "desc");

        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getData()).isEmpty();
    }

    // searchAuctionRooms

    @Test
    void searchAuctionRooms_Success_ShouldReturnPageResponse() {
        List<AuctionRoom> rooms = List.of(mockAuctionRoom);

        when(mongoTemplate.count(any(Query.class), eq(AuctionRoom.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(AuctionRoom.class))).thenReturn(rooms);
        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of(ownerId, mockOwnerInfo));
        when(auctionRoomMapper.toAuctionRoomResponseList(rooms, Map.of(ownerId, mockOwnerInfo)))
            .thenReturn(List.of(mockAuctionRoomResponse));

        PageResponse<AuctionRoomResponse> result = auctionRoomService.searchAuctionRooms(
            emptySearchRequest, 0, 10, "createdAt", "desc"
        );

        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getData()).containsExactly(mockAuctionRoomResponse);
    }

    @Test
    void searchAuctionRooms_EmptyResult_ShouldReturnCurrentPageZero() {
        when(mongoTemplate.count(any(Query.class), eq(AuctionRoom.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(AuctionRoom.class))).thenReturn(List.of());
        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of());
        when(auctionRoomMapper.toAuctionRoomResponseList(any(), any())).thenReturn(List.of());

        PageResponse<AuctionRoomResponse> result = auctionRoomService.searchAuctionRooms(
            emptySearchRequest, 0, 10, "createdAt", "desc"
        );

        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void searchAuctionRooms_InvalidTimeRange_ShouldThrowAppException() {
        SearchAuctionRoomsRequest invalidStatusRequest = SearchAuctionRoomsRequest.builder()
            .statuses(List.of("INVALID_STATUS"))
            .build();

        assertThatThrownBy(() -> auctionRoomService.searchAuctionRooms(
            invalidStatusRequest, 0, 10, "createdAt", "desc"
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Invalid auction room status")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(mongoTemplate, never()).count(any(Query.class), eq(AuctionRoom.class));
    }

    // participateAuctionRoom

    @Test
    void participateAuctionRoom_Success_ShouldJoinSuccessfully() {
        ParticipateAuctionRoomRequest participateRequest = ParticipateAuctionRoomRequest.builder()
            .participatedReason("I want to join")
            .build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(internalWalletService.validateAvailableBalance(participantId, mockAuctionRoom.getCurrentMaxPrice()))
            .thenReturn(true);
        when(auctionRoomParticipantService.existsParticipant(participantId, roomId)).thenReturn(false);
        doNothing().when(auctionRoomParticipantService).createParticipant(
            eq(roomId), eq(participantId), eq(participateRequest.getParticipatedReason())
        );
        doNothing().when(internalUserService).incReputationJoined(anyString(), anyString());
        doNothing().when(internalUserService).incrementUserAuctionMetric(anyString(), anyString(), anyLong());
        when(internalUserService.getUserInfoByIds(Set.of(participantId)))
            .thenReturn(Map.of(participantId, mockParticipantInfo));

        AuctionRoomParticipantResponse result = auctionRoomService.participateAuctionRoom(
            participantId, UserRole.USER, roomId, participateRequest
        );

        assertThat(result.getAuctionRoomId()).isEqualTo(roomId);
        assertThat(result.getUserId()).isEqualTo(participantId);
        assertThat(result.getUsername()).isEqualTo(mockParticipantInfo.getUsername());
        assertThat(result.getStatus()).isEqualTo(ParticipantStatus.PENDING.name());

        verify(auctionRoomParticipantService, times(1)).createParticipant(
            roomId, participantId, participateRequest.getParticipatedReason()
        );
    }

    @Test
    void participateAuctionRoom_AuctionRoomNotFound_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionRoomService.participateAuctionRoom(
            participantId, UserRole.USER, roomId, ParticipateAuctionRoomRequest.builder().build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Auction room not found")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void participateAuctionRoom_UserAlreadyJoined_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(internalWalletService.validateAvailableBalance(participantId, mockAuctionRoom.getCurrentMaxPrice()))
            .thenReturn(true);
        when(auctionRoomParticipantService.existsParticipant(participantId, roomId)).thenReturn(true);
        when(auctionRoomParticipantService.isParticipantInsideAuctionRoom(participantId, roomId)).thenReturn(true);

        assertThatThrownBy(() -> auctionRoomService.participateAuctionRoom(
            participantId, UserRole.USER, roomId, ParticipateAuctionRoomRequest.builder().build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("User has already joined the auction room");
    }

    @Test
    void participateAuctionRoom_InsufficientWalletBalance_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(internalWalletService.validateAvailableBalance(participantId, mockAuctionRoom.getCurrentMaxPrice()))
            .thenReturn(false);

        assertThatThrownBy(() -> auctionRoomService.participateAuctionRoom(
            participantId, UserRole.USER, roomId, ParticipateAuctionRoomRequest.builder().build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("User's wallet balance is insufficient")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(auctionRoomParticipantService, never()).createParticipant(anyString(), anyString(), anyString());
    }

    // leaveAuctionRoom

    @Test
    void leaveAuctionRoom_Success_ShouldLeaveSuccessfully() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(auctionRoomParticipantService.isParticipantInsideAuctionRoom(participantId, roomId)).thenReturn(true);
        when(auctionRoomParticipantService.isParticipantLeftAuctionRoom(participantId, roomId)).thenReturn(false);
        doNothing().when(auctionRoomParticipantService).leaveAuctionRoom(roomId, participantId);
        when(auctionRoomRepo.save(mockAuctionRoom)).thenReturn(mockAuctionRoom);
        when(internalUserService.getUserInfoByIds(Set.of(participantId)))
            .thenReturn(Map.of(participantId, mockParticipantInfo));

        AuctionRoomParticipantResponse result = auctionRoomService.leaveAuctionRoom(participantId, roomId);

        assertThat(result.getStatus()).isEqualTo(ParticipantStatus.LEFT.name());
        assertThat(result.getUserId()).isEqualTo(participantId);
        verify(auctionRoomParticipantService, times(1)).leaveAuctionRoom(roomId, participantId);
        verify(auctionRoomRepo, times(1)).save(mockAuctionRoom);
    }

    @Test
    void leaveAuctionRoom_AuctionRoomNotFound_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionRoomService.leaveAuctionRoom(participantId, roomId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Auction room not found");
    }

    @Test
    void leaveAuctionRoom_UserNotParticipant_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(auctionRoomParticipantService.isParticipantInsideAuctionRoom(participantId, roomId)).thenReturn(false);

        assertThatThrownBy(() -> auctionRoomService.leaveAuctionRoom(participantId, roomId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("User is not a participant of the auction room");
    }

    @Test
    void leaveAuctionRoom_UserAlreadyLeft_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(auctionRoomParticipantService.isParticipantInsideAuctionRoom(participantId, roomId)).thenReturn(true);
        when(auctionRoomParticipantService.isParticipantLeftAuctionRoom(participantId, roomId)).thenReturn(true);

        assertThatThrownBy(() -> auctionRoomService.leaveAuctionRoom(participantId, roomId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("User has already left the auction room");
    }

    @Test
    void leaveAuctionRoom_CurrentWinnerLeave_ShouldThrowAppException() {
        AuctionRoom roomWithWinner = AuctionRoom.builder()
            .id(roomId)
            .ownerId(ownerId)
            .currentWinnerId(participantId)
            .currentParticipants(2)
            .build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(roomWithWinner));
        when(auctionRoomParticipantService.isParticipantInsideAuctionRoom(participantId, roomId)).thenReturn(true);
        when(auctionRoomParticipantService.isParticipantLeftAuctionRoom(participantId, roomId)).thenReturn(false);

        assertThatThrownBy(() -> auctionRoomService.leaveAuctionRoom(participantId, roomId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Current winner cannot leave the auction room");
    }

    // createMyAuctionRoom

    @Test
    void createMyAuctionRoom_Success_ShouldCreateSuccessfully() {
        when(auctionRoomRepo.existsByTitleAndOwnerIdAndIsDeletedFalse(createAuctionRoomRequest.getTitle(), ownerId))
            .thenReturn(false);
        when(auctionProductService.createAuctionProduct(
            productId,
            createAuctionRoomRequest.getStartPrice(),
            createAuctionRoomRequest.getPriceStep(),
            createAuctionRoomRequest.getBuyoutPrice()
        )).thenReturn(mockAuctionProduct);
        when(auctionRoomMapper.toAuctionRoomEntity(
            anyString(), anyString(), eq(ownerId), eq(mockAuctionProduct), any(),
            any(), any(), any(Boolean.class), anyInt(), any(Boolean.class), any(), anyInt()
        )).thenReturn(mockAuctionRoom);
        when(auctionRoomRepo.save(mockAuctionRoom)).thenReturn(mockAuctionRoom);
        doNothing().when(internalUserService).incrementUserAuctionMetric(anyString(), anyString(), anyLong());
        doNothing().when(internalUserService).incReputationCreated(anyString(), anyString());
        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of(ownerId, mockOwnerInfo));
        when(auctionRoomMapper.toAuctionRoomResponse(mockAuctionRoom, Map.of(ownerId, mockOwnerInfo)))
            .thenReturn(mockAuctionRoomResponse);

        AuctionRoomResponse result = auctionRoomService.createMyAuctionRoom(ownerId, createAuctionRoomRequest);

        assertThat(result).isEqualTo(mockAuctionRoomResponse);
        verify(auctionRoomRepo, times(1)).save(mockAuctionRoom);
        verify(internalUserService, times(1)).incrementUserAuctionMetric(ownerId, "CREATED", 1L);
        verify(internalUserService, times(1)).incReputationCreated(ownerId, roomId);
    }

    @Test
    void createMyAuctionRoom_AuctionProductNotFound_ShouldThrowAppException() {
        when(auctionRoomRepo.existsByTitleAndOwnerIdAndIsDeletedFalse(createAuctionRoomRequest.getTitle(), ownerId))
            .thenReturn(false);
        when(auctionProductService.createAuctionProduct(
            productId,
            createAuctionRoomRequest.getStartPrice(),
            createAuctionRoomRequest.getPriceStep(),
            createAuctionRoomRequest.getBuyoutPrice()
        )).thenReturn(null);

        assertThatThrownBy(() -> auctionRoomService.createMyAuctionRoom(ownerId, createAuctionRoomRequest))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Product not found or is deleted")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(auctionRoomRepo, never()).save(any());
    }

    @Test
    void createMyAuctionRoom_InvalidAuctionTime_ShouldThrowAppException() {
        when(auctionRoomRepo.existsByTitleAndOwnerIdAndIsDeletedFalse(createAuctionRoomRequest.getTitle(), ownerId))
            .thenReturn(true);

        assertThatThrownBy(() -> auctionRoomService.createMyAuctionRoom(ownerId, createAuctionRoomRequest))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Auction room with the same title already exists")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createMyAuctionRoom_ConversationCreationFailed_ShouldThrowAppException() {
        CreateAuctionRoomRequest chatEnabledRequest = CreateAuctionRoomRequest.builder()
            .title("Chat Enabled Room")
            .description("Auction room with chat enabled")
            .productId(productId)
            .startPrice(100.0)
            .priceStep(10.0)
            .buyoutPrice(500.0)
            .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .durationMinutes(60)
            .chatEnabled(true)
            .build();

        AuctionRoom savedRoom = AuctionRoom.builder()
            .id(roomId)
            .title(chatEnabledRequest.getTitle())
            .ownerId(ownerId)
            .build();

        when(auctionRoomRepo.existsByTitleAndOwnerIdAndIsDeletedFalse(chatEnabledRequest.getTitle(), ownerId))
            .thenReturn(false);
        when(auctionProductService.createAuctionProduct(anyString(), any(), any(), any()))
            .thenReturn(mockAuctionProduct);
        when(auctionRoomMapper.toAuctionRoomEntity(
            anyString(), anyString(), eq(ownerId), eq(mockAuctionProduct), any(),
            any(), any(), any(Boolean.class), anyInt(), eq(true), any(), anyInt()
        )).thenReturn(savedRoom);
        when(auctionRoomRepo.save(savedRoom)).thenReturn(savedRoom);
        when(internalConversationService.createAuctionRoomConversation(roomId, savedRoom.getTitle(), ownerId))
            .thenReturn(null);

        assertThatThrownBy(() -> auctionRoomService.createMyAuctionRoom(ownerId, chatEnabledRequest))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Failed to create conversation for the auction room")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void createMyAuctionRoom_ReputationProcessThrowsException_ShouldStillReturnSuccessAndCatchError() {
        // Arrange
        when(auctionRoomRepo.existsByTitleAndOwnerIdAndIsDeletedFalse(createAuctionRoomRequest.getTitle(), ownerId))
            .thenReturn(false);
        when(auctionProductService.createAuctionProduct(anyString(), any(), any(), any()))
            .thenReturn(mockAuctionProduct);

        AuctionRoom realAuctionRoom = AuctionRoom.builder()
            .id(roomId)
            .ownerId(ownerId)
            .title(createAuctionRoomRequest.getTitle())
            .chatEnabled(createAuctionRoomRequest.isChatEnabled())
            .build();

        
        when(auctionRoomMapper.toAuctionRoomEntity(
            anyString(), anyString(), eq(ownerId), eq(mockAuctionProduct), any(),
            any(), any(), any(Boolean.class), anyInt(), any(Boolean.class), any(), anyInt()
        )).thenReturn(realAuctionRoom);
        
        when(auctionRoomRepo.save(realAuctionRoom)).thenReturn(realAuctionRoom);
        
        doNothing().when(internalUserService).incrementUserAuctionMetric(anyString(), anyString(), anyLong());
        
        doThrow(new AppException("User not found", HttpStatus.NOT_FOUND))
            .when(internalUserService).incReputationCreated(ownerId, roomId);

        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of());
        when(auctionRoomMapper.toAuctionRoomResponse(any(), any())).thenReturn(mockAuctionRoomResponse);

        // Act & Assert
        AuctionRoomResponse response = Assertions.assertDoesNotThrow(() -> 
            auctionRoomService.createMyAuctionRoom(ownerId, createAuctionRoomRequest)
        );

        assertThat(response).isNotNull();
        
        verify(internalUserService, times(1)).incReputationCreated(ownerId, roomId);
    }

    @Test
    void createMyAuctionRoom_MetricProcessThrowsException_ShouldStillReturnSuccessAndCatchError() {
        // Arrange
        when(auctionRoomRepo.existsByTitleAndOwnerIdAndIsDeletedFalse(createAuctionRoomRequest.getTitle(), ownerId))
            .thenReturn(false);
        when(auctionProductService.createAuctionProduct(anyString(), any(), any(), any()))
            .thenReturn(mockAuctionProduct);

        AuctionRoom realAuctionRoom = AuctionRoom.builder()
            .id(roomId)
            .ownerId(ownerId)
            .title(createAuctionRoomRequest.getTitle())
            .chatEnabled(createAuctionRoomRequest.isChatEnabled())
            .build();

        when(auctionRoomMapper.toAuctionRoomEntity(
            anyString(), anyString(), eq(ownerId), eq(mockAuctionProduct), any(),
            any(), any(), any(Boolean.class), anyInt(), any(Boolean.class), any(), anyInt()
        )).thenReturn(realAuctionRoom);
        
        when(auctionRoomRepo.save(realAuctionRoom)).thenReturn(realAuctionRoom);
        
        doThrow(new AppException("Failed to freeze wallet balance", HttpStatus.BAD_REQUEST))
            .when(internalUserService).incrementUserAuctionMetric(ownerId, "CREATED", 1L);

        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of());
        when(auctionRoomMapper.toAuctionRoomResponse(any(), any())).thenReturn(mockAuctionRoomResponse);

        // Act & Assert
        AuctionRoomResponse response = Assertions.assertDoesNotThrow(() -> 
            auctionRoomService.createMyAuctionRoom(ownerId, createAuctionRoomRequest)
        );

        assertThat(response).isNotNull();
        
        verify(internalUserService, times(1)).incrementUserAuctionMetric(ownerId, "CREATED", 1L);
        verify(internalUserService, never()).incReputationCreated(anyString(), anyString());
    }

    // updateMyAuctionRoom

    @Test
    void updateMyAuctionRoom_Success_ShouldUpdateSuccessfully() {
        UpdateAuctionRoomRequest updateRequest = UpdateAuctionRoomRequest.builder()
            .title(mockAuctionRoom.getTitle())
            .description("Updated auction room description text")
            .build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(auctionRoomRepo.save(mockAuctionRoom)).thenReturn(mockAuctionRoom);
        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of(ownerId, mockOwnerInfo));
        when(auctionRoomMapper.toAuctionRoomResponse(mockAuctionRoom, Map.of(ownerId, mockOwnerInfo)))
            .thenReturn(mockAuctionRoomResponse);

        AuctionRoomResponse result = auctionRoomService.updateMyAuctionRoom(ownerId, roomId, updateRequest);

        assertThat(result).isEqualTo(mockAuctionRoomResponse);
        verify(auctionRoomRepo, times(1)).save(mockAuctionRoom);
    }

    @Test
    void updateMyAuctionRoom_AuctionRoomNotFound_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionRoomService.updateMyAuctionRoom(
            ownerId, roomId, UpdateAuctionRoomRequest.builder().title("New Title").build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Auction room not found");
    }

    @Test
    void updateMyAuctionRoom_NotAuctionRoomOwner_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));

        assertThatThrownBy(() -> auctionRoomService.updateMyAuctionRoom(
            "other-user-id", roomId, UpdateAuctionRoomRequest.builder().title("New Title").build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("You are not the owner of this auction room")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateMyAuctionRoom_InvalidAuctionTime_ShouldThrowAppException() {
        UpdateAuctionRoomRequest updateRequest = UpdateAuctionRoomRequest.builder()
            .title(mockAuctionRoom.getTitle())
            .chatEnabled(true)
            .build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(internalConversationService.createAuctionRoomConversation(roomId, mockAuctionRoom.getTitle(), ownerId))
            .thenReturn(null);

        assertThatThrownBy(() -> auctionRoomService.updateMyAuctionRoom(ownerId, roomId, updateRequest))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Failed to create conversation for the auction room");
    }

    // cancelMyAuctionRoom

    @Test
    void cancelMyAuctionRoom_Success_ShouldCancelSuccessfully() {
        CancelAuctionRoomRequest cancelRequest = CancelAuctionRoomRequest.builder()
            .cancelReason("No longer needed")
            .build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(auctionRoomRepo.save(mockAuctionRoom)).thenReturn(mockAuctionRoom);
        when(auctionRoomParticipantService.getActiveParticipantIds(roomId))
            .thenReturn(List.of(participantId));

        auctionRoomService.cancelMyAuctionRoom(ownerId, roomId, cancelRequest);

        ArgumentCaptor<AuctionRoom> roomCaptor = ArgumentCaptor.forClass(AuctionRoom.class);
        verify(auctionRoomRepo).save(roomCaptor.capture());

        AuctionRoom savedRoom = roomCaptor.getValue();
        assertThat(savedRoom.getStatus()).isEqualTo(AuctionRoomStatus.CANCELLED);
        assertThat(savedRoom.getCancelReason()).isEqualTo(cancelRequest.getCancelReason());
        assertThat(savedRoom.getCanceledAt()).isNotNull();

        ArgumentCaptor<AuctionCanceledEvent> eventCaptor = ArgumentCaptor.forClass(AuctionCanceledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getParticipantIds()).containsExactly(participantId);
    }

    @Test
    void cancelMyAuctionRoom_AuctionRoomNotFound_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionRoomService.cancelMyAuctionRoom(
            ownerId, roomId, CancelAuctionRoomRequest.builder().cancelReason("reason").build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Auction room not found");
    }

    // updateManagerAuctionRoom

    @Test
    void updateManagerAuctionRoom_Success_ShouldUpdateSuccessfully() {
        AuctionRoom unassignedRoom = AuctionRoom.builder()
            .id(roomId)
            .title(mockAuctionRoom.getTitle())
            .ownerId(ownerId)
            .managerId(null)
            .build();

        UserInfo managerInfo = UserInfo.builder().id(managerId).username("manager_user").build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(unassignedRoom));
        when(auctionRoomRepo.save(unassignedRoom)).thenReturn(unassignedRoom);
        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of(ownerId, mockOwnerInfo, managerId, managerInfo));
        when(auctionRoomMapper.toAuctionRoomResponse(unassignedRoom, Map.of(ownerId, mockOwnerInfo, managerId, managerInfo)))
            .thenReturn(mockAuctionRoomResponse);

        AuctionRoomResponse result = auctionRoomService.updateManagerAuctionRoom(
            managerId, UserRole.MANAGER, roomId
        );

        assertThat(result).isEqualTo(mockAuctionRoomResponse);
        assertThat(unassignedRoom.getManagerId()).isEqualTo(managerId);
        verify(auctionRoomRepo, times(1)).save(unassignedRoom);
    }

    @Test
    void updateManagerAuctionRoom_AuctionRoomNotFound_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionRoomService.updateManagerAuctionRoom(
            managerId, UserRole.MANAGER, roomId
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Auction room not found");
    }

    @Test
    void updateManagerAuctionRoom_NotAuctionRoomManager_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));

        assertThatThrownBy(() -> auctionRoomService.updateManagerAuctionRoom(
            "another-manager-id", UserRole.MANAGER, roomId
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Manager already assigned for this auction room");
    }

    // updateAuctionRoomStatus

    @Test
    void updateAuctionRoomStatus_Success_ShouldUpdateSuccessfully() {
        UpdateAuctionRoomStatusRequest statusRequest = UpdateAuctionRoomStatusRequest.builder()
            .newAuctionRoomStatus("ONGOING")
            .build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));
        when(auctionRoomRepo.save(mockAuctionRoom)).thenReturn(mockAuctionRoom);
        when(internalUserService.getUserInfoByIds(any())).thenReturn(Map.of(ownerId, mockOwnerInfo, managerId, mockParticipantInfo));
        when(auctionRoomMapper.toAuctionRoomResponse(eq(mockAuctionRoom), anyMap()))
            .thenReturn(mockAuctionRoomResponse);

        AuctionRoomResponse result = auctionRoomService.updateAuctionRoomStatus(
            managerId, roomId, statusRequest
        );

        assertThat(result).isEqualTo(mockAuctionRoomResponse);
        assertThat(mockAuctionRoom.getStatus()).isEqualTo(AuctionRoomStatus.ONGOING);
        verify(auctionRoomRepo, times(1)).save(mockAuctionRoom);
    }

    @Test
    void updateAuctionRoomStatus_AuctionRoomNotFound_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionRoomService.updateAuctionRoomStatus(
            managerId, roomId, UpdateAuctionRoomStatusRequest.builder().newAuctionRoomStatus("ONGOING").build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Auction room not found");
    }

    @Test
    void updateAuctionRoomStatus_InvalidStatusTransition_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));

        assertThatThrownBy(() -> auctionRoomService.updateAuctionRoomStatus(
            managerId, roomId,
            UpdateAuctionRoomStatusRequest.builder().newAuctionRoomStatus("INVALID_STATUS").build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Invalid auction room status");
    }

    @Test
    void updateAuctionRoomStatus_UnauthorizedStatusUpdate_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(mockAuctionRoom));

        assertThatThrownBy(() -> auctionRoomService.updateAuctionRoomStatus(
            "unauthorized-manager-id", roomId,
            UpdateAuctionRoomStatusRequest.builder().newAuctionRoomStatus("ONGOING").build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("You are not the manager of this auction room")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // deleteAuctionRoom

    @Test
    void deleteAuctionRoom_Success_ShouldDeleteSuccessfully() {
        AuctionRoom endedRoom = AuctionRoom.builder()
            .id(roomId)
            .ownerId(ownerId)
            .managerId(managerId)
            .status(AuctionRoomStatus.ENDED)
            .isDeleted(false)
            .build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(endedRoom));
        when(auctionRoomRepo.save(endedRoom)).thenReturn(endedRoom);

        auctionRoomService.deleteAuctionRoom(managerId, roomId);

        ArgumentCaptor<AuctionRoom> roomCaptor = ArgumentCaptor.forClass(AuctionRoom.class);
        verify(auctionRoomRepo).save(roomCaptor.capture());

        AuctionRoom deletedRoom = roomCaptor.getValue();
        assertThat(deletedRoom.getIsDeleted()).isTrue();
        assertThat(deletedRoom.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteAuctionRoom_AuctionRoomNotFound_ShouldThrowAppException() {
        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionRoomService.deleteAuctionRoom(managerId, roomId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Auction room not found");
    }

    @Test
    void deleteAuctionRoom_WithAnotherStatus_ShouldThrowAppException() {
        AuctionRoom ongoingRoom = AuctionRoom.builder()
            .id(roomId)
            .managerId(managerId)
            .status(AuctionRoomStatus.ONGOING)
            .build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(ongoingRoom));

        assertThatThrownBy(() -> auctionRoomService.deleteAuctionRoom(managerId, roomId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Only ended or cancelled auction rooms can be deleted");
    }

    @Test
    void deleteAuctionRoom_UnauthorizedDelete_ShouldThrowAppException() {
        AuctionRoom endedRoom = AuctionRoom.builder()
            .id(roomId)
            .managerId(managerId)
            .status(AuctionRoomStatus.ENDED)
            .build();

        when(auctionRoomRepo.findByIdAndIsDeletedFalse(roomId)).thenReturn(Optional.of(endedRoom));

        assertThatThrownBy(() -> auctionRoomService.deleteAuctionRoom("other-manager-id", roomId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("You are not authorized to delete this auction room")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
