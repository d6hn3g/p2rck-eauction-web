package com.github.dghng36.eauction.modules.social.message.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.modules.social.message.model.Message;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class InternalMessageService {
    MongoTemplate mongoTemplate;
    
    public Message saveMessageIndependent(Message message) {
        return mongoTemplate.insert(message);
    }
}
