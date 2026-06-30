package com.github.dghng36.eauction.modules.social.message.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageReaction;
import com.github.dghng36.eauction.modules.social.message.mapper.MessageReactionMapper;
import com.github.dghng36.eauction.modules.social.message.model.Message;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReactionService {
    MongoTemplate mongoTemplate;

    MessageReactionMapper messageReactionMapper;
    
    public void addOrUpdateReactionAtomic(
        String userId,
        String messageId,
        String emoji
    ) {
        Query removeOldQuery = new Query().addCriteria(Criteria.where("_id").is(messageId));
        Update removeOldUpdate = new Update().pull("reactions", Query.query(Criteria.where("userId").is(userId)));
        mongoTemplate.updateFirst(removeOldQuery, removeOldUpdate, Message.class);

        MessageReaction newReaction = messageReactionMapper.toMessageReactionEntity(userId, emoji);

        Query updateQuery = new Query().addCriteria(Criteria.where("_id").is(messageId));
        Update update = new Update().push("reactions", newReaction);
        mongoTemplate.updateFirst(updateQuery, update, Message.class);
    }

    public boolean removeReactionAtomic(
        String userId,
        String messageId,
        String emoji
    ) {
        Query updateQuery = new Query().addCriteria(Criteria.where("_id").is(messageId));
    
        Update update = new Update().pull("reactions", 
            Query.query(Criteria.where("userId").is(userId).and("emoji").is(emoji))
        );

        UpdateResult result = mongoTemplate.updateFirst(updateQuery, update, Message.class);
        
        return result.getModifiedCount() > 0;
    }

    public void addOrUpdateReaction(String userId, Message message, String emoji) {
        if (message.getReactions() == null) {
            message.setReactions(new java.util.ArrayList<>());
        }
        
        message.getReactions().removeIf(r -> r.getUserId().equals(userId));
        MessageReaction newReaction = messageReactionMapper.toMessageReactionEntity(userId, emoji);
        message.getReactions().add(newReaction);
    }

    public boolean removeReaction(String userId, Message message, String emoji) {
        if (message.getReactions() == null) {
            return false;
        }
        
        return message.getReactions().removeIf(r -> 
            r.getUserId().equals(userId) && (emoji == null ? r.getEmoji() == null : emoji.equals(r.getEmoji()))
        );
    }

}
