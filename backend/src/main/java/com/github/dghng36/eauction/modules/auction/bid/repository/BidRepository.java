package com.github.dghng36.eauction.modules.auction.bid.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.auction.bid.model.Bid;


public interface BidRepository extends MongoRepository<Bid, String> {
    Page<Bid> findAllByAuctionRoomIdAndIsDeletedFalse(String auctionRoomId, Pageable pageable);

    Optional<Bid> findByIdAndIsDeletedFalse(String id);

    Page<Bid> findAllByBidderInfoBidderIdAndIsDeletedFalse(String bidderId, Pageable pageable);

    long countByAuctionRoomIdAndIsDeletedFalse(String auctionRoomId);

}
