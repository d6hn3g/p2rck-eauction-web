package com.github.dghng36.eauction.modules.social.message.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.github.dghng36.eauction.modules.social.chat.dto.internal.ChatSocketMessage;
import com.github.dghng36.eauction.modules.social.chat.dto.request.ReadMessageRequest;
import com.github.dghng36.eauction.modules.social.conversation.service.InternalConversationService;
import com.github.dghng36.eauction.modules.social.enums.MessageType;
import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageReplyInfo;
import com.github.dghng36.eauction.modules.social.message.dto.request.AddReactionMessageRequest;
import com.github.dghng36.eauction.modules.social.message.dto.request.EditMessageRequest;
import com.github.dghng36.eauction.modules.social.message.dto.request.SearchMessagesRequest;
import com.github.dghng36.eauction.modules.social.message.dto.response.MessageResponse;
import com.github.dghng36.eauction.modules.social.message.event.MessageDeletedEvent;
import com.github.dghng36.eauction.modules.social.message.event.MessageEditedEvent;
import com.github.dghng36.eauction.modules.social.message.event.MessageReactionEvent;
import com.github.dghng36.eauction.modules.social.message.event.MessageReadEvent;
import com.github.dghng36.eauction.modules.social.message.event.MessageSentEvent;
import com.github.dghng36.eauction.modules.social.message.mapper.MessageMapper;
import com.github.dghng36.eauction.modules.social.message.model.Message;
import com.github.dghng36.eauction.modules.social.message.repository.MessageRepository;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MessageService {
    MongoTemplate mongoTemplate;
    MessageRepository messageRepo;

    InternalUserService internalUserService;
    InternalConversationService internalConversationService;
    InternalMessageService internalMessageService;

    ReactionService reactionService;

    MessageMapper messageMapper;

    ApplicationEventPublisher eventPublisher;

    public PageResponse<MessageResponse> getMessages(
        String userId,
        String conversationId,
        int page, int size
    ) {
        // Validate conversation participant
        internalConversationService.validateParticipant(userId, conversationId);

        // Get messages
        Page<Message> messagePage = messageRepo.findAllByConversationIdAndIsDeletedFalse(
            conversationId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<MessageResponse> messageResponses = toMessageResponseList(userId, messagePage.getContent());

        return PageResponse.<MessageResponse>builder()
            .currentPage(messagePage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(messagePage.getTotalPages())
            .totalElements(messagePage.getTotalElements())
            .data(messageResponses)
            .build();
    }

    public PageResponse<MessageResponse> searchMessages(
        String userId,
        String conversationId,
        SearchMessagesRequest searchMessagesRequest,
        int page, int size
    ) {
        Query query = new Query(); 
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where("conversationId").is(conversationId));
        criteriaList.add(Criteria.where("isDeleted").is(false));
        criteriaList.add(Criteria.where("deletedForUsers").ne(userId));

        if (StringUtils.hasText(searchMessagesRequest.getKeyword())) {
            String regex = ".*" + Pattern.quote(searchMessagesRequest.getKeyword()) + ".*";

            criteriaList.add(Criteria.where("content").regex(regex, "i"));
        }

        if (StringUtils.hasText(searchMessagesRequest.getSenderId())) {
            criteriaList.add(Criteria.where("senderId").is(searchMessagesRequest.getSenderId()));
        }

        if (StringUtils.hasText(searchMessagesRequest.getMessageType())) {
            MessageType messageType = MessageType.fromString(searchMessagesRequest.getMessageType())
                .orElseThrow(() -> new AppException("Invalid message type", HttpStatus.BAD_REQUEST));
            criteriaList.add(Criteria.where("messageType").is(messageType));
        }

        if (searchMessagesRequest.getHasAttachment() != null) {
            criteriaList.add(Criteria.where("attachments").size(Boolean.TRUE.equals(searchMessagesRequest.getHasAttachment()) ? 1 : 0));
        }

        if (searchMessagesRequest.getFromDate() != null) {
            criteriaList.add(Criteria.where("createdAt").gte(searchMessagesRequest.getFromDate()));
        }

        if (searchMessagesRequest.getToDate() != null) {
            criteriaList.add(Criteria.where("createdAt").lte(searchMessagesRequest.getToDate()));
        }

        if (!criteriaList.isEmpty()) {
            criteriaList.forEach(query::addCriteria);
        }

        // Count total elements
        long totalElements = mongoTemplate.count(query, Message.class);

        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Message> messages = mongoTemplate.find(query, Message.class);

        List<MessageResponse> messageResponses = toMessageResponseList(userId, messages);

        return PageResponse.<MessageResponse>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .totalElements(totalElements)
            .data(messageResponses)
            .build();
    }

    public MessageResponse getMessageDetail(
        String userId,
        String messageId
    ) {
        Message message = messageRepo.findByIdAndIsDeletedFalse(messageId)
            .orElseThrow(() -> new AppException("Message not found", HttpStatus.NOT_FOUND));

        internalConversationService.validateParticipant(userId, message.getConversationId());

        return toMessageResponseList(userId, List.of(message)).stream().findFirst()
            .orElseThrow(() -> new AppException("Error converting message to response", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public MessageResponse editMessage(
        String userId,
        String messageId,
        EditMessageRequest editMessageRequest
    ) {
       Query findQuery = new Query().addCriteria(
            Criteria.where("_id").is(messageId).and("isDeleted").is(false)
        );
        findQuery.fields().include("senderId", "conversationId", "deletedForEveryone");

        Message message = mongoTemplate.findOne(findQuery, Message.class);
        if (message == null) {
            throw new AppException("Message not found", HttpStatus.NOT_FOUND);
        }

        validateConversation(userId, message.getConversationId());

        if (!message.getSenderId().equals(userId)) {
            log.warn("User: [{}] attempted to edit message: [{}] which they do not own", userId, messageId);

            throw new AppException("You can only edit your own messages", HttpStatus.FORBIDDEN);
        }

        if (Boolean.TRUE.equals(message.getDeletedForEveryone())) {
            log.warn("User: [{}] attempted to edit message: [{}] which has been deleted for everyone", userId, messageId);

            throw new AppException("You cannot edit a message that has been deleted for everyone", HttpStatus.BAD_REQUEST);
        }

        Query updateQuery = new Query().addCriteria(
            Criteria.where("_id").is(messageId)
                    .and("senderId").is(userId)
                    .and("isDeleted").is(false)
        );

        Update update = new Update()
            .set("content", editMessageRequest.getContent())
            .set("editedAt", Instant.now());

        Message editedMessage = mongoTemplate.findAndModify(
            updateQuery, 
            update, 
            FindAndModifyOptions.options().returnNew(true), 
            Message.class
        );

        if (editedMessage == null) {
            throw new AppException("Message could not be edited or was modified by another request", HttpStatus.CONFLICT);
        }

        eventPublisher.publishEvent(
            MessageEditedEvent.builder()
                .messageId(editedMessage.getId())
                .conversationId(editedMessage.getConversationId())
                .editorId(userId)
                .build()
        );

        return toMessageResponseList(userId, List.of(editedMessage)).stream().findFirst()
            .orElseThrow(() -> new AppException("Error converting message to response", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void deleteMessageForMe(
        String userId,
        String messageId
    ) {
        Query query = new Query()
            .addCriteria(Criteria.where("id").is(messageId).and("isDeleted").is(false));

        Update update = new Update()
            .addToSet("deletedForUsers", userId);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Message.class);
        if (result.getMatchedCount() == 0) {
            log.warn("User: [{}] attempted to delete message: [{}] for themselves, but the message was not found or already deleted", userId, messageId);

            throw new AppException("Message not found", HttpStatus.NOT_FOUND);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void deleteMessageForEveryone(
        String userId,
        String messageId
    ) {
        Query query = new Query()
            .addCriteria(Criteria.where("id").is(messageId)
                .and("senderId").is(userId)
                .and("isDeleted").is(false));

        Update update = new Update()
            .set("deletedForEveryone", true)
            .set("deletedForEveryoneAt", Instant.now());

        Message message = mongoTemplate.findAndModify(
            query, update, 
            FindAndModifyOptions.options().returnNew(false),
            Message.class
        );

        if (message == null) {
            log.warn("User: [{}] attempted to delete message: [{}] for everyone failed", userId, messageId);
            throw new AppException("Message not found or you are not the sender", HttpStatus.NOT_FOUND);
        }

        eventPublisher.publishEvent(
            MessageDeletedEvent.builder()
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .deleterId(userId)
                .build()
        );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void addReaction(
        String userId,
        String messageId,
        AddReactionMessageRequest addReactionMessageRequest
    ) {
        Query findQuery = new Query().addCriteria(Criteria.where("_id").is(messageId).and("isDeleted").is(false));
        findQuery.fields().include("conversationId", "deletedForEveryone");
        
        Message message = mongoTemplate.findOne(findQuery, Message.class);
        if (message == null) throw new AppException("Message not found", HttpStatus.NOT_FOUND);
        if (Boolean.TRUE.equals(message.getDeletedForEveryone())) {
            throw new AppException("You cannot react to a deleted message", HttpStatus.BAD_REQUEST);
        }

        validateConversation(userId, message.getConversationId());

        reactionService.addOrUpdateReactionAtomic(userId, messageId, addReactionMessageRequest.getEmoji());

        // Publish event here
        eventPublisher.publishEvent(
            MessageReactionEvent.builder()
                .messageId(messageId)
                .conversationId(message.getConversationId())
                .reactorId(userId)
                .emoji(addReactionMessageRequest.getEmoji())
                .build()
        );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void removeReaction(
        String userId,
        String messageId,
        String emoji
    ) {
        Query findQuery = new Query().addCriteria(Criteria.where("_id").is(messageId).and("isDeleted").is(false));
        findQuery.fields().include("conversationId", "deletedForEveryone");
        
        Message message = mongoTemplate.findOne(findQuery, Message.class);
        if (message == null) {
            throw new AppException("Message not found", HttpStatus.NOT_FOUND);
        }

        if (Boolean.TRUE.equals(message.getDeletedForEveryone())) {
            log.warn("User: [{}] attempted to remove reaction from message: [{}] which has been deleted for everyone", userId, messageId);
            throw new AppException("You cannot remove reaction from a message that has been deleted for everyone", HttpStatus.BAD_REQUEST);
        }

        validateConversation(userId, message.getConversationId());

        boolean isRemoved = reactionService.removeReactionAtomic(userId, messageId, emoji);
        
        if (!isRemoved) {
            log.warn("User: [{}] attempted to remove reaction: [{}] from message: [{}] which does not exist", userId, emoji, messageId);
            throw new AppException("Reaction not found", HttpStatus.NOT_FOUND);
        }

        // Publish event here
        eventPublisher.publishEvent(
            MessageReactionEvent.builder()
                .messageId(messageId)
                .conversationId(message.getConversationId())
                .reactorId(userId)
                .emoji(emoji)
                .build()
        );
    }

    // Methods for websocket
    @Transactional(propagation = Propagation.SUPPORTS)
    public MessageResponse sendMessage(
        String userId,
        ChatSocketMessage chatSocketMessage
    ) {
        // Validate conversation and participant
        validateConversation(userId, chatSocketMessage.getConversationId());

        // Check if reply message
        if (StringUtils.hasText(chatSocketMessage.getReplyToMessageId())) {
            Message replyToMessage = messageRepo.findByIdAndIsDeletedFalse(chatSocketMessage.getReplyToMessageId())
                .orElseThrow(() -> new AppException("Reply message not found", HttpStatus.NOT_FOUND));

            if (!replyToMessage.getConversationId().equals(chatSocketMessage.getConversationId())) {
                log.warn("User: [{}] attempted to reply to message: [{}] in a different conversation: [{}]", userId, chatSocketMessage.getReplyToMessageId(), chatSocketMessage.getConversationId());
                
                throw new AppException("Reply message must be in the same conversation", HttpStatus.BAD_REQUEST);
            }
        }

        // Create message
        Message message = messageMapper.toMessageEntity(
            userId, chatSocketMessage.getConversationId(),
            chatSocketMessage.getContent(), chatSocketMessage.getType().name(),
            chatSocketMessage.getAttachments(), chatSocketMessage.getReplyToMessageId()
        );

        Message savedMessage = internalMessageService.saveMessageIndependent(message);


        // Publish event here
        eventPublisher.publishEvent(
            MessageSentEvent.builder()
                .messageId(savedMessage.getId())
                .conversationId(savedMessage.getConversationId())
                .senderId(userId)
                .content(savedMessage.getContent())
                .createdAt(savedMessage.getCreatedAt())
                .build()  
        );

        return toMessageResponseList(userId, List.of(savedMessage)).stream().findFirst()
            .orElseThrow(() -> new AppException("Error converting message to response", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void mark(
        String userId,
        ReadMessageRequest readMessageRequest
    ) {
        // Validate conversation and participant
        validateConversation(userId, readMessageRequest.getConversationId());

        eventPublisher.publishEvent(
            MessageReadEvent.builder()
                .conversationId(readMessageRequest.getConversationId())
                .readerId(userId)
                .lastReadMessageId(readMessageRequest.getLastReadMessageId())
                .build()
        );
    }

    // Utility methods
    private List<MessageResponse> toMessageResponseList(
        String userId,
        List<Message> messages
    ) {
        if (messages.isEmpty()) {
            return List.of();
        }

        // Get reply message ids
        Set<String> replyMessageIds = messages.stream()
            .map(message -> message.getReplyToMessageId())
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        
        // Get reply messages and sender info
        Map<String, Message> replyMessageMap = 
            replyMessageIds.isEmpty() ? 
                new HashMap<>() :
                messageRepo.findByIdInAndIsDeletedFalse(replyMessageIds).stream()
                    .collect(Collectors.toMap(message -> message.getId(), Function.identity()));

        // Get sender info
        Set<String> senderIds = messages.stream()
            .map(message -> message.getSenderId())
            .collect(Collectors.toSet());
        replyMessageMap.values().forEach(m -> senderIds.add(m.getSenderId()));
        
        // Get sender info
        Map<String, UserInfo> senderInfoMap = internalUserService.getUserInfoByIds(senderIds);

        return messages.stream()
            .filter(message -> CollectionUtils.isEmpty(message.getDeletedForUsers()) 
                    || !message.getDeletedForUsers().contains(userId))
            .map(message -> {
                UserInfo sender = senderInfoMap.get(message.getSenderId());
                MessageReplyInfo replyInfo = null;
                if (StringUtils.hasText(message.getReplyToMessageId())) {
                    Message replyMessage = replyMessageMap.get(message.getReplyToMessageId());

                    if (replyMessage != null) {
                        UserInfo replySender = senderInfoMap.get(replyMessage.getSenderId());
                        replyInfo = messageMapper.toMessageReplyInfo(replyMessage, replySender);
                    }
                }

                return messageMapper.toMessageResponse(message, sender, replyInfo);
            })
            .toList();
    }

    private void validateConversation(String userId, String conversationId) {
        internalConversationService.validateActive(conversationId);
        internalConversationService.validateParticipant(userId, conversationId);
    }
}
