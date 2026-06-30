package com.github.dghng36.eauction.modules.identity.user.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.ReputationUtils;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.identity.enums.UserStatus;
import com.github.dghng36.eauction.modules.identity.helper.PIICryptoHelper;
import com.github.dghng36.eauction.modules.identity.helper.PasswordHelper;
import com.github.dghng36.eauction.modules.identity.reputation.service.ReputationProcessor;
import com.github.dghng36.eauction.modules.identity.user.dto.request.ChangePasswordRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.RegisterRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.SearchManagedUsersRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.SearchPublicUsersRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.UpdateMyProfileRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.UpdateUserRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.response.UserResponse;
import com.github.dghng36.eauction.modules.identity.user.mapper.UserMapper;
import com.github.dghng36.eauction.modules.identity.user.model.User;
import com.github.dghng36.eauction.modules.identity.user.repository.UserRepository;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;
import com.github.dghng36.eauction.modules.media.dto.request.MediaFileUploadRequest;
import com.github.dghng36.eauction.modules.media.service.InternalMediaService;
import com.mongodb.WriteConcern;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    MongoTemplate mongoTemplate;
    UserRepository userRepo;

    InternalMediaService internalMediaService;
    InternalWalletService internalWalletService;
    ReputationProcessor reputationProcessor;

    UserMapper userMapper;
    PasswordHelper passwordHelper;
    PIICryptoHelper piiCryptoHelper;
    JobExecutorTasks jobExecutorTasks;

    static Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
        "username",
        "email",
        "fullName",
        "phoneNumber",
        "nationality",
        "reputation",
        "auctionStatus.totalBids",
        "auctionStatus.totalWins",
        "auctionStatus.totalAuctionRoomsCreated",
        "auctionStatus.totalAuctionRoomsJoined",
        "createdAt",
        "updatedAt"
    );
    
    // Public user methods
    @Transactional
    public UserResponse registerUser(RegisterRequest registerRequest) {
        // Check if user exists
        String usernameIdentifier = registerRequest.getUsername().trim();
        String emailIdentifier = registerRequest.getEmail().trim();
        String phoneIdentifier = registerRequest.getPhoneNumber().trim();
        
        if (userRepo.existsByUsernameOrEmailOrPhoneNumber(usernameIdentifier, emailIdentifier, phoneIdentifier)) {
            log.warn("Attempt to register with existing username, email, or phone number: [{}], [{}], [{}]", 
                usernameIdentifier, emailIdentifier, phoneIdentifier);

            throw new AppException("Username, email, or phone number already exists", HttpStatus.BAD_REQUEST);
        }

        // Hash password
        String hashedPassword = passwordHelper.hashPassword(registerRequest.getPassword());

        // Encode national ID
        String encodedNationalId = piiCryptoHelper.encodeNationalId(registerRequest.getNationalId());

        // Create new user
        User newUser = userMapper.toUserEntity(registerRequest, hashedPassword, encodedNationalId);

        // Save user to database
        mongoTemplate.setWriteConcern(WriteConcern.MAJORITY);

        User savedUser = mongoTemplate.save(newUser);

        log.info("New user has created: [{}]", savedUser.getId());

        // Run async tasks

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    triggerPostRegistrationTasks(savedUser);
                }
            });
            
        } else {
            triggerPostRegistrationTasks(savedUser);
        }

        log.info("Registration completed for user with ID [{}]", savedUser.getId());

        // Map to response DTO
        return userMapper.toUserResponse(savedUser);
    }

    public PageResponse<UserResponse> getPublicUsers(
        int page, int size, 
        String sortBy, String sortDirection
    ) {
        // Validate sortBy field to prevent injection
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        // Find users with pagination and sorting
        Page<User> userPage = userRepo.findAllByIsDeletedFalse(PageRequest.of(page, size, sortBuilt));
        
        // Map to response DTOs
        List<UserResponse> userResponses = userMapper.toUserResponseList(userPage.getContent());

        return PageResponse.<UserResponse>builder()
            .currentPage(userPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(userPage.getTotalPages())
            .totalElements(userPage.getTotalElements())
            .data(userResponses)
            .build();
    }

    public PageResponse<UserResponse> searchPublicUsers(SearchPublicUsersRequest searchPublicUsersRequest, 
        int page, int size, 
        String sortBy, String sortDirection
    ) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        applyPublicCriteria(searchPublicUsersRequest, criteriaList);

        return executeSearchQuery(query, criteriaList, page, size, sortBy, sortDirection);
    }

    public UserResponse getUserProfile(String username) {
        User user = userRepo.findByUsernameAndIsDeletedFalse(username)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    // My profile methods
    public UserResponse getMyProfile(String userId) {
        User user = userRepo.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateMyProfile(String userId, UpdateMyProfileRequest updateMyProfileRequest) {
        // Find user by ID
        User user = userRepo.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        
        // Update user fields
        userMapper.updateUserEntity(user, updateMyProfileRequest);

        // Save updated user to database
        User updatedUser = userRepo.save(user);

        log.info("User's profile updated for user with ID [{}]", updatedUser.getId());

        return userMapper.toUserResponse(updatedUser);
    }

    @Transactional
    public void updatePassword(String userId, ChangePasswordRequest changePasswordRequest) {
        // Find user by ID
        User user = userRepo.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        // Check if current password matches
        if (!passwordHelper.matchPassword(changePasswordRequest.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        if (passwordHelper.matchPassword(changePasswordRequest.getNewPassword(), user.getPasswordHash())) {
            throw new AppException("New password cannot be the same as the current password", HttpStatus.BAD_REQUEST);
        }

        // Update password
        user.setPasswordHash(
            passwordHelper.hashPassword(changePasswordRequest.getNewPassword())
        );
        userRepo.save(user);
        log.info("User's password updated for user with ID [{}]", user.getId());
    }
    
    @Transactional
    public void updateAvatar(String userId, MediaFileUploadRequest mediaFileRequest) {
        // Find user by ID
        User user = userRepo.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        // Check current avatar and delete if exists
        if (user.getAvatar() != null) {
            internalMediaService.delete(user.getAvatar().getObjectKey());
        }

        // Get Media URL from media code
        String originalUrl = internalMediaService.resolve(mediaFileRequest.getMediaCode());

        // Update avatar
        MediaFile newAvatar = MediaFile.builder()
            .mediaCode(mediaFileRequest.getMediaCode())
            .objectKey(mediaFileRequest.getObjectKey())
            .originalUrl(originalUrl)
            .build();

        user.setAvatar(newAvatar);
        userRepo.save(user);
        log.info("User's avatar updated for user with ID [{}]", user.getId());
    }  

    // Admin methods
    public PageResponse<UserResponse> getManagedUsers(
        int page, int size, 
        String sortBy, String sortDirection
    ) {
        // Validate sortBy field to prevent injection
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<User> userPage = userRepo.findAllByIsDeletedFalse(PageRequest.of(page, size, sortBuilt));
        List<UserResponse> userResponses = userMapper.toUserResponseList(userPage.getContent());

        return PageResponse.<UserResponse>builder()
            .currentPage(userPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(userPage.getTotalPages())
            .totalElements(userPage.getTotalElements())
            .data(userResponses)
            .build();
    }

    public PageResponse<UserResponse> searchManagedUsers(SearchManagedUsersRequest searchManagedUsersRequest, int page, int size, String sortBy, String sortDirection) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        applyPublicCriteria(searchManagedUsersRequest, criteriaList);

        // Apply admin-specific criteria
        if (searchManagedUsersRequest.getRoles() != null && !searchManagedUsersRequest.getRoles().isEmpty()) {
            List<UserRole> roles = searchManagedUsersRequest.getRoles().stream()
                .map(roleStr -> UserRole.fromString(roleStr)
                    .orElseThrow(() -> new AppException("Invalid role: " + roleStr, HttpStatus.BAD_REQUEST))
                )
                .toList();

            criteriaList.add(Criteria.where("role").in(roles));
        }

        if (searchManagedUsersRequest.getStatuses() != null && !searchManagedUsersRequest.getStatuses().isEmpty()) {
            List<UserStatus> statuses = searchManagedUsersRequest.getStatuses().stream()
                .map(statusStr -> UserStatus.fromString(statusStr)
                    .orElseThrow(() -> new AppException("Invalid status: " + statusStr, HttpStatus.BAD_REQUEST))
                )
                .toList();

            criteriaList.add(Criteria.where("status").in(statuses));
        }

        if (searchManagedUsersRequest.getCreatedAfter() != null) {
            criteriaList.add(Criteria.where("createdAt").gte(searchManagedUsersRequest.getCreatedAfter()));
        }

        if (searchManagedUsersRequest.getCreatedBefore() != null) {
            criteriaList.add(Criteria.where("createdAt").lte(searchManagedUsersRequest.getCreatedBefore()));
        }

        return executeSearchQuery(query, criteriaList, page, size, sortBy, sortDirection);
    }

    @Transactional
    public void updateUser(String userId, UpdateUserRequest updateUserRequest) {
        // Find user by ID
        User user = userRepo.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        // Change string fields to entity
        UserRole newRole;
        UserStatus newStatus;
        
        // Update user fields
        if (updateUserRequest.getRole() != null) {
            newRole = UserRole.fromString(updateUserRequest.getRole())
                .orElse(UserRole.USER);
            user.setRole(newRole);
        }

        if (updateUserRequest.getStatus() != null) {
            newStatus = UserStatus.fromString(updateUserRequest.getStatus())
                .orElse(UserStatus.PENDING);
            user.setStatus(newStatus);
        }

        // Save updated user to database
        userRepo.save(user);

        log.info("Updated for user with ID [{}], New Role: [{}], New Status: [{}]", user.getId(), user.getRole(), user.getStatus());
    }

    @Transactional
    public void deleteUser(String userId) {
        // Find user by ID
        User user = userRepo.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        
        if (UserRole.ADMIN.equals(user.getRole())) {
            throw new AppException("Cannot delete admin user", HttpStatus.BAD_REQUEST);
        }

        // Soft delete by setting isDeleted flag
        user.setIsDeleted(true);
        user.setDeletedAt(LocalDateTime.now());

        jobExecutorTasks.runAsync(() -> internalWalletService.deleteUserWallet(user.getId()));

        userRepo.save(user);

        log.info("User deleted for user with ID [{}]", user.getId());
    }

    @Transactional
    public void updateUserReputation(String userId, double reputationChange) {
        // Find user by ID
        User user = userRepo.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        // Update user reputation
        double newReputation = user.getReputation() + reputationChange;
        ReputationUtils.validateReputationScore(newReputation);
        newReputation = ReputationUtils.clampReputation(newReputation);

        user.setReputation(newReputation);

        userRepo.save(user);
        
        log.info("User's reputation updated for user with ID [{}]. New reputation: [{}]", user.getId(), newReputation);
    }

    // Utility methods for this service
    /** 
     * Apply public search criteria to the criteria list based on the search request.
     * This includes both the basic search query and the specific filters for public search.
    */
    private void applyPublicCriteria(SearchPublicUsersRequest searchPublicUsersRequest, List<Criteria> criteriaList) {
        criteriaList.add(Criteria.where("isDeleted").is(false));
        
        if (StringUtils.hasText(searchPublicUsersRequest.getSearchQuery())) {
            String regex = ".*" + Pattern.quote(searchPublicUsersRequest.getSearchQuery()) + ".*";
            criteriaList.add(new Criteria().orOperator(
                Criteria.where("username").regex(regex, "i"),
                Criteria.where("email").regex(regex, "i"),
                Criteria.where("fullName").regex(regex, "i"),
                Criteria.where("phoneNumber").regex(regex, "i"),
                Criteria.where("address").regex(regex, "i"),
                Criteria.where("nationality").regex(regex, "i")
            ));
        }

        // Filter exact match fields
        if (StringUtils.hasText(searchPublicUsersRequest.getNationality())) {
            criteriaList.add(Criteria.where("nationality").is(searchPublicUsersRequest.getNationality()));
        }

        // Filter range fields
        if (searchPublicUsersRequest.getYearOfBirth() != null) {
            LocalDate startOfYear = LocalDate.of(searchPublicUsersRequest.getYearOfBirth(), 1, 1);
            LocalDate endOfYear = LocalDate.of(searchPublicUsersRequest.getYearOfBirth(), 12, 31);
            criteriaList.add(Criteria.where("dateOfBirth").gte(startOfYear).lte(endOfYear));
        }

        Criteria reputationCriteria = new Criteria("reputation");
        boolean hasMin = searchPublicUsersRequest.getMinReputation() != null;
        boolean hasMax = searchPublicUsersRequest.getMaxReputation() != null;

        if (hasMin) {
            reputationCriteria.gte(searchPublicUsersRequest.getMinReputation());
        }

        if (hasMax) {
            reputationCriteria.lte(searchPublicUsersRequest.getMaxReputation());
        }

        if (hasMin || hasMax) {
            criteriaList.add(reputationCriteria);
        }
    }

    private PageResponse<UserResponse> executeSearchQuery(
        Query baseQuery, List<Criteria> criteriaList,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Combine criteria
        if (!criteriaList.isEmpty()) {
            criteriaList.forEach(baseQuery::addCriteria);
        }

        // Count total elements for pagination
        long totalElements = mongoTemplate.count(baseQuery, User.class);

        Query pageableQuery = Query.of(baseQuery);
        
        // Validate sortBy field to prevent injection
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        pageableQuery.with(PageRequest.of(page, size, sortBuilt));

        // Execute query
        List<User> users = mongoTemplate.find(pageableQuery, User.class);
        
        // Map to response DTOs
        List<UserResponse> userResponses = userMapper.toUserResponseList(users);
        
        return PageResponse.<UserResponse>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .totalElements(totalElements)
            .data(userResponses)
            .build();
    }

    private void triggerPostRegistrationTasks(User savedUser) {
        CompletableFuture<Void> bonusTask = jobExecutorTasks.runAsync(() -> {
            log.info("Awarding async welcome bonus to user with ID [{}]", savedUser.getId());
            reputationProcessor.awardWelcomeBonus(savedUser);
        });

        CompletableFuture<Void> walletTask = jobExecutorTasks.runAsync(() -> {
            log.info("Creating async wallet for user with ID [{}]", savedUser.getId());
            internalWalletService.createUserWallet(savedUser.getId());
        });

        CompletableFuture.allOf(bonusTask, walletTask)
            .thenRun(() -> log.info("Post-registration background tasks completed for user: [{}]", savedUser.getId()))
            .exceptionally(ex -> {
                log.error("Critical to execute post-registration tasks for user [{}]: ", savedUser.getId(), ex);
                return null;
            });
    }
}
