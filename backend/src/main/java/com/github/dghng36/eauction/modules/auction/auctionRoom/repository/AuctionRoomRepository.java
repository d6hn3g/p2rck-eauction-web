package com.github.dghng36.eauction.modules.auction.auctionRoom.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;

public interface AuctionRoomRepository extends MongoRepository<AuctionRoom, String> {
    boolean existsByTitleAndOwnerIdAndIsDeletedFalse(String title, String userId);
    
    boolean existsByIdAndIsDeletedFalse(String id);

    Optional<AuctionRoom> findByIdAndIsDeletedFalse(String id);

    Page<AuctionRoom> findAllByIsDeletedFalse(Pageable pageable);

    Page<AuctionRoom> findAllByOwnerIdAndIsDeletedFalse(String ownerId, Pageable pageable);

    Page<AuctionRoom> findAllByIdInAndIsDeletedFalse(List<String> ids, Pageable pageable);

    List<AuctionRoom> findAllByIdInAndIsDeletedFalse(Set<String> ids);
}
