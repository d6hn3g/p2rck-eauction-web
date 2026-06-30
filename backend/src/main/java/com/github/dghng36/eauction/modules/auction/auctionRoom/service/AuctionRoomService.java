package com.github.dghng36.eauction.modules.auction.auctionRoom.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.Document;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.core.utils.MetadataUtils;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.CancelAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.CreateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.ParticipateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.SearchAuctionRoomsRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.UpdateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.UpdateAuctionRoomStatusRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.UpdateParticipantStatusRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomParticipantResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.event.AuctionCanceledEvent;
import com.github.dghng36.eauction.modules.auction.auctionRoom.mapper.AuctionRoomMapper;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.auctionRoom.repository.AuctionRoomRepository;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.enums.ParticipantStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;
import com.github.dghng36.eauction.modules.auction.product.service.AuctionProductService;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.github.dghng36.eauction.modules.identity.enums.UserAuctionStatus;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.github.dghng36.eauction.modules.social.conversation.service.InternalConversationService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuctionRoomService {
    MongoTemplate mongoTemplate;
    AuctionRoomRepository auctionRoomRepo;

    AuctionRoomParticipantService auctionRoomParticipantService;
    AuctionProductService auctionProductService;

    InternalUserService internalUserService;
    InternalConversationService internalConversationService;
    InternalWalletService internalWalletService;

    AuctionRoomMapper auctionRoomMapper;

    ApplicationEventPublisher eventPublisher;

    JobExecutorTasks jobExecutorTasks;

    static final Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
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

    // Public auction room methods
    public PageResponse<AuctionRoomResponse> getAuctionRooms(int page, int size, String sortBy, String sortDirection) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<AuctionRoom> auctionRoomPage = auctionRoomRepo.findAllByIsDeletedFalse(
            PageRequest.of(page, size, sortBuilt)
        );

        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(
            auctionRoomPage.getContent().stream()
                .flatMap(room -> Stream.of(room.getOwnerId(), room.getManagerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );
        
        return PageResponse.<AuctionRoomResponse>builder()
            .currentPage(auctionRoomPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(auctionRoomPage.getTotalPages())
            .totalElements(auctionRoomPage.getTotalElements())
            .data(auctionRoomMapper.toAuctionRoomResponseList(
                auctionRoomPage.getContent(), userInfoMaps
            ))
            .build();
    }

    public PageResponse<AuctionRoomResponse> searchAuctionRooms(SearchAuctionRoomsRequest searchAuctionRoomsRequest, int page, int size, String sortBy, String sortDirection) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        applyCriteria(criteriaList, searchAuctionRoomsRequest);
        return applyExecuteQuery(query, criteriaList, page, size, sortBy, sortDirection);
    }

    public AuctionRoomResponse getAuctionRoom(String id) {
        AuctionRoom auctionRoom = auctionRoomRepo.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new AppException("Auction room not found", HttpStatus.NOT_FOUND));

        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(
            Stream.of(auctionRoom.getOwnerId(), auctionRoom.getManagerId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

        return auctionRoomMapper.toAuctionRoomResponse(auctionRoom, userInfoMaps);
    }

    @Transactional
    public AuctionRoomParticipantResponse participateAuctionRoom(String userId, UserRole userRole, String auctionRoomId, ParticipateAuctionRoomRequest participateAuctionRoomRequest) {
        // Find auction room by id
        AuctionRoom auctionRoom = auctionRoomRepo.findByIdAndIsDeletedFalse(auctionRoomId)
            .orElseThrow(() -> new AppException("Auction room not found", HttpStatus.NOT_FOUND));

        // Check if auction room is has opened
        if (auctionRoom.getStatus() != AuctionRoomStatus.UPCOMING && auctionRoom.getStatus() != AuctionRoomStatus.ONGOING) {
            log.warn("Auction room is not open for joining or is pending verification or has been closed, auction room: [{}], status: [{}]", auctionRoomId, auctionRoom.getStatus());

            throw new AppException("Auction room is not open for joining or is pending verification or has been closed", HttpStatus.BAD_REQUEST);
        }

        // Check if user is manager is not allowed to join
        if (userRole.equals(UserRole.MANAGER)
            && auctionRoom.getManagerId() != null 
            && auctionRoom.getManagerId().equals(userId)
        ) {
            log.warn("Manager cannot join the auction room, auction room: [{}], user: [{}]", auctionRoomId, userId);

            throw new AppException("Manager cannot join the auction room", HttpStatus.BAD_REQUEST);
        }

        // Check if user is admin and managed this room
        if (userRole.equals(UserRole.ADMIN) 
            && auctionRoom.getManagerId() != null 
            && auctionRoom.getManagerId().equals(userId)
        ) {
            log.warn("Admin who is managing the auction room cannot join the auction room, auction room: [{}], user: [{}]", auctionRoomId, userId);

            throw new AppException("Admin who is managing the auction room cannot join the auction room", HttpStatus.BAD_REQUEST);
        }

        // Check if user is the owner of the auction room
        if (auctionRoom.getOwnerId().equals(userId)) {
            log.warn("Owner cannot join the auction room, auction room: [{}], user: [{}]", auctionRoomId, userId);

            throw new AppException("Owner cannot join the auction room", HttpStatus.BAD_REQUEST);
        }

        // Check current user's wallet
        if (!internalWalletService.validateAvailableBalance(userId, auctionRoom.getCurrentMaxPrice())) {
            log.warn("User's wallet balance is insufficient to join the auction room, auction room: [{}], user: [{}]", auctionRoomId, userId);

            throw new AppException("User's wallet balance is insufficient to join the auction room", HttpStatus.BAD_REQUEST);
        }
        
        // Check current participants and total participants
        if (auctionRoom.getCurrentParticipants() >= auctionRoom.getTotalParticipants()) {
            log.warn("Auction room is full, cannot join, auction room: [{}], user: [{}]", auctionRoomId, userId);
            
            throw new AppException("Auction room is full, cannot join", HttpStatus.BAD_REQUEST);
        }

        // Check if user has already joined the auction room
        boolean existingAuctionRoomParticipant = auctionRoomParticipantService.existsParticipant(userId, auctionRoomId);
        if (existingAuctionRoomParticipant) {
            boolean isParticipantInAuctionRoom = auctionRoomParticipantService.isParticipantInsideAuctionRoom(userId, auctionRoomId);
            if (isParticipantInAuctionRoom) {
                log.warn("User has already joined the auction room, auction room: [{}], user: [{}]", auctionRoomId, userId);

                throw new AppException("User has already joined the auction room", HttpStatus.BAD_REQUEST);
            } else {
                auctionRoomParticipantService.requestToJoin(userId, auctionRoomId, participateAuctionRoomRequest.getParticipatedReason());
            }
        } else {
            auctionRoomParticipantService.createParticipant(auctionRoomId, userId, participateAuctionRoomRequest.getParticipatedReason());
        }

        jobExecutorTasks.runAsync(() -> {
            try {
                internalUserService.incReputationJoined(userId, auctionRoomId);
                internalUserService.incrementUserAuctionMetric(userId, UserAuctionStatus.JOINED.name(), 1);
                
                log.info("Updated async joined metrics and reputation for user: [{}] in auction room: [{}]", userId, auctionRoomId);
            } catch (Exception ex) {
                log.error("Failed to update user post-join metrics for user: [{}]: ", userId, ex);
            }
        });

        UserInfo userInfo = internalUserService.getUserInfoByIds(Set.of(userId)).get(userId);

        log.info("User has requested to join the auction room, auction room: [{}], user: [{}]", auctionRoomId, userId);

        return AuctionRoomParticipantResponse.builder()
            .auctionRoomId(auctionRoomId)
            .auctionRoomTitle(auctionRoom.getTitle())
            .userId(userInfo.getId())
            .username(userInfo.getUsername())
            .status(ParticipantStatus.PENDING.name())
            .build();
    }

    @Transactional
    public AuctionRoomParticipantResponse leaveAuctionRoom(String userId, String auctionRoomId) {
        // Find auction room by id
        AuctionRoom auctionRoom = auctionRoomRepo.findByIdAndIsDeletedFalse(auctionRoomId)
            .orElseThrow(() -> new AppException("Auction room not found", HttpStatus.NOT_FOUND));

        // Check if user is a participant of the auction room
        boolean isParticipantInAuctionRoom = auctionRoomParticipantService.isParticipantInsideAuctionRoom(userId, auctionRoomId);
        if (!isParticipantInAuctionRoom) {
            log.warn("User is not a participant of the auction room, cannot leave, auction room: [{}], user: [{}]", auctionRoomId, userId);

            throw new AppException("User is not a participant of the auction room", HttpStatus.BAD_REQUEST);
        }

        boolean isParticipantLeftAuctionRoom = auctionRoomParticipantService.isParticipantLeftAuctionRoom(userId, auctionRoomId);
        if (isParticipantLeftAuctionRoom) {
            log.warn("User has already left the auction room, cannot leave again, auction room: [{}], user: [{}]", auctionRoomId, userId);

            throw new AppException("User has already left the auction room", HttpStatus.BAD_REQUEST);
        }

        // Check current winner is not allowed to leave the auction room
        if (auctionRoom.getCurrentWinnerId() != null && auctionRoom.getCurrentWinnerId().equals(userId)) {
            log.warn("Current winner cannot leave the auction room, auction room: [{}], user: [{}]", auctionRoomId, userId);

            throw new AppException("Current winner cannot leave the auction room", HttpStatus.BAD_REQUEST);
        }

        // Update participant status to left and decrement current participants in auction room
        auctionRoomParticipantService.leaveAuctionRoom(auctionRoomId, userId);

        // Decrement current participants
        auctionRoom.setCurrentParticipants(auctionRoom.getCurrentParticipants() - 1);

        // Update auction room participant and auction room to database
        auctionRoomRepo.save(auctionRoom);

        UserInfo userInfo = internalUserService.getUserInfoByIds(Set.of(userId)).get(userId);

        log.info("User has successfully left the auction room, auction room: [{}], user: [{}]", auctionRoomId, userId);

        return AuctionRoomParticipantResponse.builder()
            .auctionRoomId(auctionRoomId)
            .auctionRoomTitle(auctionRoom.getTitle())
            .userId(userId)
            .username(userInfo.getUsername())
            .status(ParticipantStatus.LEFT.name())
            .build();
    }

    // My auction room methods
    @Transactional
    public AuctionRoomResponse createMyAuctionRoom(String userId, CreateAuctionRoomRequest createAuctionRoomRequest) {
        // Check if auction room exists same title and user id
        if (auctionRoomRepo.existsByTitleAndOwnerIdAndIsDeletedFalse(createAuctionRoomRequest.getTitle(), userId)) {
            log.warn("Auction room with the same title already exists for user, cannot create, title: [{}], user: [{}]", createAuctionRoomRequest.getTitle(), userId);

            throw new AppException("Auction room with the same title already exists", HttpStatus.CONFLICT);
        }

        // Create new auction room
        Instant endTime = createAuctionRoomRequest.getStartTime()
            .plusSeconds((long) createAuctionRoomRequest.getDurationMinutes() * 60);

        // Create new auction product
        AuctionProduct auctionProduct = auctionProductService.createAuctionProduct(
            createAuctionRoomRequest.getProductId(),
            createAuctionRoomRequest.getStartPrice(),
            createAuctionRoomRequest.getPriceStep(),
            createAuctionRoomRequest.getBuyoutPrice()
        );

        if (auctionProduct == null) {
            log.warn("Failed to create auction product for the auction room, product: [{}], user: [{}]", createAuctionRoomRequest.getProductId(), userId);

            throw new AppException("Product not found or is deleted", HttpStatus.BAD_REQUEST);
        }

        log.info("Created new auction product: [{}]", auctionProduct.getProductId());

        // Sanitize metadata
        Map<String, Object> sanitizedMetadata = MetadataUtils.sanitizeDynamicMetadata(createAuctionRoomRequest.getMetadata());
        
        // Create new auction room entity
        AuctionRoom newAuctionRoom = auctionRoomMapper.toAuctionRoomEntity(
            createAuctionRoomRequest.getTitle(),
            createAuctionRoomRequest.getDescription(),
            userId,
            auctionProduct,
            createAuctionRoomRequest.getStartPrice(),
            createAuctionRoomRequest.getStartTime(),
            endTime,
            createAuctionRoomRequest.isAllowAutoExtend(),
            createAuctionRoomRequest.getExtensionTimeMinutes(),
            createAuctionRoomRequest.isChatEnabled(),
            sanitizedMetadata,
            createAuctionRoomRequest.getTotalParticipants()
        );

        // Save auction room to database
        AuctionRoom savedAuctionRoom = auctionRoomRepo.save(newAuctionRoom);

        // Create new conversation for the auction room if chat enabled
        if (createAuctionRoomRequest.isChatEnabled()) {
            String conversationId = internalConversationService.createAuctionRoomConversation(
                savedAuctionRoom.getId(),
                savedAuctionRoom.getTitle(), 
                userId
            );

            if (conversationId == null) {
                log.error("Failed to create conversation for the auction room, auctionRoomId: {}, userId: {}", savedAuctionRoom.getId(), userId);

                throw new AppException("Failed to create conversation for the auction room", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            savedAuctionRoom.setConversationId(conversationId);
            auctionRoomRepo.save(savedAuctionRoom);
        }

        // Update user auction status and user's reputation for creating auction room
        jobExecutorTasks.runAsync(() -> {
            try {
                internalUserService.incrementUserAuctionMetric(userId, UserAuctionStatus.CREATED.name(), +1);
                 internalUserService.incReputationCreated(userId, savedAuctionRoom.getId());
                
                log.info("Updated async joined metrics and reputation for user: [{}] in auction room: [{}]", userId, savedAuctionRoom.getId());
            } catch (Exception ex) {
                log.error("Failed to update user post-join metrics for user: [{}]: ", userId, ex);
            }
        });

        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(
            Stream.of(savedAuctionRoom.getOwnerId(), savedAuctionRoom.getManagerId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

        log.info("User has successfully created a new auction room, auction room: [{}], user: [{}]", savedAuctionRoom.getId(), userId);

        return auctionRoomMapper.toAuctionRoomResponse(savedAuctionRoom, userInfoMaps);
    }

    public PageResponse<AuctionRoomResponse> getUserAuctionRooms(String userId, int page, int size, String sortBy, String sortDirection) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        // Find auction room participant
        List<String> joinedRoomIds = auctionRoomParticipantService.getActiveRoomIds(userId);

        // Find auction room by owner Id
        Page<AuctionRoom> ownerAuctionRoomPage = auctionRoomRepo.findAllByOwnerIdAndIsDeletedFalse(userId, PageRequest.of(page, size, sortBuilt));
        List<String> ownerRoomIds = ownerAuctionRoomPage.getContent().stream()
            .filter(Objects::nonNull)
            .map(auctionRoom -> auctionRoom.getId())
            .toList();

        List<String> joinedAndOwnerRoomIds = Stream.concat(joinedRoomIds.stream(), ownerRoomIds.stream())
            .distinct()
            .toList();

        // Find all auction rooms that user has joined or owned
        Page<AuctionRoom> auctionRoomPage = auctionRoomRepo.findAllByIdInAndIsDeletedFalse(joinedAndOwnerRoomIds, PageRequest.of(page, size, sortBuilt));
        
        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(
            auctionRoomPage.getContent().stream()
                .flatMap(auctionRoom -> Stream.of(auctionRoom.getOwnerId(), auctionRoom.getManagerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

        return PageResponse.<AuctionRoomResponse>builder()
            .currentPage(auctionRoomPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(auctionRoomPage.getTotalPages())
            .totalElements(auctionRoomPage.getTotalElements())
            .data(auctionRoomMapper.toAuctionRoomResponseList(
                auctionRoomPage.getContent(), userInfoMaps
            ))
            .build();
    }

    public PageResponse<AuctionRoomResponse> searchMyAuctionRooms(String userId, SearchAuctionRoomsRequest searchMyAuctionRoomsRequest, int page, int size, String sortBy, String sortDirection) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        applyCriteria(criteriaList, searchMyAuctionRoomsRequest);

        return applyExecuteQuery(
            query, criteriaList, 
            page, size, 
            sortBy, sortDirection,
            true, userId
        );

    }

    @Transactional
    public AuctionRoomResponse updateMyAuctionRoom(String userId, String auctionRoomId, UpdateAuctionRoomRequest updateAuctionRoomRequest) {
        // Find auction room by id
        AuctionRoom auctionRoom = auctionRoomRepo.findByIdAndIsDeletedFalse(auctionRoomId)
            .orElseThrow(() -> new AppException("Auction room not found", HttpStatus.NOT_FOUND));

        // Check if current user is the owner of the auction room
        if (!auctionRoom.getOwnerId().equals(userId)) {
            log.warn("User is not the owner of the auction room, cannot update, auctionRoomId: {}, userId: {}", auctionRoomId, userId);

            throw new AppException("You are not the owner of this auction room", HttpStatus.FORBIDDEN);
        }

        // Update fields of the auction room
        if (!StringUtils.hasText(updateAuctionRoomRequest.getTitle()) 
            && !updateAuctionRoomRequest.getTitle().equals(auctionRoom.getTitle())
        ) {
            auctionRoom.setTitle(updateAuctionRoomRequest.getTitle());
        }

        if (StringUtils.hasText(updateAuctionRoomRequest.getDescription()) 
            && !updateAuctionRoomRequest.getDescription().equals(auctionRoom.getDescription())
        ) {
            auctionRoom.setDescription(updateAuctionRoomRequest.getDescription());
        }

        if (updateAuctionRoomRequest.getAllowAutoExtend() != null 
            && !updateAuctionRoomRequest.getAllowAutoExtend().equals(auctionRoom.getAllowAutoExtend())
        ) {
            auctionRoom.setAllowAutoExtend(updateAuctionRoomRequest.getAllowAutoExtend());
        }

        if (updateAuctionRoomRequest.getNewDurationExtensionTime() != null 
            && !updateAuctionRoomRequest.getNewDurationExtensionTime().equals(auctionRoom.getExtensionTime())
        ) {
            Integer newExtensionTime = updateAuctionRoomRequest.getNewDurationExtensionTime() + auctionRoom.getExtensionTime();
            auctionRoom.setExtensionTime(newExtensionTime);
        }

        if (updateAuctionRoomRequest.getNewStartTime() != null 
            && !updateAuctionRoomRequest.getNewStartTime().equals(auctionRoom.getStartTime())
            && updateAuctionRoomRequest.getNewStartTime().isAfter(Instant.now()) 
        ) {
            auctionRoom.setStartTime(updateAuctionRoomRequest.getNewStartTime());
        }

        if (updateAuctionRoomRequest.getNewDurationMinutes() != null 
            && updateAuctionRoomRequest.getNewDurationMinutes() > 0
            && auctionRoom.getEndTime().isAfter(auctionRoom.getStartTime().plusSeconds((long) updateAuctionRoomRequest.getNewDurationMinutes() * 60))    
        ) {
            auctionRoom.setEndTime(auctionRoom.getStartTime().plusSeconds((long) updateAuctionRoomRequest.getNewDurationMinutes() * 60));
        }

        if (updateAuctionRoomRequest.getNewAmountTotalParticipants() != null 
            && updateAuctionRoomRequest.getNewAmountTotalParticipants() > 0
        ) {
            Integer newTotalParticipants = updateAuctionRoomRequest.getNewAmountTotalParticipants() + auctionRoom.getTotalParticipants();
            auctionRoom.setTotalParticipants(newTotalParticipants);
        }

        Boolean newChatState = updateAuctionRoomRequest.getChatEnabled();
        if (newChatState != null && !newChatState.equals(auctionRoom.isChatEnabled())) {
            if (Boolean.TRUE.equals(newChatState)) {
                // Create new conversation for the auction room
                String currentConversationId = auctionRoom.getConversationId();
                if (StringUtils.hasText(currentConversationId)) {
                    internalConversationService.unarchiveAuctionRoomConversation(
                        currentConversationId
                    );
                } else {
                    String conversationId = internalConversationService.createAuctionRoomConversation(
                        auctionRoomId,
                        auctionRoom.getTitle(), 
                        userId
                    );

                    if (!StringUtils.hasText(conversationId)) {
                        log.error("Failed to create conversation for the auction room, auction room: [{}], user: [{}]", auctionRoom.getId(), userId);

                        throw new AppException("Failed to create conversation for the auction room", HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    auctionRoom.setConversationId(conversationId);
                }

            } else {
                // Archive the conversation of the auction room
                if (StringUtils.hasText(auctionRoom.getConversationId())) {
                    internalConversationService.archiveAuctionRoomConversation(auctionRoom.getConversationId());
                }
            }

            auctionRoom.setChatEnabled(newChatState);
        }

        // Update metadata field by FIFO round robin strategy
        if (updateAuctionRoomRequest.getNewMetadata() != null && !updateAuctionRoomRequest.getNewMetadata().isEmpty()) {
            Map<String, Object> sanitizedMetadata = MetadataUtils.sanitizeDynamicMetadata(updateAuctionRoomRequest.getNewMetadata());
            Map<String, Object> existingMetadata = auctionRoom.getMetadata() != null ? auctionRoom.getMetadata() : new LinkedHashMap<>();

            long newKeysCount = sanitizedMetadata.keySet().stream()
                .filter(key -> !existingMetadata.containsKey(key))
                .count();

            long overflowCount = existingMetadata.size() + newKeysCount - ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE;

            while (overflowCount > 0 && !existingMetadata.isEmpty()) {
                String oldestKey = existingMetadata.keySet().iterator().next();
                existingMetadata.remove(oldestKey);
                overflowCount--;
            }

            existingMetadata.putAll(sanitizedMetadata);
            auctionRoom.setMetadata(existingMetadata);

            log.info("Auction room has updated, auction room: [{}], user: [{}]", auctionRoomId, userId);
        }

        // Save updated auction room to database
        AuctionRoom updatedAuctionRoom = auctionRoomRepo.save(auctionRoom);

        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(
            Stream.of(updatedAuctionRoom.getOwnerId(), updatedAuctionRoom.getManagerId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

        return auctionRoomMapper.toAuctionRoomResponse(updatedAuctionRoom, userInfoMaps);

    }

    @Transactional
    public void cancelMyAuctionRoom(String userId, String auctionRoomId, CancelAuctionRoomRequest cancelAuctionRoomRequest) {
        // Find auction room by id
        AuctionRoom auctionRoom = auctionRoomRepo.findByIdAndIsDeletedFalse(auctionRoomId)
            .orElseThrow(() -> new AppException("Auction room not found", HttpStatus.NOT_FOUND));

        // Check if current user is the owner of the auction room
        if (!auctionRoom.getOwnerId().equals(userId)) {
            log.warn("User is not the owner of the auction room, cannot cancel, auction room: [{}], user: [{}]", auctionRoomId, userId);

            throw new AppException("You are not the owner of this auction room", HttpStatus.FORBIDDEN);
        }

        // Update auction room status to cancelled and set cancel reason in metadata
        auctionRoom.setStatus(AuctionRoomStatus.CANCELLED);
        auctionRoom.setCancelReason(cancelAuctionRoomRequest.getCancelReason());
        auctionRoom.setCanceledAt(Instant.now());

        // Save updated auction room to database
        auctionRoomRepo.save(auctionRoom);

        log.info("Auction room cancelled, auction room: [{}] by user: [{}] with reason: [{}]", auctionRoomId, userId, cancelAuctionRoomRequest.getCancelReason());

        // Publish event
        List<String> participantIds = auctionRoomParticipantService.getActiveParticipantIds(auctionRoomId);
        if (participantIds.isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(
          AuctionCanceledEvent.builder()
            .auctionRoomId(auctionRoom.getId())
            .auctionTitle(auctionRoom.getTitle())
            .cancelReason(cancelAuctionRoomRequest.getCancelReason())
            .participantIds(participantIds)
            .build()  
        );
    }

    public PageResponse<AuctionRoomResponse> getOwnerCreatedAuctionRooms(String userId, int page, int size, String sortBy, String sortDirection) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        
        Page<AuctionRoom> auctionRoomPage = auctionRoomRepo.findAllByOwnerIdAndIsDeletedFalse(
            userId, PageRequest.of(page, size, sortBuilt)
        );

        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(
            auctionRoomPage.getContent().stream()
                .flatMap(auctionRoom -> Stream.of(auctionRoom.getOwnerId(), auctionRoom.getManagerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

        return PageResponse.<AuctionRoomResponse>builder()
            .currentPage(auctionRoomPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(auctionRoomPage.getTotalPages())
            .totalElements(auctionRoomPage.getTotalElements())
            .data(auctionRoomMapper.toAuctionRoomResponseList(
                auctionRoomPage.getContent(), userInfoMaps
            ))
            .build();
    }

    public PageResponse<AuctionRoomResponse> getUserJoinedAuctionRooms(String userId, int page, int size, String sortBy, String sortDirection) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        // Find auction room participant by user id and status is approved
        List<String> auctionRoomIds = auctionRoomParticipantService.getActiveRoomIds(userId);

        // Find auction rooms by auction room ids
        Page<AuctionRoom> auctionRooms = auctionRoomRepo.findAllByIdInAndIsDeletedFalse(auctionRoomIds, PageRequest.of(page, size, sortBuilt));

        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(
            auctionRooms.getContent().stream()
                .flatMap(room -> Stream.of(room.getOwnerId(), room.getManagerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

        return PageResponse.<AuctionRoomResponse>builder()
            .currentPage(auctionRooms.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(auctionRooms.getTotalPages())
            .totalElements(auctionRooms.getTotalElements())
            .data(auctionRoomMapper.toAuctionRoomResponseList(
                auctionRooms.getContent(), userInfoMaps
            ))
            .build();
    }

    // Admin, manager auction room methods
    public PageResponse<AuctionRoomResponse> getManagedAuctionRooms(
        String managerId, UserRole managerRole, 
        int page, int size, 
        String sortBy, String sortDirection
    ) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);


        Page<AuctionRoom> auctionRoomPage = auctionRoomRepo.findAllByIsDeletedFalse(PageRequest.of(page, size, sortBuilt));
        List<AuctionRoomResponse> auctionRoomResponses = new ArrayList<>();
        
        switch (managerRole) {
            case MANAGER -> {
                auctionRoomResponses = auctionRoomMapper.toAuctionRoomResponseList(
                    auctionRoomPage.getContent().stream()
                        .filter(auctionRoom -> managerId.equals(auctionRoom.getManagerId()))
                        .toList(),
                    internalUserService.getUserInfoByIds(
                        auctionRoomPage.getContent().stream()
                            .filter(auctionRoom -> managerId.equals(auctionRoom.getManagerId()))
                            .flatMap(auctionRoom -> Stream.of(auctionRoom.getOwnerId(), auctionRoom.getManagerId()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet())
                    )
                );   
            } 
            case ADMIN -> {
                auctionRoomResponses = auctionRoomMapper.toAuctionRoomResponseList(
                    auctionRoomPage.getContent(),
                    internalUserService.getUserInfoByIds(
                        auctionRoomPage.getContent().stream()
                            .flatMap(auctionRoom -> Stream.of(auctionRoom.getOwnerId(), auctionRoom.getManagerId()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet())
                    )
                );
            } 
            default -> {
                throw new AppException("You are not authorized to view the auction rooms management", HttpStatus.FORBIDDEN);
            }
        }

        return PageResponse.<AuctionRoomResponse>builder()
            .currentPage(auctionRoomPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(auctionRoomPage.getTotalPages())
            .totalElements(auctionRoomPage.getTotalElements())
            .data(auctionRoomResponses)
            .build();
    }

    @Transactional
    public AuctionRoomResponse updateManagerAuctionRoom(String managerId, UserRole managerRole, String auctionRoomId) {
        // Find auction room by id
        AuctionRoom auctionRoom = auctionRoomRepo.findByIdAndIsDeletedFalse(auctionRoomId)
            .orElseThrow(() -> new AppException("Auction room not found", HttpStatus.NOT_FOUND));
        
        // Check if manager role
        if (!managerRole.equals(UserRole.MANAGER) && !managerRole.equals(UserRole.ADMIN)) {
            log.warn("User is not authorized to manage the auction room, auction room: [{}], user: [{}], role: [{}]", auctionRoomId, managerId, managerRole);

            throw new AppException("You are not authorized to manage this auction room", HttpStatus.FORBIDDEN);
        }

        // Update manager info of the auction room
        // If manager info is null, manager set directly, else admin update manager info if manager info exist
        switch (managerRole) {

            case MANAGER -> {
                if (auctionRoom.getManagerId() == null) {
                    auctionRoom.setManagerId(managerId);
                } else {
                    log.warn("Manager already assigned for this auction room, auction room: [{}], manager: [{}]", auctionRoomId, managerId);

                    throw new AppException(
                        "Manager already assigned for this auction room",
                        HttpStatus.BAD_REQUEST
                    );
                }
            }

            case ADMIN -> auctionRoom.setManagerId(managerId);

            case USER -> {
                log.warn("User is not authorized to manage the auction room, auction room: [{}], user: [{}]", auctionRoomId, managerId);
                throw new AppException(
                    "You are not authorized to manage this auction room",
                    HttpStatus.FORBIDDEN
                );
            }

            default -> throw new AppException(
                "Invalid manager role for updating manager auction room",
                HttpStatus.BAD_REQUEST
            );
        }

        // Save updated auction room to database
        AuctionRoom updatedAuctionRoom = auctionRoomRepo.save(auctionRoom);

        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(
            Stream.of(updatedAuctionRoom.getOwnerId(), updatedAuctionRoom.getManagerId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())  
        );

        log.info("Auction room manager updated, auction room: [{}], manager: [{}]", auctionRoomId, managerId);

        return auctionRoomMapper.toAuctionRoomResponse(updatedAuctionRoom, userInfoMaps);
        
    }

    @Transactional
    public AuctionRoomResponse updateAuctionRoomStatus(String managerId, String auctionRoomId, UpdateAuctionRoomStatusRequest updateAuctionRoomStatusRequest) {
        // Find auction room by id
        AuctionRoom auctionRoom = auctionRoomRepo.findByIdAndIsDeletedFalse(auctionRoomId)
            .orElseThrow(() -> new AppException("Auction room not found", HttpStatus.NOT_FOUND));
        
        // Check if current user is the manager of the auction room
        if (!auctionRoom.getManagerId().equals(managerId)) {
            log.warn("User is not the manager of the auction room, cannot update status, auction room: [{}], user: [{}]", auctionRoomId, managerId);

            throw new AppException("You are not the manager of this auction room", HttpStatus.FORBIDDEN);
        }

        // Update status of the auction room
        AuctionRoomStatus newStatus = AuctionRoomStatus.fromString(updateAuctionRoomStatusRequest.getNewAuctionRoomStatus())
            .orElseThrow(() -> new AppException("Invalid auction room status", HttpStatus.BAD_REQUEST));
        if (auctionRoom.getStatus().equals(newStatus)) {
            log.warn("Auction room is already in the desired status, auction room: [{}], manager: [{}], status: [{}]", auctionRoomId, managerId, newStatus);

            throw new AppException("Auction room is already in the desired status", HttpStatus.BAD_REQUEST);
        }

        auctionRoom.setStatus(newStatus);

        // Save updated auction room to database
        AuctionRoom updatedAuctionRoom = auctionRoomRepo.save(auctionRoom);

        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(
            Stream.of(updatedAuctionRoom.getOwnerId(), updatedAuctionRoom.getManagerId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())  
        );

        log.info("Auction room status updated, auction room: [{}], manager: [{}], newStatus: [{}]", auctionRoomId, managerId, newStatus);

        return auctionRoomMapper.toAuctionRoomResponse(updatedAuctionRoom, userInfoMaps);
    }

    @Transactional
    public AuctionRoomParticipantResponse updateAuctionRoomParticipantStatus(String managerId, String auctionRoomId, String participantId, UpdateParticipantStatusRequest updateParticipantStatusRequest) {
        // Find auction room by id
        AuctionRoom auctionRoom = auctionRoomRepo.findByIdAndIsDeletedFalse(auctionRoomId)
            .orElseThrow(() -> new AppException("Auction room not found", HttpStatus.NOT_FOUND));

        // Check if current user is the manager of the auction room
        if (!auctionRoom.getManagerId().equals(managerId)) {
            log.warn("User is not the manager of the auction room, cannot update participant status, auction room: [{}], user: [{}]", auctionRoomId, managerId);

            throw new AppException("You are not the manager of this auction room", HttpStatus.FORBIDDEN);
        }

        boolean isUpdateAuctionRoomParticipant = auctionRoomParticipantService.processParticipantStatus(
            managerId, auctionRoomId, participantId, updateParticipantStatusRequest.getNewParticipantStatus()
        );

        if (!isUpdateAuctionRoomParticipant) {
            log.error("Failed to update auction room participant status, auction room: [{}], manager: [{}], participant: [{}], newStatus: [{}]", auctionRoomId, managerId, participantId, updateParticipantStatusRequest.getNewParticipantStatus());

            throw new AppException("Failed to update auction room participant status", HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            auctionRoom.setCurrentParticipants(auctionRoom.getCurrentParticipants() + 1);
        }

        auctionRoomRepo.save(auctionRoom);

        UserInfo userInfo = internalUserService.getUserInfoByIds(Set.of(participantId)).get(participantId);

        log.info("Auction room participant status updated, auction room: [{}], manager: [{}], participant: [{}], newStatus: [{}]", auctionRoomId, managerId, participantId, updateParticipantStatusRequest.getNewParticipantStatus());

        return AuctionRoomParticipantResponse.builder()
            .auctionRoomId(auctionRoomId)
            .auctionRoomTitle(auctionRoom.getTitle())
            .userId(participantId)
            .username(userInfo.getUsername())
            .status(updateParticipantStatusRequest.getNewParticipantStatus())
            .build();
    }

    @Transactional
    public void deleteAuctionRoom(String managerId, String auctionRoomId) {
        // Find auction room by id
        AuctionRoom auctionRoom = auctionRoomRepo.findByIdAndIsDeletedFalse(auctionRoomId)
            .orElseThrow(() -> new AppException("Auction room not found", HttpStatus.NOT_FOUND));

        // Check if the manager is authorized to delete the auction room
        if (!auctionRoom.getManagerId().equals(managerId)) {
            log.warn("User is not authorized to delete the auction room, auction room: [{}], user: [{}]", auctionRoomId, managerId);

            throw new AppException("You are not authorized to delete this auction room", HttpStatus.FORBIDDEN);
        }

        if (auctionRoom.getStatus().equals(AuctionRoomStatus.ENDED)
            || auctionRoom.getStatus().equals(AuctionRoomStatus.CANCELLED)
        ) {
            // Soft delete the auction room by setting isDeleted to true
            auctionRoom.setIsDeleted(true);
            auctionRoom.setDeletedAt(LocalDateTime.now());

            // Delete auction room conversation if exists
            internalConversationService.unarchiveAuctionRoomConversation(auctionRoom.getConversationId());
            auctionRoom.setConversationId(null);

            auctionRoomRepo.save(auctionRoom);
        } else {
            log.warn("Only ended or cancelled auction rooms can be deleted, auction room: [{}], user: [{}], status: [{}]", auctionRoomId, managerId, auctionRoom.getStatus());

            throw new AppException("Only ended or cancelled auction rooms can be deleted", HttpStatus.BAD_REQUEST);
        }

        log.info("Auction room deleted, auction room: [{}], manager: [{}]", auctionRoomId, managerId);
    }

    // Utility methods
    /*
     * These methods are used for search auction rooms
     */
    private void applyCriteria( List<Criteria> criteriaList, SearchAuctionRoomsRequest searchAuctionRoomsRequest) {
        criteriaList.add(Criteria.where("isDeleted").is(false));
        
        if (StringUtils.hasText(searchAuctionRoomsRequest.getSearchQuery())) {
            String regex = Pattern.quote(searchAuctionRoomsRequest.getSearchQuery().trim());
            criteriaList.add(new Criteria().orOperator(
                Criteria.where("title").regex(regex, "i"),
                Criteria.where("description").regex(regex, "i"),
                Criteria.where("auctionProduct.productName").regex(regex, "i")
            ));
        }

        if (searchAuctionRoomsRequest.getStatuses() != null && !searchAuctionRoomsRequest.getStatuses().isEmpty()) {
            List<AuctionRoomStatus> statusesEntity = searchAuctionRoomsRequest.getStatuses().stream()
                .map(status -> {
                    return AuctionRoomStatus.fromString(status)
                        .orElseThrow(() -> new AppException("Invalid auction room status: " + status, HttpStatus.BAD_REQUEST));
                })
                .toList();
            criteriaList.add(Criteria.where("status").in(statusesEntity));
        }


        // Filter by start time range
        if (searchAuctionRoomsRequest.getStartTimeFrom() != null || searchAuctionRoomsRequest.getStartTimeTo() != null) {
            if (searchAuctionRoomsRequest.getStartTimeFrom() != null && searchAuctionRoomsRequest.getStartTimeTo() != null) {
                criteriaList.add(Criteria.where("startTime").gte(searchAuctionRoomsRequest.getStartTimeFrom()).lte(searchAuctionRoomsRequest.getStartTimeTo()));
            } else if (searchAuctionRoomsRequest.getStartTimeFrom() != null) {
                criteriaList.add(Criteria.where("startTime").gte(searchAuctionRoomsRequest.getStartTimeFrom()));
            } else {
                criteriaList.add(Criteria.where("startTime").lte(searchAuctionRoomsRequest.getStartTimeTo()));
            }
        }

        // Filter by created at range
        if (searchAuctionRoomsRequest.getCreatedAtFrom() != null || searchAuctionRoomsRequest.getCreatedAtTo() != null) {
            if (searchAuctionRoomsRequest.getCreatedAtFrom() != null && searchAuctionRoomsRequest.getCreatedAtTo() != null) {
                criteriaList.add(Criteria.where("createdAt").gte(searchAuctionRoomsRequest.getCreatedAtFrom()).lte(searchAuctionRoomsRequest.getCreatedAtTo()));
            } else if (searchAuctionRoomsRequest.getCreatedAtFrom() != null) {
                criteriaList.add(Criteria.where("createdAt").gte(searchAuctionRoomsRequest.getCreatedAtFrom()));
            } else {
                criteriaList.add(Criteria.where("createdAt").lte(searchAuctionRoomsRequest.getCreatedAtTo()));
            }
        }

        // Filter by hasSlotted
        if (searchAuctionRoomsRequest.getHasSlotted() != null) {
            if (searchAuctionRoomsRequest.getHasSlotted()) {
                criteriaList.add(Criteria.where("$expr").is(
                    new Document("$gte", List.of("$currentParticipants", "$totalParticipants"))
                ));
            } else {
                criteriaList.add(Criteria.where("$expr").is(
                    new Document("$lt", List.of("$currentParticipants", "$totalParticipants"))
                ));
            }
        }

        // Filter by total participants range
        if (searchAuctionRoomsRequest.getMinTotalParticipants() != null || searchAuctionRoomsRequest.getMaxTotalParticipants() != null) {
            if (searchAuctionRoomsRequest.getMinTotalParticipants() != null && searchAuctionRoomsRequest.getMaxTotalParticipants() != null) {
                criteriaList.add(Criteria.where("totalParticipants").gte(searchAuctionRoomsRequest.getMinTotalParticipants()).lte(searchAuctionRoomsRequest.getMaxTotalParticipants()));
            } else if (searchAuctionRoomsRequest.getMinTotalParticipants() != null) {
                criteriaList.add(Criteria.where("totalParticipants").gte(searchAuctionRoomsRequest.getMinTotalParticipants()));
            } else {
                criteriaList.add(Criteria.where("totalParticipants").lte(searchAuctionRoomsRequest.getMaxTotalParticipants()));
            }
        }

        // Filter by current participants range
        if (searchAuctionRoomsRequest.getMinCurrentParticipants() != null || searchAuctionRoomsRequest.getMaxCurrentParticipants() != null) {
            if (searchAuctionRoomsRequest.getMinCurrentParticipants() != null && searchAuctionRoomsRequest.getMaxCurrentParticipants() != null) {
                criteriaList.add(Criteria.where("currentParticipants").gte(searchAuctionRoomsRequest.getMinCurrentParticipants()).lte(searchAuctionRoomsRequest.getMaxCurrentParticipants()));
            } else if (searchAuctionRoomsRequest.getMinCurrentParticipants() != null) {
                criteriaList.add(Criteria.where("currentParticipants").gte(searchAuctionRoomsRequest.getMinCurrentParticipants()));
            } else {
                criteriaList.add(Criteria.where("currentParticipants").lte(searchAuctionRoomsRequest.getMaxCurrentParticipants()));
            }
        }
    }

    private PageResponse<AuctionRoomResponse> applyExecuteQuery(
        Query baseQuery, List<Criteria> criteriaList,
        int page, int size,
        String sortBy, String sortDirection,
        Object... options
    ) { 
        // Filter by my auction rooms if options is true and currentUserId is provided
        boolean isSearchMyAuctionRoom = (
            options != null && options.length > 0 && options[0] instanceof Boolean && (Boolean) options[0]
        );

        String currentUserId = (
            isSearchMyAuctionRoom && options != null && options.length > 1 && options[1] instanceof String ? (String) options[1] : null
        );

        if (isSearchMyAuctionRoom && currentUserId != null) {
            criteriaList.add(Criteria.where("ownerId").is(currentUserId));
        }

        // Combine criteria
        if(!criteriaList.isEmpty()) {
            criteriaList.forEach(baseQuery::addCriteria);
        }

        // Count total elements before applying pagination
        long totalElements = mongoTemplate.count(baseQuery, AuctionRoom.class);

        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        
        Query pageableQuery = Query.of(baseQuery);
        
        pageableQuery.with(PageRequest.of(page, size, sortBuilt));
        
        List<AuctionRoom> auctionRooms = mongoTemplate.find(pageableQuery, AuctionRoom.class);

        // Get list of user, manager info
        Set<String> userIds = auctionRooms.stream()
            .flatMap(room -> Stream.of(room.getOwnerId(), room.getManagerId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<String, UserInfo> userInfoMaps = internalUserService.getUserInfoByIds(userIds);

        return PageResponse.<AuctionRoomResponse>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .totalElements(totalElements)
            .data(auctionRoomMapper.toAuctionRoomResponseList(
                auctionRooms, userInfoMaps
            ))
            .build();
    }
}
