package com.github.dghng36.eauction.modules.auction.product.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.modules.auction.enums.ProductStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProductInfo;
import com.github.dghng36.eauction.modules.auction.product.mapper.AuctionProductInfoMapper;
import com.github.dghng36.eauction.modules.auction.product.mapper.AuctionProductMapper;
import com.github.dghng36.eauction.modules.auction.product.model.Product;
import com.github.dghng36.eauction.modules.auction.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuctionProductService {
    MongoTemplate mongoTemplate;
    ProductRepository productRepo;

    AuctionProductMapper auctionProductMapper;
    AuctionProductInfoMapper auctionProductInfoMapper;

    public AuctionProduct createAuctionProduct(
        String productId,
        Double startPrice,
        Double priceStep,
        Double buyoutPrice
    ) {
        Product product = productRepo.findByIdAndIsDeletedFalse(productId)
            .orElse(null);
        if (product == null) {
            return null;
        }

        return auctionProductMapper.toAuctionProduct(
            productId,
            product.getName(),
            product.getDescription(),
            product.getImages().get(0),
            startPrice,
            priceStep,
            startPrice,
            buyoutPrice
        );
    }

    public Map<String, AuctionProductInfo> getAuctionProductInfoByIds(Set<String> auctionProductIds) {
        List<Product> products = productRepo.findAllByIdInAndIsDeletedFalse(auctionProductIds);

        return products.stream()
            .collect(Collectors.toMap(product -> product.getId(), 
                product -> auctionProductInfoMapper.toAuctionProductInfo(product.getId(), product.getName()))
            );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void markProductInAuction(String productId) {
        Query query = new Query().addCriteria(
            Criteria.where("id").is(productId).and("isDeleted").is(false)
        );
        Update update = new Update().set("status", ProductStatus.IN_AUCTION.name());

        mongoTemplate.updateFirst(query, update, Product.class);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void markProductAsSold(String productId) {
        Query query = new Query().addCriteria(
            Criteria.where("id").is(productId).and("isDeleted").is(false)
        );
        Update update = new Update().set("status", ProductStatus.SOLD.name());

        mongoTemplate.updateFirst(query, update, Product.class);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void markProductAvailable(String productId) {
        Query query = new Query().addCriteria(
            Criteria.where("id").is(productId).and("isDeleted").is(false)
        );
        Update update = new Update().set("status", ProductStatus.AVAILABLE.name());

        mongoTemplate.updateFirst(query, update, Product.class);
    }
}
