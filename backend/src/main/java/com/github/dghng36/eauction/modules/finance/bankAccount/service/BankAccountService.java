package com.github.dghng36.eauction.modules.finance.bankAccount.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.request.CreateBankAccountRequest;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.request.SearchBankAccountsRequest;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.request.UpdateBankAccountRequest;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.response.BankAccountResponse;
import com.github.dghng36.eauction.modules.finance.bankAccount.event.RejectedBankAccountEvent;
import com.github.dghng36.eauction.modules.finance.bankAccount.mapper.BankAccountMapper;
import com.github.dghng36.eauction.modules.finance.bankAccount.model.BankAccount;
import com.github.dghng36.eauction.modules.finance.bankAccount.repository.BankAccountRepository;
import com.github.dghng36.eauction.modules.finance.enums.BankAccountStatus;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BankAccountService {
    MongoTemplate mongoTemplate;
    BankAccountRepository bankAccountRepo;

    InternalUserService internalUserService;

    BankAccountMapper bankAccountMapper;

    ApplicationEventPublisher eventPublisher;

    static final Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
        "bankCode", 
        "bankName", 
        "accountHolderName", 
        "status", 
        "createdAt", 
        "updatedAt"
    );

    // Methods for MyBankAccountController
    @Transactional
    public BankAccountResponse createMyBankAccount(
        String userId,
        CreateBankAccountRequest createBankAccountRequest
    ) {
        BankAccount bankAccount = bankAccountMapper.toBankAccountEntity(userId, createBankAccountRequest);

        BankAccount savedBankAccount = bankAccountRepo.save(bankAccount);

        // Get user info from User Service
        UserInfo userInfo = internalUserService.getUserInfoByIds(Set.of(userId)).get(userId);

        log.info("Created bank account with ID: [{}] for user: [{}]", savedBankAccount.getId(), userId);

        return bankAccountMapper.toBankAccountResponse(savedBankAccount, userInfo);
    }

    public PageResponse<BankAccountResponse> getMyBankAccounts(
        String userId,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        // Find my bank accounts
        Page<BankAccount> bankAccountPage = bankAccountRepo.findAllByUserIdAndIsDeletedFalse(
            userId,
            PageRequest.of(page, size, sortBuilt)
        );

        // Get user info from User Service
        Map<String, UserInfo> userInfoMap = internalUserService.getUserInfoByIds(Set.of(userId));

        // Map to response
        return PageResponse.<BankAccountResponse>builder()
            .currentPage(bankAccountPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(bankAccountPage.getTotalElements())
            .totalPages(bankAccountPage.getTotalPages())
            .data(
                bankAccountMapper.toBankAccountResponseList(bankAccountPage.getContent(), userInfoMap)
            )
            .build();
    }

    public PageResponse<BankAccountResponse> searchMyBankAccounts(
        String userId, SearchBankAccountsRequest searchBankAccountsRequest,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Add criteria for userId and isDeleted
        criteriaList.add(Criteria.where("userId").is(userId));
        criteriaList.add(Criteria.where("isDeleted").is(false));

        // Apply search criteria
        applyCriteria(criteriaList, searchBankAccountsRequest, false);

        // Execute query
        return executeQuery(
            query, criteriaList,
            page, size,
            sortBy, sortDirection
        );
    }

    public BankAccountResponse getUserBankAccount(String userId, String bankAccountId) {
        // Find bank account by id and userId
        BankAccount bankAccount = bankAccountRepo.findByIdAndUserIdAndIsDeletedFalse(bankAccountId, userId)
            .orElseThrow(() -> new AppException("Bank account not found", HttpStatus.NOT_FOUND));

        // Get user info from User Service
        UserInfo userInfo = internalUserService.getUserInfoByIds(Set.of(userId)).get(userId);

        return bankAccountMapper.toBankAccountResponse(bankAccount, userInfo);
    }

    @Transactional
    public BankAccountResponse updateMyBankAccount(
        String userId, String bankAccountId, UpdateBankAccountRequest updateBankAccountRequest
    ) {
        // Find bank account by id and userId
        BankAccount bankAccount = bankAccountRepo.findByIdAndUserIdAndIsDeletedFalse(bankAccountId, userId)
            .orElseThrow(() -> new AppException("Bank account not found", HttpStatus.NOT_FOUND));

        // Get bank account is default
        BankAccount defaultBankAccount = bankAccountRepo.findByUserIdAndIsDefaultTrueAndIsDeletedFalse(userId)
            .orElse(null);
        
        // Update bank account fields
        if (StringUtils.hasText(updateBankAccountRequest.getBankCode())) {
            bankAccount.setBankCode(updateBankAccountRequest.getBankCode());
        }

        if (StringUtils.hasText(updateBankAccountRequest.getBankName())) {
            bankAccount.setBankName(updateBankAccountRequest.getBankName());
        }

        if (StringUtils.hasText(updateBankAccountRequest.getAccountNumber())) {
            bankAccount.setAccountNumber(updateBankAccountRequest.getAccountNumber());
        }

        if (StringUtils.hasText(updateBankAccountRequest.getAccountHolderName())) {
            bankAccount.setAccountHolderName(updateBankAccountRequest.getAccountHolderName());
        }

        if (updateBankAccountRequest.getIsDefault() != null) {
            bankAccount.setIsDefault(updateBankAccountRequest.getIsDefault());

            if (defaultBankAccount != null && updateBankAccountRequest.getIsDefault() && !defaultBankAccount.getId().equals(bankAccountId)) {
                defaultBankAccount.setIsDefault(false);
                bankAccountRepo.save(defaultBankAccount);
            } else if (defaultBankAccount != null && !updateBankAccountRequest.getIsDefault() && defaultBankAccount.getId().equals(bankAccountId)) {
                bankAccount.setIsDefault(true);
            }
        }

        // Save updated bank account
        BankAccount updatedBankAccount = bankAccountRepo.save(bankAccount);

        // Get user info from User Service
        UserInfo userInfo = internalUserService.getUserInfoByIds(Set.of(userId)).get(userId);

        log.info("Updated bank account with ID: [{}] for userId: [{}]", bankAccountId, userId);

        return bankAccountMapper.toBankAccountResponse(updatedBankAccount, userInfo);
    }

    public void deleteMyBankAccount(String userId, String bankAccountId) {
        Query query = new Query(
            Criteria.where("id").is(bankAccountId)
                    .and("userId").is(userId)
                    .and("isDeleted").is(false)
        );

        Instant now = Instant.now();
        Update update = new Update()
            .set("isDeleted", true)
            .set("deletedAt", now);

        UpdateResult result = mongoTemplate.updateFirst(query, update, BankAccount.class);

        if (result.getMatchedCount() == 0) {
            log.warn(" User: [{}] critical tried to delete non-existent, already deleted, or unauthorized bank account: [{}]", userId, bankAccountId);
            throw new AppException("Bank account not found", HttpStatus.NOT_FOUND);
        }

        log.info("Deleted bank account with ID: [{}] for user: [{}]", bankAccountId, userId);
    }

    // Methods for AdminBankAccountController
    public PageResponse<BankAccountResponse> getAllBankAccounts(
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        // Find all bank accounts
        Page<BankAccount> bankAccountPage = bankAccountRepo.findAllByIsDeletedFalse(
            PageRequest.of(page, size, sortBuilt)
        );

        // Get user info from User Service
        Set<String> userIds = bankAccountPage.getContent().stream()
            .map(bankAccount -> bankAccount.getUserId())
            .collect(Collectors.toSet());
        Map<String, UserInfo> userInfoMap = internalUserService.getUserInfoByIds(userIds);

        // Map to response
        return PageResponse.<BankAccountResponse>builder()
            .currentPage(bankAccountPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(bankAccountPage.getTotalElements())
            .totalPages(bankAccountPage.getTotalPages())
            .data(
                bankAccountMapper.toBankAccountResponseList(bankAccountPage.getContent(), userInfoMap)
            )
            .build();
    }

    public PageResponse<BankAccountResponse> searchAllBankAccounts(
        SearchBankAccountsRequest searchBankAccountsRequest,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Add criteria for isDeleted
        criteriaList.add(Criteria.where("isDeleted").is(false));

        // Apply search criteria
        applyCriteria(criteriaList, searchBankAccountsRequest, true);

        // Execute query
        return executeQuery(
            query, criteriaList,
            page, size,
            sortBy, sortDirection
        );
    }

    public void verifyBankAccount(String bankAccountId) {
        Query query = new Query(
            Criteria.where("id").is(bankAccountId).and("isDeleted").is(false)
        );

        Update update = new Update()
            .set("status", BankAccountStatus.VERIFIED)
            .set("verifiedAt", Instant.now());

        BankAccount updatedAccount = mongoTemplate.findAndModify(
            query, 
            update, 
            FindAndModifyOptions.options().returnNew(true),
            BankAccount.class
        );

        if (updatedAccount == null) {
            throw new AppException("Bank account not found", HttpStatus.NOT_FOUND);
        }

        eventPublisher.publishEvent(
            RejectedBankAccountEvent.builder()
                .userId(updatedAccount.getUserId())
                .bankAccountId(bankAccountId)
        );

        log.info("Verified bank account with ID: [{}]", bankAccountId);
    }

    public void rejectBankAccount(String bankAccountId) {
        Query query = new Query(
            Criteria.where("id").is(bankAccountId).and("isDeleted").is(false)
        );

        Update update = new Update()
            .set("status", BankAccountStatus.REJECTED)
            .set("verifiedAt", Instant.now());

        BankAccount updatedAccount = mongoTemplate.findAndModify(
            query, 
            update, 
            FindAndModifyOptions.options().returnNew(true),
            BankAccount.class
        );

        if (updatedAccount == null) {
            throw new AppException("Bank account not found", HttpStatus.NOT_FOUND);
        }

        eventPublisher.publishEvent(
            RejectedBankAccountEvent.builder()
                .userId(updatedAccount.getUserId())
                .bankAccountId(bankAccountId)
        );

        log.info("Rejected bank account with ID: [{}]", bankAccountId);
    }

    // Utility methods
    private void applyCriteria(
        List<Criteria> criteriaList,
        SearchBankAccountsRequest searchBankAccountsRequest,
        boolean isAdminSearch
    ) {
        if (StringUtils.hasText(searchBankAccountsRequest.getSearchQuery())) {
            String regex = ".*" + Pattern.quote(searchBankAccountsRequest.getSearchQuery()) + ".*";

            List<Criteria> orCriteriaList = new ArrayList<>();
            orCriteriaList.add(Criteria.where("bankCode").regex(regex, "i"));
            orCriteriaList.add(Criteria.where("bankName").regex(regex, "i"));
            orCriteriaList.add(Criteria.where("accountNumber").regex(regex, "i"));
            orCriteriaList.add(Criteria.where("accountHolderName").regex(regex, "i"));
            if (isAdminSearch) {
                Set<String> matchedUserIds = getMatchUserIdsByUsername(regex);
                if (!matchedUserIds.isEmpty()) {
                    orCriteriaList.add(Criteria.where("userId").in(matchedUserIds));
                }
            }
            criteriaList.add(new Criteria().orOperator(orCriteriaList.toArray(Criteria[]::new)));
        }

        if (StringUtils.hasText(searchBankAccountsRequest.getBankName())) {
            criteriaList.add(Criteria.where("bankName").is(searchBankAccountsRequest.getBankName()));
        }

        if (searchBankAccountsRequest.getBankAccountStatus() != null) {
            BankAccountStatus bankAccountStatus = BankAccountStatus.fromString(searchBankAccountsRequest.getBankAccountStatus())
                .orElseThrow(() -> new AppException("Invalid bank account status", HttpStatus.BAD_REQUEST));

            criteriaList.add(Criteria.where("status").is(bankAccountStatus));
        }

        if (searchBankAccountsRequest.getCreatedAtBefore() != null) {
            criteriaList.add(Criteria.where("createdAt").lte(searchBankAccountsRequest.getCreatedAtBefore()));
        }

        if (searchBankAccountsRequest.getCreatedAtAfter() != null) {
            criteriaList.add(Criteria.where("createdAt").gte(searchBankAccountsRequest.getCreatedAtAfter()));
        }

        if (searchBankAccountsRequest.getUpdatedAtBefore() != null) {
            criteriaList.add(Criteria.where("updatedAt").lte(searchBankAccountsRequest.getUpdatedAtBefore()));
        }

        if (searchBankAccountsRequest.getUpdatedAtAfter() != null) {
            criteriaList.add(Criteria.where("updatedAt").gte(searchBankAccountsRequest.getUpdatedAtAfter()));
        }

        if (searchBankAccountsRequest.getVerifiedAtBefore() != null) {
            criteriaList.add(Criteria.where("verifiedAt").lte(searchBankAccountsRequest.getVerifiedAtBefore()));
        }

        if (searchBankAccountsRequest.getVerifiedAtAfter() != null) {
            criteriaList.add(Criteria.where("verifiedAt").gte(searchBankAccountsRequest.getVerifiedAtAfter()));
        }
    }

    private PageResponse<BankAccountResponse> executeQuery(
        Query baseQuery, List<Criteria> criteriaList,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        if (!criteriaList.isEmpty()) {
            criteriaList.forEach(baseQuery::addCriteria);
        }

        long totalElements = mongoTemplate.count(baseQuery, BankAccount.class);

        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        baseQuery.with(sortBuilt);

        // Execute query
        List<BankAccount> bankAccounts = mongoTemplate.find(baseQuery, BankAccount.class);

        // Get user infos
        Map<String, UserInfo> userInfoMap = internalUserService.getUserInfoByIds(
            bankAccounts.stream().map(bankAccount -> bankAccount.getUserId()).collect(Collectors.toSet())
        );

        // Map to response
        return PageResponse.<BankAccountResponse>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(totalElements)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .data(
                bankAccountMapper.toBankAccountResponseList(bankAccounts, userInfoMap)
            )
            .build();
    }

    private Set<String> getMatchUserIdsByUsername(String regex) {
        return internalUserService.searchUserIdsByUsername(regex).stream()
            .collect(Collectors.toSet());
    }
    
}
