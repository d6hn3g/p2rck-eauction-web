package com.github.dghng36.eauction.modules.auction.product.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.auction.product.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
    boolean existsByOwnerIdAndNameAndIsDeletedFalse(String userId, String name);
    
    Optional<Product> findByIdAndIsDeletedFalse(String id);
    
    List<Product> findAllByIdInAndIsDeletedFalse(Set<String> ids);

    Page<Product> findAllByIsDeletedFalse(Pageable pageable);
    
    Page<Product> findAllByOwnerIdAndIsDeletedFalse(String ownerId, Pageable pageable);
}
