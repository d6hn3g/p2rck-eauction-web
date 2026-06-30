package com.github.dghng36.eauction.modules.finance.transactions.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.core.utils.MetadataUtils;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.modules.finance.enums.TransactionStatus;
import com.github.dghng36.eauction.modules.finance.enums.TransactionType;
import com.github.dghng36.eauction.modules.finance.transactions.dto.request.SearchTransactionsRequest;
import com.github.dghng36.eauction.modules.finance.transactions.dto.request.UpdateTransactionRequest;
import com.github.dghng36.eauction.modules.finance.transactions.dto.response.TransactionAdminResponse;
import com.github.dghng36.eauction.modules.finance.transactions.dto.response.TransactionResponse;
import com.github.dghng36.eauction.modules.finance.transactions.mapper.TransactionMapper;
import com.github.dghng36.eauction.modules.finance.transactions.model.Transaction;
import com.github.dghng36.eauction.modules.finance.transactions.repository.TransactionRepository;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * All transactions should not be deleted (soft deleted),
 * but can be updated to "cancelled" status if needed.
 */

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TransactionService {
    MongoTemplate mongoTemplate;
    TransactionRepository transactionRepo;

    InternalUserService internalUserService;

    TransactionMapper transactionMapper;

    static Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
        "createdAt",
        "updatedAt",
        "amount",
        "status"
    );

    // Methods for MyTransactionController
    public PageResponse<TransactionResponse> getMyTransactions(
        String userId,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<Transaction> transactionPage = transactionRepo.findAllByUserId(
            userId,
            PageRequest.of(page, size, sortBuilt)
        );

        return PageResponse.<TransactionResponse>builder()
            .currentPage(transactionPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(transactionPage.getTotalElements())
            .totalPages(transactionPage.getTotalPages())
            .data(
                transactionMapper.toTransactionResponseList(transactionPage.getContent())
            )
            .build();
    }

    public PageResponse<TransactionResponse> searchMyTransactions(
        String userId,
        SearchTransactionsRequest searchTransactionsRequest,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("userId").is(userId));

        // Add criteria
        applyCriteria(searchTransactionsRequest, criteriaList, false);

        // Execute query
        return executeMyQuery(
            query, criteriaList,
            page, size,
            sortBy, sortDirection
        );
    }

    public TransactionResponse getMyTransaction(String transactionId) {
        return transactionRepo.findById(transactionId)
            .map(transactionMapper::toTransactionResponse)
            .orElseThrow(() -> new AppException("Transaction not found", HttpStatus.NOT_FOUND));
    }

    // Methods for AdminTransactionController
    public PageResponse<TransactionAdminResponse> getAllTransactions(
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<Transaction> transactionPage = transactionRepo.findAll(PageRequest.of(page, size, sortBuilt));

        Map<String, Map<String, String>> userIdToUsernameAndFullName = internalUserService.getUsernameAndFullNameFromUserIds(
            transactionPage.getContent().stream()
                .map(transaction -> transaction.getUserId())
                .collect(Collectors.toSet())
        );
        
        return PageResponse.<TransactionAdminResponse>builder()
            .currentPage(transactionPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(transactionPage.getTotalElements())
            .totalPages(transactionPage.getTotalPages())
            .data(
                transactionMapper.toTransactionAdminResponseList(transactionPage.getContent(), userIdToUsernameAndFullName)
            )
            .build();
    }

    public PageResponse<TransactionAdminResponse> searchAllTransactions(
        SearchTransactionsRequest searchTransactionsRequest,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Add criteria
        applyCriteria(searchTransactionsRequest, criteriaList, true);

        // Execute query
        return executeAdminQuery(
            query, criteriaList,
            page, size,
            sortBy, sortDirection
        );
    }

    public TransactionAdminResponse getUserTransaction(
        String userId, String transactionId
    ) {
        Map<String, Map<String, String>> userIdToUsernameAndFullName = internalUserService.getUsernameAndFullNameFromUserIds(Set.of(userId));

        return transactionRepo.findById(transactionId)
            .filter(transaction -> transaction.getUserId().equals(userId))
            .map(
                transaction -> 
                    transactionMapper.toTransactionAdminResponse(
                        transaction, 
                        userIdToUsernameAndFullName.get(userId)
                    )
                )
            .orElseThrow(() -> new AppException("Transaction not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void updateTransaction(
        String transactionId,
        UpdateTransactionRequest updateTransactionRequest
    ) {
        Transaction transaction = transactionRepo.findById(transactionId)
            .orElseThrow(() -> new AppException("Transaction not found", HttpStatus.NOT_FOUND));

        // Update fields
        if (updateTransactionRequest.getNewStatus() != null) {
            TransactionStatus transNewStatus = TransactionStatus.fromString(updateTransactionRequest.getNewStatus())
                .orElseThrow(() -> new AppException("Invalid transaction status", HttpStatus.BAD_REQUEST));

            transaction.setStatus(transNewStatus);
        }

        if (StringUtils.hasText(updateTransactionRequest.getDescription())) {
            transaction.setDescription(updateTransactionRequest.getDescription());
        }

        if (updateTransactionRequest.getMetadata() != null) {
            Map<String, Object> currentMetadata = transaction.getMetadata() != null
                ? transaction.getMetadata()
                : new LinkedHashMap<>();

            Map<String, Object> newMetadata = MetadataUtils.sanitizeDynamicMetadata(updateTransactionRequest.getMetadata());

            long newKeysCount = newMetadata.keySet().stream()
                .filter(key -> !currentMetadata.containsKey(key))
                .count();

            long overflowCount = currentMetadata.size() + newKeysCount - ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE;

            while (overflowCount > 0 && !currentMetadata.isEmpty()) {
                String oldestKey = currentMetadata.keySet().iterator().next();
                currentMetadata.remove(oldestKey);
                overflowCount--;
            }

            currentMetadata.putAll(newMetadata);
            transaction.setMetadata(currentMetadata);
        }

        transactionRepo.save(transaction);

        log.info("Updated transaction with id: [{}]", transactionId);
    }


    // Utility methods
    private void applyCriteria(
        SearchTransactionsRequest searchTransactionsRequest,
        List<Criteria> criteriaList,
        boolean isAdminSearch
    ) {
        if (StringUtils.hasText(searchTransactionsRequest.getSearchQuery())) {
            String regex = ".*" + Pattern.quote(searchTransactionsRequest.getSearchQuery()) + ".*";

            List<Criteria> orCriteriaList = new ArrayList<>();
            orCriteriaList.add(Criteria.where("description").regex(regex, "i"));
            orCriteriaList.add(Criteria.where("transactionCode").regex(regex, "i"));

            if (isAdminSearch) {
                Set<String> matchedIds = getMatchedUserIdsByUsername(searchTransactionsRequest.getSearchQuery());
                if (!matchedIds.isEmpty()) {
                    criteriaList.add(Criteria.where("userId").in(matchedIds));
                }
            }
            Criteria orCriteria = new Criteria()
                .orOperator(orCriteriaList.toArray(Criteria[]::new));
            criteriaList.add(orCriteria);
        }

        if (searchTransactionsRequest.getType() != null && !searchTransactionsRequest.getType().isEmpty()) {
            List<TransactionType> types = searchTransactionsRequest.getType().stream()
                .map(typeStr -> TransactionType.fromString(typeStr)
                    .orElseThrow(() -> new AppException("Invalid transaction type: " + typeStr, HttpStatus.BAD_REQUEST)))
                .toList();
            criteriaList.add(Criteria.where("type").in(types));
        }

        if (searchTransactionsRequest.getStatus() != null && !searchTransactionsRequest.getStatus().isEmpty()) {
            List<TransactionStatus> statuses = searchTransactionsRequest.getStatus().stream()
                .map(statusStr -> TransactionStatus.fromString(statusStr)
                    .orElseThrow(() -> new AppException("Invalid transaction status: " + statusStr, HttpStatus.BAD_REQUEST)))
                .toList();
            criteriaList.add(Criteria.where("status").in(statuses));
        }

        if (searchTransactionsRequest.getCreatedAtBefore() != null) {
            criteriaList.add(Criteria.where("createdAt").lte(searchTransactionsRequest.getCreatedAtBefore()));
        }

        if (searchTransactionsRequest.getCreatedAtAfter() != null) {
            criteriaList.add(Criteria.where("createdAt").gte(searchTransactionsRequest.getCreatedAtAfter()));
        }

        if (searchTransactionsRequest.getUpdatedAtBefore() != null) {
            criteriaList.add(Criteria.where("updatedAt").lte(searchTransactionsRequest.getUpdatedAtBefore()));
        }

        if (searchTransactionsRequest.getUpdatedAtAfter() != null) {
            criteriaList.add(Criteria.where("updatedAt").gte(searchTransactionsRequest.getUpdatedAtAfter()));
        }
    }

    private record QueryResult(List<Transaction> transactions, long totalElements) {}

    private PageResponse<TransactionResponse> executeMyQuery(
        Query baseQuery, List<Criteria> criteriaList,
        int page, int size, String sortBy, String sortDirection
    ) {
        QueryResult result = executeCommonQuery(baseQuery, criteriaList, page, size, sortBy, sortDirection);
    
        return createPageResponse(page, size, result.totalElements(), 
        transactionMapper.toTransactionResponseList(result.transactions()));
    }

    private PageResponse<TransactionAdminResponse> executeAdminQuery(
        Query baseQuery, List<Criteria> criteriaList,
        int page, int size, String sortBy, String sortDirection
    ) {
        QueryResult result = executeCommonQuery(baseQuery, criteriaList, page, size, sortBy, sortDirection);
        
        Map<String, Map<String, String>> userIdToUsernameAndFullName = internalUserService.getUsernameAndFullNameFromUserIds(
            result.transactions().stream()
                .map(transaction -> transaction.getUserId())
                .collect(Collectors.toSet())
        );

        return createPageResponse(page, size, result.totalElements(), 
        transactionMapper.toTransactionAdminResponseList(result.transactions(), userIdToUsernameAndFullName));
    }

    private <T extends TransactionResponse> PageResponse<T> createPageResponse(
        int page, int size, long totalElements, List<T> data
    ) {
        return PageResponse.<T>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(totalElements)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .data(data)
            .build();
    }

    private QueryResult executeCommonQuery(
        Query baseQuery, List<Criteria> criteriaList,
        int page, int size, String sortBy, String sortDirection
    ) {
        if (!criteriaList.isEmpty()) {
            criteriaList.forEach(baseQuery::addCriteria);
        }

        long totalElements = mongoTemplate.count(Query.of(baseQuery), Transaction.class);

        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        baseQuery.with(PageRequest.of(page, size, sortBuilt));

        List<Transaction> transactions = mongoTemplate.find(baseQuery, Transaction.class);
    
        return new QueryResult(transactions, totalElements);
    }

    private Set<String> getMatchedUserIdsByUsername(String usernameQuery) {
        return internalUserService.searchUserIdsByUsername(usernameQuery).stream()
            .collect(Collectors.toSet());
    }

}
