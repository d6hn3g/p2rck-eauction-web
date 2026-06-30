package com.github.dghng36.eauction.modules.auction.bid.service;

import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.modules.auction.bid.model.Bid;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class InternalBidService {
    MongoTemplate mongoTemplate;

    @Transactional(propagation = Propagation.NOT_SUPPORTED) 
    public Bid saveBidHistoryIndependent(Bid newBid) {
        return mongoTemplate.insert(newBid);
    }

    public double getTotalBidAmount(String auctionRoomId, String userId) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("auctionRoomId").is(auctionRoomId)
                        .and("bidderInfo.bidderId").is(userId)
                        .and("isDeleted").is(false)
            ),
            Aggregation.group().sum("bidAmount").as("totalAmount")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "bids", Document.class);
        
        List<Document> mappedResults = results.getMappedResults();

        if (mappedResults != null && !mappedResults.isEmpty()) {
            Document firstResult = mappedResults.get(0);
            
            Object totalAmountObj = firstResult.get("totalAmount");
            if (totalAmountObj instanceof Number number) {
                return number.doubleValue();
            }
        }
        
        return 0.0;
    }
}
