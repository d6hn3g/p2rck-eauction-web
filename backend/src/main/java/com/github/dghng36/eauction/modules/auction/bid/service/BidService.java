package com.github.dghng36.eauction.modules.auction.bid.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.TransientClientSessionException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.MetadataUtils;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.internal.AuctionRoomInfo;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.InternalAuctionRoomService;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.SearchBidsRequest;
import com.github.dghng36.eauction.modules.auction.bid.dto.response.BidResponse;
import com.github.dghng36.eauction.modules.auction.bid.event.BidOutbidEvent;
import com.github.dghng36.eauction.modules.auction.bid.event.BidPlacedEvent;
import com.github.dghng36.eauction.modules.auction.bid.mapper.BidMapper;
import com.github.dghng36.eauction.modules.auction.bid.model.Bid;
import com.github.dghng36.eauction.modules.auction.bid.repository.BidRepository;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProductInfo;
import com.github.dghng36.eauction.modules.auction.product.service.AuctionProductService;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.mongodb.MongoCommandException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BidService {
    MongoTemplate mongoTemplate;
    BidRepository bidRepo;

    AutoBidProcessor autoBidProcessor;

    AuctionProductService auctionProductService;
    InternalAuctionRoomService internalAuctionRoomService;
    InternalUserService internalUserService;
    InternalWalletService internalWalletService;
    InternalBidService internalBidService;

    BidMapper bidMapper;

    ApplicationEventPublisher eventPublisher;

    static Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
        "bidderInfo.bidderName",
        "bidAmount",
        "bidTime",
        "createdAt",
        "updatedAt"
    );
    
    // Public methods for bid controller
    public PageResponse<BidResponse> getAuctionRoomBidHistories(String auctionRoomId, int page, int size, String sortBy, String sortDirection) {
        // Check if auction room exists
        boolean existingAuctionRoom = internalAuctionRoomService.existsAuctionRoom(auctionRoomId);
        if (!existingAuctionRoom) {
            throw new AppException("Auction room not found", HttpStatus.NOT_FOUND);
        }

        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<Bid> bidHistoriesPage = bidRepo.findAllByAuctionRoomIdAndIsDeletedFalse(auctionRoomId, PageRequest.of(page, size, sortBuilt));

        List<BidResponse> bidResponses = toBidResponseListConcurrently(bidHistoriesPage.getContent());
            
        return PageResponse.<BidResponse>builder()
            .currentPage(bidHistoriesPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(bidHistoriesPage.getTotalElements())
            .totalPages(bidHistoriesPage.getTotalPages())
            .data(bidResponses)
            .build();
        
    }

    public PageResponse<BidResponse> searchAuctionRoomBidHistories(String auctionRoomId, int page, int size, String sortBy, String sortDirection, SearchBidsRequest searchBidsRequest) {
        // Create new query and new criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Add criteria for auction room id
        criteriaList.add(Criteria.where("auctionRoomId").is(auctionRoomId));

        applyCriteria(criteriaList, searchBidsRequest);

        return applyExecuteQuery(
            query, criteriaList,
            page, size, 
            sortBy, sortDirection
        );
    }

    public BidResponse getBidDetail(String bidId) {
        Bid bid = bidRepo.findByIdAndIsDeletedFalse(bidId)
            .orElseThrow(() -> new AppException("Bid not found", HttpStatus.NOT_FOUND));

        // Get auction product info and auction room info by their ids concurrently
        CompletableFuture<AuctionProductInfo> auctionProductFuture = CompletableFuture.supplyAsync(() -> {
            if (bid.getAuctionProductId() != null) {
                return auctionProductService
                    .getAuctionProductInfoByIds(Set.of(bid.getAuctionProductId()))
                    .get(bid.getAuctionProductId());
            }
            return null;
        });

        CompletableFuture<AuctionRoomInfo> auctionRoomFuture = CompletableFuture.supplyAsync(() -> {
            if (bid.getAuctionRoomId() != null) {
                return internalAuctionRoomService
                    .getAuctionRoomInfoByIds(Set.of(bid.getAuctionRoomId()))
                    .get(bid.getAuctionRoomId());
            }
            return null;
        });

        CompletableFuture.allOf(auctionProductFuture, auctionRoomFuture).join();

        AuctionProductInfo auctionProductInfo = auctionProductFuture.join();
        AuctionRoomInfo auctionRoomInfo = auctionRoomFuture.join();
        
        return bidMapper.toBidResponse(
            bid, 
            auctionProductInfo, auctionRoomInfo
        );
    }

    // Methods for user bid controller
    public PageResponse<BidResponse> getUserBidHistories(String userId, int page, int size, String sortBy, String sortDirection) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<Bid> bidHistoriesPage = bidRepo.findAllByBidderInfoBidderIdAndIsDeletedFalse(
            userId, PageRequest.of(page, size, sortBuilt)
        );

        List<BidResponse> bidResponses = toBidResponseListConcurrently(bidHistoriesPage.getContent());
        
        return PageResponse.<BidResponse>builder()
            .currentPage(bidHistoriesPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(bidHistoriesPage.getTotalElements())
            .totalPages(bidHistoriesPage.getTotalPages())
            .data(bidResponses)
            .build();
    }

    public PageResponse<BidResponse> searchUserBidHistories(
        String userId, SearchBidsRequest searchBidsRequest, 
        int page, int size, 
        String sortBy, String sortDirection) {
        // Create new query and new criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        applyCriteria(criteriaList, searchBidsRequest);

        // Add criteria for bidder id
        criteriaList.add(Criteria.where("bidderInfo.bidderId").is(userId));

        return applyExecuteQuery(
            query, criteriaList,
            page, size, 
            sortBy, sortDirection
        );
    }

    // Methods for manager bid controller
    public long getTotalAuctionRoomBidHistories(String auctionRoomId) {
        return bidRepo.countByAuctionRoomIdAndIsDeletedFalse(auctionRoomId);
    }

    @Transactional
    public void deleteAuctionRoomBidHistory(String auctionRoomId, String bidHistoryId) {
        Bid bid = bidRepo.findByIdAndIsDeletedFalse(bidHistoryId)
            .orElseThrow(() -> new AppException("Bid history not found", HttpStatus.NOT_FOUND));

        if (!bid.getAuctionRoomId().equals(auctionRoomId)) {
            log.warn("Bid history: [{}] does not belong to auction room: [{}]", bidHistoryId, auctionRoomId);

            throw new AppException("Bid history does not belong to the auction room", HttpStatus.BAD_REQUEST);
        }

        bid.setIsDeleted(true);
        bid.setDeletedAt(LocalDateTime.now());

        bidRepo.save(bid);

        log.info("Deleted bid history: [{}] for auction room: [{}]", bidHistoryId, auctionRoomId);
    }

    @Transactional
    public void deleteAllAuctionRoomBidHistories(String auctionRoomId) {
        List<Bid> bids = bidRepo.findAllByAuctionRoomIdAndIsDeletedFalse(auctionRoomId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        for (Bid bid : bids) {
            bid.setIsDeleted(true);
            bid.setDeletedAt(LocalDateTime.now());
        }

        bidRepo.saveAll(bids);

        log.info("Deleted all bid histories for auction room: [{}]", auctionRoomId);
    }

    // Methods for web socket bid
    @Retryable(
        retryFor = { 
            DataIntegrityViolationException.class,
            MongoCommandException.class,
            ConcurrencyFailureException.class,
            TransientClientSessionException.class
        },
        maxAttempts = 5,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 300) 
    )
    @Transactional(propagation = Propagation.SUPPORTS)
    public void placeBid(String userId,
        String auctionRoomId, Double bidAmount, Map<String, Object> metadata,
        Boolean enableAutoBid, Double maxAutoBidPrice, Double incrementAmount,
        Boolean isAutoGenerated
    ) {
        if (bidAmount == null || bidAmount <= 0) {
            log.warn("Invalid bid amount: [{}] for user: [{}] in auction room: [{}]", bidAmount, userId, auctionRoomId);

            throw new AppException("Bid amount must be greater than 0", HttpStatus.BAD_REQUEST);
        }

        // Find user info by id
        UserInfo bidder = internalUserService.getUserInfoByIds(Set.of(userId)).get(userId);
        if (bidder == null) {
            log.warn("User not found with id: [{}]", userId);

            throw new AppException("User not found", HttpStatus.NOT_FOUND);
        }

        AuctionRoom auctionRoom = internalAuctionRoomService.validateAndGetForBidding(auctionRoomId, userId);

        // Check if current max price is greater than or equal to buyout price
        boolean isBuyout = internalAuctionRoomService.isBuyoutReached(auctionRoom, bidAmount);
        if (isBuyout) {
            internalAuctionRoomService.validateBidAmount(auctionRoom, bidAmount);
        }

        // Check if auto bid enabled
        if (Boolean.TRUE.equals(enableAutoBid)) {
            if (!autoBidProcessor.validateAutoBidPrice(maxAutoBidPrice, incrementAmount)) {
                log.warn("Invalid auto bid price settings for user: [{}] in auction room: [{}]", userId, auctionRoomId);

                throw new AppException("Invalid auto bid price settings", HttpStatus.BAD_REQUEST);
            }

            if (maxAutoBidPrice <= bidAmount) {
                log.warn("Max auto bid price: [{}] must be greater than bid amount: [{}]", maxAutoBidPrice, bidAmount);

                throw new AppException("Max auto bid price must be greater than bid amount", HttpStatus.BAD_REQUEST);
            }
            autoBidProcessor.processEnableAutoBid(auctionRoomId, userId, maxAutoBidPrice, incrementAmount);
        }

        // Check wallet balance
        if (!internalWalletService.validateAvailableBalance(userId, bidAmount)) {
            log.warn("User's wallet balance is insufficient to place the bid, auction room: [{}], user: [{}], bid amount: [{}]", auctionRoomId, userId, bidAmount);

            throw new AppException("User's wallet balance is insufficient to place the bid", HttpStatus.BAD_REQUEST);
        }

        // Check is Buyout
        if (isBuyout && !Boolean.TRUE.equals(auctionRoom.getAllowAutoExtend())) {
            internalAuctionRoomService.updateAuctionRoomStatus(auctionRoomId, AuctionRoomStatus.ENDED.name());
        }

        AuctionRoom updatedAuctionRoom = internalAuctionRoomService.processNewBidSuccess(auctionRoom, userId, bidAmount);

        // Check is outbid
        String currentHighestBidderId = updatedAuctionRoom.getCurrentWinnerId();
        Double currentHighestPrice = updatedAuctionRoom.getCurrentMaxPrice();

        if (StringUtils.hasText(currentHighestBidderId) && !currentHighestBidderId.equals(userId)) {
            eventPublisher.publishEvent(
                BidOutbidEvent.builder()
                    .auctionRoomId(auctionRoomId)
                    .auctionTitle(auctionRoom.getTitle())
                    .outbidUserId(currentHighestBidderId)
                    .newHighestBidderId(userId)
                    .previousHighestPrice(currentHighestPrice)
                    .currentHighestPrice(bidAmount)
                    .build()
            );
        }

        // Hold balance
        internalWalletService.holdBalance(userId, bidAmount);

        // Create new bid
        String auctionProductId = internalAuctionRoomService.getProductId(auctionRoomId);

        String safeProductId = (auctionProductId != null) ? auctionProductId : "N/A";

        Bid newBid = Bid.builder()
            .auctionRoomId(auctionRoomId)
            .auctionProductId(safeProductId)
            .bidderInfo(bidMapper.toBidderInfo(
                bidder.getId(),
                bidder.getUsername(),
                bidder.getAvatar()
            ))
            .bidAmount(bidAmount)
            .bidTime(Instant.now())
            .metadata(MetadataUtils.sanitizeDynamicMetadata(metadata))
            .isWinningBid(isBuyout)
            .isDeleted(false)
            .build();

        Bid savedBid = internalBidService.saveBidHistoryIndependent(newBid);
        
        // Get total current bidder bid amount in the auction room
        double totalBidAmount = internalBidService.getTotalBidAmount(auctionRoomId, userId);

        log.info("User: [{}] placed a bid of [{}] in auction room [{}]. Total bid amount: [{}]", userId, bidAmount, auctionRoomId, totalBidAmount);

        // Publish domain event
        eventPublisher.publishEvent(
            BidPlacedEvent.builder()
                .bidId(savedBid.getId())
                .auctionRoomId(auctionRoomId)
                .userId(userId)
                .username(bidder.getUsername())
                .bidAmount(bidAmount)
                .totalBidAmount(totalBidAmount)
                .bidTime(savedBid.getBidTime())
                .isAutoBid(Boolean.TRUE.equals(enableAutoBid))
                .build()  
        );
    }

    @Transactional
    public void enableAutoBid(
        String userId,
        String auctionRoomId,
        Double maxAutoBidPrice,
        Double incrementAmount
    ) { 
        // Find user by id
        UserInfo userInfo = internalUserService.getUserInfoByIds(Set.of(userId)).get(userId);
        if (userInfo == null) {
            log.warn("User not found with id: [{}]", userId);

            throw new AppException("User not found", HttpStatus.NOT_FOUND);
        }

        // Validate auction room
        internalAuctionRoomService.validateAndGetForBidding(auctionRoomId, userId);
        
        // Validate auto bid price
        boolean validAutoBidPrice = autoBidProcessor.validateAutoBidPrice(maxAutoBidPrice, incrementAmount);
        if (!validAutoBidPrice) {
            log.warn("Invalid auto bid price settings for user: [{}] in auction room: [{}]", userId, auctionRoomId);

            throw new AppException("Invalid auto bid price settings", HttpStatus.BAD_REQUEST);
        }

        autoBidProcessor.processEnableAutoBid(auctionRoomId, userId, maxAutoBidPrice, incrementAmount);

        log.info("Enabled auto bid for user: [{}] in auction room: [{}] with max price: [{}] and increment: [{}]", userId, auctionRoomId, maxAutoBidPrice, incrementAmount);
    }

    @Transactional
    public void disableAutoBid(
        String userId,
        String auctionRoomId
    ) {
        // Find user info by id
        UserInfo userInfo = internalUserService.getUserInfoByIds(Set.of(userId)).get(userId);
        if (userInfo == null) {
            log.warn("User not found with id [{}]", userId);

            throw new AppException("User not found", HttpStatus.NOT_FOUND);
        }

        // Validate auction room
        internalAuctionRoomService.validateAndGetForBidding(auctionRoomId, userId);

        // Disable auto bid in auto bid service
        autoBidProcessor.processDisableAutoBid(auctionRoomId, userId);
        log.info("Disabled auto bid for user: [{}] in auction room: [{}]", userId, auctionRoomId);
    }

    // Utility methods
    private void applyCriteria(List<Criteria> criteriaList, SearchBidsRequest searchBidsRequest) {
        criteriaList.add(Criteria.where("isDeleted").is(false));
        
        if (StringUtils.hasText(searchBidsRequest.getSearchQuery())) {
            String regex = Pattern.quote(searchBidsRequest.getSearchQuery().trim());

            List<Criteria> orCriteriaList = new ArrayList<>();

            orCriteriaList.add(new Criteria().orOperator(
                Criteria.where("bidderInfo.bidderName").regex(regex, "i"),
                Criteria.where("bidderInfo.bidderEmail").regex(regex, "i")
            ));
            

            // Try to parse search query to number and add criteria for bid amount
            try {
                double bidAmount = Double.parseDouble(regex);

                orCriteriaList.add(Criteria.where("bidderInfo.bidAmount").is(bidAmount));
            } catch (NumberFormatException e) {
                // Ignore if not a valid number
            }

            criteriaList.add(new Criteria().orOperator(orCriteriaList.toArray(Criteria[]::new)));
        }

        // Filter by bidder name
        if (searchBidsRequest.getBidderName() != null) {
            criteriaList.add(Criteria.where("bidderInfo.bidderName").is(searchBidsRequest.getBidderName()));
        }

        // Filter by bid amount range
        if (searchBidsRequest.getMinBidAmount() != null) {
            criteriaList.add(Criteria.where("bidAmount").gte(searchBidsRequest.getMinBidAmount()));
        }

        if (searchBidsRequest.getMaxBidAmount() != null) {
            criteriaList.add(Criteria.where("bidAmount").lte(searchBidsRequest.getMaxBidAmount()));
        }

        // Filter by bid time range
        if (searchBidsRequest.getBidTimeFrom() != null) {
            criteriaList.add(Criteria.where("bidTime").gte(searchBidsRequest.getBidTimeFrom()));
        }

        if (searchBidsRequest.getBidTimeTo() != null) {
            criteriaList.add(Criteria.where("bidTime").lte(searchBidsRequest.getBidTimeTo()));
        }

    }

    private PageResponse<BidResponse> applyExecuteQuery(
        Query baseQuery, List<Criteria> criteriaList, 
        int page, int size, 
        String sortBy, String sortDirection
    ) {
        if (!criteriaList.isEmpty()) {
            criteriaList.forEach(baseQuery::addCriteria);
        }

        // Count total elements
        long totalElements = mongoTemplate.count(baseQuery, Bid.class);

        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Query pageableQuery = baseQuery.with(PageRequest.of(page, size, sortBuilt));
        
        
        List<Bid> bids = mongoTemplate.find(pageableQuery, Bid.class);

        List<BidResponse> bidResponses = toBidResponseListConcurrently(bids);
        
        return PageResponse.<BidResponse>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(totalElements)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .data(bidResponses)
            .build();
    }

    private Map<String, AuctionProductInfo> getAuctionProductInfoMap(Set<String> auctionProductIds) {
        return auctionProductService.getAuctionProductInfoByIds(auctionProductIds);
    }

    private Map<String, AuctionRoomInfo> getAuctionRoomInfoMap(Set<String> auctionRoomIds) {
        return internalAuctionRoomService.getAuctionRoomInfoByIds(auctionRoomIds);
    }

    private List<BidResponse> toBidResponseListConcurrently(
        List<Bid> bids
    ) {
        Set<String> auctionProductIds = bids.stream()
            .map(bid -> bid.getAuctionProductId())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Set<String> auctionRoomIds = bids.stream()
            .map(bid -> bid.getAuctionRoomId())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        CompletableFuture<Map<String, AuctionProductInfo>> auctionProductFuture = CompletableFuture.supplyAsync(() -> {
            return getAuctionProductInfoMap(auctionProductIds);
        });

        CompletableFuture<Map<String, AuctionRoomInfo>> auctionRoomFuture = CompletableFuture.supplyAsync(() -> {
            return getAuctionRoomInfoMap(auctionRoomIds);
        });

        CompletableFuture.allOf(auctionProductFuture, auctionRoomFuture).join();

        Map<String, AuctionProductInfo> auctionProductInfoMap = auctionProductFuture.join();
        Map<String, AuctionRoomInfo> auctionRoomInfoMap = auctionRoomFuture.join();

        return bidMapper.toBidResponseList(
            bids, 
            auctionProductInfoMap, auctionRoomInfoMap
        );
    }
}
