package com.github.dghng36.eauction.modules.social.conversation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.github.dghng36.eauction.modules.social.conversation.dto.request.CreateDirectConversationRequest;
import com.github.dghng36.eauction.modules.social.conversation.dto.request.SearchConversationRequest;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationDetailResponse;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationParticipantResponse;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationResponse;
import com.github.dghng36.eauction.modules.social.conversation.mapper.ConversationMapper;
import com.github.dghng36.eauction.modules.social.conversation.model.Conversation;
import com.github.dghng36.eauction.modules.social.conversation.model.ConversationParticipant;
import com.github.dghng36.eauction.modules.social.conversation.repository.ConversationRepository;
import com.github.dghng36.eauction.modules.social.enums.ConversationType;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ConversationService {
    MongoTemplate mongoTemplate;
    ConversationRepository conversationRepo;

    InternalUserService internalUserService;
    ConversationParticipantService conversationParticipantService;

    ConversationMapper conversationMapper;

    JobExecutorTasks jobExecutorTasks;

    static final Set<String> ALLOWED_CONVERSATION_SORT_BY_FIELDS = Set.of(
        "title", 
        "lastMessageTime",
        "createdAt",
        "updatedAt"
    );

    static final Set<String> ALLOWED_CONVERSATION_PARTICIPANT_SORT_BY_FIELDS = Set.of(
        "joinedAt",
        "unreadCount",
        "lastReadMessageAt",
        "pinnedAt",
        "hiddenAt"
    );

    @Transactional
    public ConversationResponse createDirectConversation(String userId, CreateDirectConversationRequest createDirectConversationRequest) {
        // Check current user and recipient are not the same
        String recipientUserId = createDirectConversationRequest.getRecipientUserId();
        if (userId.equals(recipientUserId)) {
            log.error("User {} attempted to create a direct conversation with themselves", userId);

            throw new AppException("Cannot create a direct conversation with yourself", HttpStatus.BAD_REQUEST);
        }

        // Check if existing direct conversation between the two users exists
        Conversation existingConversation = conversationRepo.findDirectConversationBetweenTwoUsersAndIsDeletedFalse(userId, recipientUserId).orElse(null);
        if (existingConversation != null) {
            ConversationParticipant conversationParticipant = 
                conversationParticipantService.getParticipant(userId, existingConversation.getId());
            
            return conversationParticipant == null ? null
                : toConversationResponse(existingConversation, conversationParticipant);
        }

        // Create new conversation
        Conversation newConversation = conversationMapper.toDirectConversationEntity(userId, recipientUserId);
        Conversation savedConversation = conversationRepo.save(newConversation);

        // Create new participant records for both users
        conversationParticipantService.createParticipant(userId, savedConversation.getId());
        conversationParticipantService.createParticipant(recipientUserId, savedConversation.getId());

        log.info("Direct conversation created between user {} and user {} with conversationId {}", userId, recipientUserId, savedConversation.getId());
        
        return toConversationResponse(savedConversation, null);

    }

    public PageResponse<ConversationResponse> getUserConversations(
        String userId,
        int page,
        int size,
        String sortBy,
        String sortDirection
    ) { 
       boolean isParticipantSort = ALLOWED_CONVERSATION_PARTICIPANT_SORT_BY_FIELDS.contains(sortBy);
       
       Set<String> allowedFields = isParticipantSort ? ALLOWED_CONVERSATION_PARTICIPANT_SORT_BY_FIELDS : ALLOWED_CONVERSATION_SORT_BY_FIELDS;
       String defaultField = isParticipantSort ? "joinedAt" : "lastMessageTime";

       Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, allowedFields, defaultField);
       return isParticipantSort 
            ? getUserConversationsSortedByConversationParticipant(userId, page, size, sortBuilt)
            : getUserConversationsSortedByConversation(userId, page, size, sortBuilt);
    }

    public PageResponse<ConversationResponse> searchConversations(
        String userId,
        SearchConversationRequest searchConversationRequest,
        int page,
        int size,
        String sortBy,
        String sortDirection

    ) {
        Query query = new Query();

        query.addCriteria(Criteria.where("userId").is(userId));

        if (searchConversationRequest.getPinned() != null) {
            query.addCriteria(Criteria.where("pinnedAt").exists(searchConversationRequest.getPinned()));
        }

        if (searchConversationRequest.getMuted() != null) {
            query.addCriteria(Criteria.where("mutedAt").exists(searchConversationRequest.getMuted()));
        }

        if (searchConversationRequest.getActive() != null) {
            query.addCriteria(Criteria.where("leftAt").exists(!searchConversationRequest.getActive()));
        }

        if (Boolean.TRUE.equals(searchConversationRequest.getUnreadOnly())) {
            query.addCriteria(Criteria.where("unreadCount").gt(0));
        }

        List<ConversationParticipant> participants = conversationParticipantService.getParticipantsByQuery(
            query
        );

        if (participants.isEmpty()) {
            return PageResponse.<ConversationResponse>builder()
                .currentPage(page + 1)
                .pageSize(size)
                .totalElements(0)
                .totalPages(0)
                .data(List.of())
                .build();
        }

        Map<String, ConversationParticipant> participantsMap = 
            participants.stream()
                .collect(Collectors.toMap(participant -> participant.getConversationId(), Function.identity()));

        // Build conversation query
        Query conversationQuery = new Query();
        List<Criteria> conversationCriteriaList = new ArrayList<>();

        conversationCriteriaList.add(Criteria.where("isDeleted").is(false));

        Set<String> userConversationIds = participantsMap.keySet();

        if (StringUtils.hasText(searchConversationRequest.getKeyWord())) {
            String regex = Pattern.quote(searchConversationRequest.getKeyWord().trim());
            Criteria titleCriteria = Criteria.where("title").regex(regex, "i");

            Set<String> matchedIds = getMatchedConversationIdsByUsername(searchConversationRequest.getKeyWord(), userId);

            // Retain only conversation IDs that the user is a participant of
            matchedIds.retainAll(userConversationIds);

            if (!matchedIds.isEmpty()) {
                // Find conversations that match the title or have participants that match the username
                conversationCriteriaList.add(new Criteria().orOperator(titleCriteria, Criteria.where("_id").in(matchedIds)));
            } else {
                conversationCriteriaList.add(titleCriteria);
            }
        } else {
            conversationCriteriaList.add(Criteria.where("_id").in(userConversationIds));
        }

        if (searchConversationRequest.getConversationType() != null) {
            ConversationType.fromString(searchConversationRequest.getConversationType())
                .ifPresent(type -> conversationCriteriaList.add(Criteria.where("type").is(type)));
        }

        conversationQuery.addCriteria(new Criteria().andOperator(conversationCriteriaList));
        
        // Get total count before pagination
        long totalElements = mongoTemplate.count(conversationQuery, Conversation.class);

        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_CONVERSATION_SORT_BY_FIELDS, "lastMessageTime");
        conversationQuery.with(PageRequest.of(page, size, sortBuilt));

        // Execute query
        List<Conversation> conversations = mongoTemplate.find(conversationQuery, Conversation.class);

        List<ConversationResponse> conversationResponses = conversations.stream()
            .map(conversation -> {
                ConversationParticipant participant = participantsMap.get(conversation.getId());

                return participant == null ? null 
                    : toConversationResponse(conversation, participant);
            })
            .filter(Objects::nonNull)
            .toList();

        return PageResponse.<ConversationResponse>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(totalElements)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .data(conversationResponses)
            .build();
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetail(
        String userId,
        String conversationId
    ) {
        Conversation conversation = conversationRepo.findByIdAndIsDeletedFalse(conversationId)
            .orElseThrow(() -> new AppException("Conversation not found", HttpStatus.NOT_FOUND));

        ConversationParticipant conversationParticipant = conversationParticipantService.getParticipant(userId, conversationId);
        if (conversationParticipant == null || conversationParticipant.getLeftAt() != null) {
            throw new AppException("Conversation not found", HttpStatus.NOT_FOUND);
        }

        List<ConversationParticipant> participants = conversationParticipantService.getParticipantsByConversationId(conversationId);
        
        Map<String, UserInfo> userInfoMap = internalUserService.getUserInfoByIds(
            participants.stream()
                .map(participant -> participant.getUserId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

        List<ConversationParticipantResponse> participantResponses =
            conversationParticipantService.toParticipantResponseList(participants, userInfoMap);

        int unreadCount = conversationParticipant.getUnreadCount();
        Boolean pinned = conversationParticipant.getPinnedAt() != null;
        Boolean muted = conversationParticipant.getMutedAt() != null;
        Boolean hidden = conversationParticipant.getHiddenAt() != null;

        return conversationMapper.toConversationDetailResponse(conversation, participantResponses, unreadCount, pinned, muted, hidden);
    }

    public void hideConversation(
        String userId,
        String conversationId
    ) {
        conversationParticipantService.hideParticipant(userId, conversationId);
    }
    
    public void togglePinConversation(
        String userId, 
        String conversationId,
        Boolean pinned
    ) {
        conversationParticipantService.togglePinParticipant(userId, conversationId, pinned);
    }

    public void toggleMuteConversation(
        String userId, 
        String conversationId,
        Boolean muted
    ) {
        conversationParticipantService.toggleMuteParticipant(userId, conversationId, muted);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void markConversationAsRead(
        String userId,
        String conversationId
    ) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(conversationId).and("isDeleted").is(false));
        query.fields().include("lastMessageId");
        
        Conversation conversation = mongoTemplate.findOne(query, Conversation.class);
        if (conversation == null) {
            throw new AppException("Conversation not found", HttpStatus.NOT_FOUND);
        }

        ConversationParticipant participant = conversationParticipantService.getParticipant(userId, conversationId);
        if (participant == null || participant.getLeftAt() != null) {
            throw new AppException("Conversation not found", HttpStatus.NOT_FOUND);
        }

        String lastMessageId = conversation.getLastMessageId();
        if (lastMessageId != null && !lastMessageId.equals(participant.getLastReadMessageId())) {
            conversationParticipantService.markParticipantAsRead(userId, conversationId, lastMessageId);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS) 
    public void leaveConversation(
        String userId,
        String conversationId
    ) {
        conversationParticipantService.leaveParticipant(userId, conversationId);

        Query query = new Query().addCriteria(Criteria.where("_id").is(conversationId).and("isDeleted").is(false));
        Update update = new Update().pull("participantIds", userId);

        mongoTemplate.updateFirst(query, update, Conversation.class);
    }

    @Transactional(propagation = Propagation.SUPPORTS) 
    public void deleteConversation(String conversationId) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(conversationId).and("isDeleted").is(false));
        Update update = new Update()
            .set("active", false)
            .set("isDeleted", true)
            .set("deletedAt", LocalDateTime.now());

        long matchedCount = mongoTemplate.updateFirst(query, update, Conversation.class).getMatchedCount();
        if (matchedCount == 0) {
            throw new AppException("Conversation not found", HttpStatus.NOT_FOUND);
        }

        conversationParticipantService.leaveAllParticipants(conversationId);
    }

    // Utility methods
    private PageResponse<ConversationResponse> getUserConversationsSortedByConversation(
        String userId,
        int page, int size,
        Sort sorted
    ) {
        Page<Conversation> conversationPage = conversationRepo.findActiveConversationsByUserId(
            userId, 
            PageRequest.of(page, size, sorted)
        );

        List<String> conversationIds = conversationPage.getContent().stream()
            .map(conversation -> conversation.getId())
            .toList();

        List<ConversationParticipant> conversationParticipants = conversationParticipantService.getParticipantsByUserIdAndConversationIds(userId, conversationIds);

        Map<String, ConversationParticipant> conversationParticipantMap = conversationParticipants.stream()
            .collect(Collectors.toMap(participant -> participant.getConversationId(), Function.identity()));

        List<ConversationResponse> conversationResponses = conversationPage.getContent().stream()
            .map(conversation -> {
                ConversationParticipant conversationParticipant = conversationParticipantMap.get(conversation.getId());
                return conversationParticipant == null ? null 
                    : toConversationResponse(conversation, conversationParticipant);
            })
            .filter(Objects::nonNull)
            .toList();

        return PageResponse.<ConversationResponse>builder()
            .currentPage(conversationPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(conversationPage.getSize())
            .totalElements(conversationPage.getTotalElements())
            .totalPages(conversationPage.getTotalPages())
            .data(conversationResponses)
            .build();
    }

    private PageResponse<ConversationResponse> getUserConversationsSortedByConversationParticipant(
        String userId,
        int page, int size,
        Sort sorted
    ) {
        Page<ConversationParticipant> conversationParticipants = 
            conversationParticipantService.getParticipantsByUserId(userId, PageRequest.of(page, size, sorted));

        if (conversationParticipants.isEmpty()) {
            return PageResponse.<ConversationResponse>builder()
                .currentPage(page + 1)
                .pageSize(size)
                .totalElements(0)
                .totalPages(0)
                .data(List.of())
                .build();
        }

        List<String> conversationIds = conversationParticipants.getContent().stream()
            .map(participant -> participant.getConversationId())
            .toList();

        Map<String, Conversation> conversationMap = conversationRepo.findByIdInAndIsDeletedFalse(conversationIds)
            .stream()
            .collect(Collectors.toMap(conversation -> conversation.getId(), Function.identity()));

        List<ConversationResponse> conversationResponses = conversationParticipants.getContent().stream()
            .map(conversationParticipant -> {
                Conversation conversation = conversationMap.get(conversationParticipant.getConversationId());
                return conversation == null ? null 
                    : toConversationResponse(conversation, conversationParticipant);
            })
            .filter(Objects::nonNull)
            .toList();

        return PageResponse.<ConversationResponse>builder()
            .currentPage(conversationParticipants.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(conversationParticipants.getTotalElements())
            .totalPages(conversationParticipants.getTotalPages())
            .data(conversationResponses)
            .build();
    }

    private Set<String> getMatchedConversationIdsByUsername(String keyword, String userId) {
        List<String> participantUserIds = internalUserService.searchUserIdsByUsername(keyword);
        if (participantUserIds.isEmpty()) {
            return Set.of();
        }

        return conversationRepo.findAllDirectConversationsByParticipants(userId, participantUserIds)
            .stream()
            .map(conversation -> conversation.getId())
            .collect(Collectors.toSet());
    }

    private ConversationResponse toConversationResponse(Conversation conversation, ConversationParticipant conversationParticipant) {
        if (conversation == null) {
            return null;
        }

        if (conversationParticipant == null) {
            return conversationMapper.toConversationResponse(conversation);
        }

        if (conversationParticipant.getLeftAt() != null) {
            return null;
        }

        int unreadCount = conversationParticipant.getUnreadCount();
        Boolean pinned = conversationParticipant.getPinnedAt() != null;
        Boolean muted = conversationParticipant.getMutedAt() != null;
        Boolean hidden = conversationParticipant.getHiddenAt() != null;

        return conversationMapper.toConversationResponse(conversation, unreadCount, pinned, muted, hidden);
    }
}