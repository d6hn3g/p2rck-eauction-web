package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.user.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
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
import com.github.dghng36.eauction.modules.identity.user.service.UserService;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;
import com.github.dghng36.eauction.modules.media.dto.request.MediaFileUploadRequest;
import com.github.dghng36.eauction.modules.media.service.InternalMediaService;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private UserRepository userRepo;
    @Mock private PasswordHelper passwordHelper;
    @Mock private PIICryptoHelper piiCryptoHelper;
    @Mock private UserMapper userMapper;
    @Mock private InternalMediaService internalMediaService;
    @Mock private ReputationProcessor reputationProcessor;
    @Mock private InternalWalletService internalWalletService;
    @Mock private JobExecutorTasks jobExecutorTasks;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private User mockUser;
    private UserResponse mockUserResponse;

    private User mockUser1, mockUser2, mockUser3;
    private UserResponse mockUserResponse1, mockUserResponse2, mockUserResponse3;

    private SearchPublicUsersRequest searchPublicUsersRequest1, searchPublicUsersRequest2, searchPublicUsersRequest3, searchPublicUsersRequest4;

    private UpdateMyProfileRequest updateMyProfileRequest;

    private SearchManagedUsersRequest searchManagedUsersRequest;

    private final static Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
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

    @BeforeEach
    void setUp() {
       Mockito.lenient().when(jobExecutorTasks.runAsync(any(Runnable.class)))
            .thenAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                CompletableFuture.runAsync(task); 
                return CompletableFuture.completedFuture(null);
            });

        // Common setup for registerUser tests
        registerRequest = RegisterRequest.builder()
                .username("test_user_reg")
                .password("TestUserRegister")
                .email("test-user-register@test.com")
                .fullName("TestUserRegister")
                .phoneNumber("0123456789")
                .nationalId("123456789")
                .idIssueDate(LocalDate.of(2022, 5, 20))
                .idIssuePlace("Ha Noi")
                .address("123 Street, HCM")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .nationality("Vietnam")
                .build();

        mockUser = new User();
        mockUser.setId("userId123");
        mockUser.setUsername("test_user_reg");

        mockUserResponse = UserResponse.builder()
                .id("userId123")
                .username("test_user_reg")
                .build();

        // Setup for getPublicUsers tests
        mockUser1 = User.builder()
                .id("userId1")
                .username("public_user_1")
                .nationality("VietNam")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .build();

        mockUser2 = User.builder()
                .id("userId2")
                .username("public_user_2")
                .nationality("USA")
                .dateOfBirth(LocalDate.of(1997, 2, 15))
                .build();

        mockUser3 = User.builder()
                .id("userId3")
                .username("public_user_3")
                .nationality("VietNam")
                .dateOfBirth(LocalDate.of(2000, 1, 13))
                .build();

        mockUserResponse1 = UserResponse.builder()
                .id("userId1")
                .username("public_user_1")
                .nationality("VietNam")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .build();

        mockUserResponse2 = UserResponse.builder()
                .id("userId2")
                .username("public_user_2")
                .nationality("USA")
                .dateOfBirth(LocalDate.of(1997, 2, 15))
                .build();

        mockUserResponse3 = UserResponse.builder()
                .id("userId3")
                .username("public_user_3")
                .nationality("VietNam")
                .dateOfBirth(LocalDate.of(2000, 1, 13))
                .build();

        // Setup for searchPublicUsers tests
        searchPublicUsersRequest1 = SearchPublicUsersRequest.builder()
                .build(); // Empty search criteria, should return all users

        searchPublicUsersRequest2 = SearchPublicUsersRequest.builder()
                .searchQuery("pub")
                .build();

        searchPublicUsersRequest3 = SearchPublicUsersRequest.builder()
                .nationality("VietNam")
                .yearOfBirth(2000)
                .build();

        searchPublicUsersRequest4 = SearchPublicUsersRequest.builder()
                .minReputation(4.0)
                .maxReputation(5.0)
                .build();

        // Setup for updateMyProfile tests
        updateMyProfileRequest = UpdateMyProfileRequest.builder()
                .username("Updated Name")
                .email("updated-user@test.com")
                .dateOfBirth(LocalDate.of(1998, 1, 4))
                .build();

        // Setup for searchManagedUsers
        searchManagedUsersRequest = SearchManagedUsersRequest.builder()
            .roles(List.of("MANaGER", "USER"))
            .statuses(List.of("PENDING", "VERIFIED"))
            .build();
    }

    /**
     * Test cases for registerUser method:
     * registerUser_Success_ShouldReturnUserResponse: Tests that a user 
     * can be successfully registered and that the correct UserResponse is returned.
     * registerUser_WhenIdentifierExists_ShouldThrowAppException: Tests that if 
     * the username, email, or phone number already exists, an AppException is thrown with the appropriate message and status code.
     */

    @Test
    void registerUser_Success_ShouldReturnUserResponse() {
        // Arrange
        when(userRepo.existsByUsernameOrEmailOrPhoneNumber(anyString(), anyString(), anyString())).thenReturn(false);
        when(passwordHelper.hashPassword(registerRequest.getPassword())).thenReturn("hashed_password");
        when(piiCryptoHelper.encodeNationalId(registerRequest.getNationalId())).thenReturn("encoded_id");
        when(userMapper.toUserEntity(eq(registerRequest), eq("hashed_password"), eq("encoded_id"))).thenReturn(mockUser);
        when(mongoTemplate.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(mockUserResponse);

        // Act
        UserResponse response = userService.registerUser(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("userId123");
        verify(userRepo, times(1)).existsByUsernameOrEmailOrPhoneNumber(anyString(), anyString(), anyString());
        verify(mongoTemplate, times(1)).save(mockUser);
    }

    @Test
    void registerUser_WhenIdentifierExists_ShouldThrowAppException() {
        // Arrange
        when(userRepo.existsByUsernameOrEmailOrPhoneNumber(anyString(), anyString(), anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(registerRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Username, email, or phone number already exists")
                .extracting(ex -> ((AppException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(mongoTemplate, never()).save(any(User.class));
        verify(reputationProcessor, never()).awardWelcomeBonus(any(User.class));
    }

    /**
     * Test cases for getPublicUsers
     * getPublicUsers_ShouldReturnListOfUserResponses: Tests that the method returns a list of UserResponse 
     * objects based on the users retrieved from the repository.
     * getPublicUsers_WhenParamsAreProvided_ShouldPassParamsToRepository: Tests that if pagination and sorting parameters are provided, 
     * they are correctly passed to the repository method.
     * getPublicUsers_WhenNoUsers_ShouldReturnEmptyList: Tests that if there are no 
     * users in the repository, an empty list is returned.
     * getPublicUSers_WhenRepositoryThrowsException_ShouldPropagateException: Tests that if the repository throws an exception, 
     * it is propagated by the service method.
     */
    @Test
    void getPublicUsers_ShouldReturnListOfUserResponses() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";
        Sort expectedSort = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(page, size, expectedSort);
        List<User> mockUsers = List.of(mockUser1, mockUser2, mockUser3);
        Page<User> mockUserPage = new PageImpl<>(mockUsers, expectedPageRequest, mockUsers.size());
        List<UserResponse> expectedResponses = List.of(mockUserResponse1, mockUserResponse2, mockUserResponse3);

        when(userRepo.findAllByIsDeletedFalse(expectedPageRequest)).thenReturn(mockUserPage);
        when(userMapper.toUserResponseList(mockUsers)).thenReturn(expectedResponses);

        PageResponse<UserResponse> result = userService.getPublicUsers(page, size, sortBy, sortDirection);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getData()).isEqualTo(expectedResponses);
    }

    @Test
    void getPublicUsers_WhenParamsAreProvided_ShouldPassParamsToRepository() {
        // Arrange
        int page = 2, size = 25;
        String sortBy = "username", sortDirection = "asc";

        when(userRepo.findAllByIsDeletedFalse(any(PageRequest.class)))
            .thenReturn(Page.empty());
        when(userMapper.toUserResponseList(any())).thenReturn(List.of());

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.of(0, 1).getClass());

        // Act
        userService.getPublicUsers(page, size, sortBy, sortDirection);

        // Assert
        verify(userRepo).findAllByIsDeletedFalse(pageRequestCaptor.capture());
        PageRequest capturedPageRequest = pageRequestCaptor.getValue();

        assertThat(capturedPageRequest.getPageNumber()).isEqualTo(page);
        assertThat(capturedPageRequest.getPageSize()).isEqualTo(size);
    
        Sort expectedSort = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        assertThat(capturedPageRequest.getSort()).isEqualTo(expectedSort);
    }

    @Test
    void getPublicUsers_WhenNoUsers_ShouldReturnEmptyList() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        Sort expectedSort = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(page, size, expectedSort);

        when(userRepo.findAllByIsDeletedFalse(expectedPageRequest)).thenReturn(Page.empty());

        // Act
        PageResponse<UserResponse> result = userService.getPublicUsers(page, size, sortBy, sortDirection);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isEmpty();

        verify(userRepo, times(1)).findAllByIsDeletedFalse(expectedPageRequest);
        verify(userMapper, times(1)).toUserResponseList(any());
    }

    @Test
    void getPublicUSers_WhenRepositoryThrowsException_ShouldPropagateException() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        Sort expectedSort = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(page, size, expectedSort);

        when(userRepo.findAllByIsDeletedFalse(expectedPageRequest)).thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> userService.getPublicUsers(page, size, sortBy, sortDirection))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database error");

        verify(userRepo, times(1)).findAllByIsDeletedFalse(expectedPageRequest);
        verify(userMapper, never()).toUserResponseList(any());
    }

    /**
     * Test cases for searchPublicUsers
     * searchPublicUsers_ShouldReturnListOfUserResponses: Tests that the 
     * method returns a list of UserResponse
     * searchPublicUsers_WhenSearchQueryProvided_ShouldSearchByQuery: Tests that if a search query is provided, it is used to filter users based on multiple fields.
     * searchPublicUsers_WhenHasFilter_ShouldApplyFilters: Tests that if specific 
     * filters like nationality
     * searchPublicUsers_WhenNoUsers_ShouldReturnEmptyList: Tests that if there are no 
     * users matching the search criteria, an empty list is returned.
     * searchPublicUsers_WhenRepositoryThrowsException_ShouldPropagateException: Tests that if the repository throws an exception, it is propagated by the service method.
     */
    @Test
    void searchPublicUsers_ShouldReturnListOfUserResponses() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";
        List<User> mockUsers = List.of(mockUser1, mockUser2, mockUser3);
        List<UserResponse> expectedResponses = List.of(mockUserResponse1, mockUserResponse2, mockUserResponse3);

        when(mongoTemplate.count(any(Query.class), eq(User.class))).thenReturn((long) mockUsers.size());
        when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(mockUsers);
        when(userMapper.toUserResponseList(mockUsers)).thenReturn(expectedResponses);

        PageResponse<UserResponse> result = userService.searchPublicUsers(searchPublicUsersRequest1, page, size, sortBy, sortDirection);

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(3);
    }

    @Test
    void searchPublicUsers_WhenSearchQueryProvided_ShouldSearchByQuery() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        List<User> mockUsers = List.of(mockUser1, mockUser2, mockUser3);
        List<UserResponse> expectedResponses = List.of(mockUserResponse1, mockUserResponse2, mockUserResponse3);

        when(mongoTemplate.count(any(Query.class), eq(User.class))).thenReturn((long) mockUsers.size());
        when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(mockUsers);
        when(userMapper.toUserResponseList(mockUsers)).thenReturn(expectedResponses);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        // Act
        PageResponse<UserResponse> result = userService.searchPublicUsers(searchPublicUsersRequest2, page, size, sortBy, sortDirection);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(3);
        assertThat(result.getData()).isEqualTo(expectedResponses);

        verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(User.class));

        Query capturedQuery = queryCaptor.getValue();
        String queryString = capturedQuery.getQueryObject().toJson();
        assertThat(queryString).contains("pub");
        assertThat(capturedQuery.getLimit()).isEqualTo(size);
    }

    @Test
    void searchPublicUsers_WhenHasFilter_ShouldApplyFilters() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        List<User> mockUsers = List.of(mockUser3);
        List<UserResponse> expectedResponses = List.of(mockUserResponse3);

        when(mongoTemplate.count(any(), eq(User.class))).thenReturn((long) mockUsers.size());
        when(mongoTemplate.find(any(), eq(User.class))).thenReturn(mockUsers);
        when(userMapper.toUserResponseList(mockUsers)).thenReturn(expectedResponses);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        // Act
        PageResponse<UserResponse> result = userService.searchPublicUsers(searchPublicUsersRequest3, page, size, sortBy, sortDirection);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0)).isEqualTo(mockUserResponse3);

        verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(User.class));

        Query capturedQuery = queryCaptor.getValue();

        Document queryCriteria = capturedQuery.getQueryObject();

        assertThat(queryCriteria.containsKey("dateOfBirth")).isTrue();

        Document dateOfBirthFilter = (Document) queryCriteria.get("dateOfBirth");
        assertThat(dateOfBirthFilter).containsKey("$gte");
        assertThat(dateOfBirthFilter).containsKey("$lte");

        assertThat(capturedQuery.getLimit()).isEqualTo(size);
    }

    @Test
    void searchPublicUsers_WhenNoUsers_ShouldReturnEmptyList() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        when(mongoTemplate.count(any(Query.class), eq(User.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(List.of());

        // Act
        PageResponse<UserResponse> result = userService.searchPublicUsers(searchPublicUsersRequest4, page, size, sortBy, sortDirection);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isEmpty();

        verify(mongoTemplate, times(1)).count(any(Query.class), eq(User.class));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(User.class));
    }

    /**
     * Test cases for getUserProfile
     * getUserProfile_ShouldReturnUserResponse: Tests that the method returns the correct UserResponse for a given username.
     * getUserProfile_WhenUserNotFound_ShouldThrowAppException: Tests that if the username does not exist, an AppException is thrown with the appropriate message and status code.
     */

    @Test
    void getUserProfile_ShouldReturnUserResponse() {
        String username = "test_user_reg";

        when(userRepo.findByUsernameAndIsDeletedFalse(username)).thenReturn(Optional.of(mockUser));
        when(userMapper.toUserResponse(mockUser)).thenReturn(mockUserResponse);

        UserResponse result = userService.getUserProfile(username);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("userId123");
        assertThat(result.getUsername()).isEqualTo("test_user_reg");

        verify(userRepo, times(1)).findByUsernameAndIsDeletedFalse(username);
        verify(userMapper, times(1)).toUserResponse(mockUser);
    }

    @Test
    void getUserProfile_WhenUserNotFound_ShouldThrowAppException() {
        String username = "non_existent_user";

        when(userRepo.findByUsernameAndIsDeletedFalse(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(username))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("User not found")
                .extracting(ex -> ((AppException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepo, times(1)).findByUsernameAndIsDeletedFalse(username);
        verify(userMapper, never()).toUserResponse(any(User.class));
    }

    /**
     * Test cases for getMyProfile
     * getMyProfile_ShouldReturnUserResponse: Tests that the method returns the correct UserResponse for a given user ID.
     * getMyProfile_WhenUserNotFound_ShouldThrowAppException: Tests that if the user ID does not exist, an AppException is thrown with the appropriate message and status code.
     */

    @Test
    void getMyProfile_ShouldReturnUserResponse() {
        String userId = "userId123";

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(mockUser));
        when(userMapper.toUserResponse(mockUser)).thenReturn(mockUserResponse);

        UserResponse result = userService.getMyProfile(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("userId123");
        assertThat(result.getUsername()).isEqualTo("test_user_reg");

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
        verify(userMapper, times(1)).toUserResponse(mockUser);
    }

    @Test
    void getMyProfile_WhenUserNotFound_ShouldThrowAppException() {
        String userId = "non_existent_user";

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(userId))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("User not found")
                .extracting(ex -> ((AppException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
        verify(userMapper, never()).toUserResponse(any(User.class));
    }

    /**
     * Test cases for updateMyProfile
     * updateMyProfile_ShouldUpdateAndReturnUserResponse: Tests that the method correctly updates the user's profile and returns the updated UserResponse.
     * updateMyProfile_WhenUserNotFound_ShouldThrowAppException: Tests that if the user ID does not exist, an AppException is thrown with the appropriate message and status code.
     */
    
    @Test
    void updateMyProfile_ShouldUpdateAndReturnUserResponse() {
        String userId = "userId123";

        UserResponse expectedResponse = UserResponse.builder()
            .id(userId)
            .username("Updated Name")
            .email("updated-user@test.com")
            .dateOfBirth(LocalDate.of(1998, 1, 4))
            .build();

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(mockUser));
        when(userRepo.save(any(User.class))).thenReturn(mockUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(expectedResponse);

        UserResponse result = userService.updateMyProfile(userId, updateMyProfileRequest);

        assertThat(result).isNotNull();

        assertThat(result.getId()).isEqualTo(expectedResponse.getId());
        assertThat(result.getUsername()).isEqualTo(expectedResponse.getUsername());
        assertThat(result.getEmail()).isEqualTo(expectedResponse.getEmail());
        assertThat(result.getDateOfBirth()).isEqualTo(expectedResponse.getDateOfBirth());

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
        verify(userRepo, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toUserResponse(any(User.class));
    }

    @Test
    void updateMyProfile_WhenUserNotFound_ShouldThrowAppException() {
        String userId = "non_existent_user";

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMyProfile(userId, updateMyProfileRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("User not found")
                .extracting(ex -> ((AppException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
        verify(userRepo, never()).save(any(User.class));
        verify(userMapper, never()).toUserResponse(any(User.class));
    }

    /**
     * Test cases for updatePassword
     * Tests:
     * - updatePassword_Success_ShouldSaveNewPassword
     * - updatePassword_Incorrect_CurrentPassword_ShouldThrowBadRequest
     */

    @Test
    void updatePassword_Success_ShouldSaveNewPassword() {
        String userId = "userId123";
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass123");

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(mockUser));
        when(passwordHelper.matchPassword(request.getCurrentPassword(), mockUser.getPasswordHash())).thenReturn(true);
        when(passwordHelper.matchPassword(request.getNewPassword(), mockUser.getPasswordHash())).thenReturn(false);
        when(passwordHelper.hashPassword(request.getNewPassword())).thenReturn("new_hashed_password");
        when(userRepo.save(mockUser)).thenReturn(mockUser);

        userService.updatePassword(userId, request);

        assertThat(mockUser.getPasswordHash()).isEqualTo("new_hashed_password");
        verify(userRepo, times(1)).save(mockUser);
    }

    @Test
    void updatePassword_IncorrectCurrentPassword_ShouldThrowBadRequest() {
        String userId = "userId123";
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass", "NewPass123");

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(mockUser));
        when(passwordHelper.matchPassword(request.getCurrentPassword(), mockUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.updatePassword(userId, request))
            .isInstanceOf(AppException.class)
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepo, never()).save(any());
    }

    /**
     * Testcases for updateMyAvatar
     * updateAvatar_Success_ShouldDeleteOldAvatarAndUpdateNewOne: Tests that when updating the avatar, the old avatar is deleted and the new avatar is set correctly.
     * updateAvatar_WhenUserNotFound_ShouldThrowAppException: Tests that if the user ID does not exist, an AppException is thrown with the appropriate message and status code.
     */

    @Test
    void updateAvatar_Success_ShouldDeleteOldAvatarAndUpdateNewOne() {
        String userId = "userId123";
        
        MediaFileUploadRequest mockUploadRequest = MediaFileUploadRequest.builder()
            .mediaCode("NEW_CODE")
            .objectKey("avatars/object-key")
            .build();

        MediaFile oldAvatar = MediaFile.builder()
            .mediaCode("OLD_CODE")
            .objectKey("avatars/old-object-key")
            .originalUrl("http://cdn.com/old.jpg")
            .build();

        mockUser.setAvatar(oldAvatar);

        String expectedNewAvatarUrl = "http://cdn.com/avatars/object-key";
       
        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(mockUser));
        when(internalMediaService.resolve(mockUploadRequest.getMediaCode())).thenReturn(expectedNewAvatarUrl);
        when(userRepo.save(any(User.class))).thenReturn(mockUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        userService.updateAvatar(userId, mockUploadRequest);

        // Assert
        verify(internalMediaService, times(1)).delete(oldAvatar.getObjectKey());
        verify(internalMediaService, times(1)).resolve(mockUploadRequest.getMediaCode());

        verify(userRepo, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getAvatar()).isNotNull();
        assertThat(savedUser.getAvatar().getMediaCode()).isEqualTo(mockUploadRequest.getMediaCode());
        assertThat(savedUser.getAvatar().getOriginalUrl()).isEqualTo(expectedNewAvatarUrl);
        assertThat(savedUser.getAvatar().getObjectKey()).isEqualTo(mockUploadRequest.getObjectKey());
    }

    @Test
    void updateAvatar_WhenUserNotFound_ShouldThrowAppException() {
        String userId = "non_existent_user";
        MediaFileUploadRequest mockUploadRequest = MediaFileUploadRequest.builder()
            .mediaCode("NEW_CODE")
            .objectKey("avatars/object-key")
            .build();

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateAvatar(userId, mockUploadRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("User not found")
                .extracting(ex -> ((AppException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
        verify(internalMediaService, never()).delete(anyString());
        verify(internalMediaService, never()).resolve(anyString());
        verify(userRepo, never()).save(any(User.class));
    }

    /**
     * Testcases for getManagedUsers similar to getPublicUsers 
     * but with different repository method and potentially different filters.
     * And combined test in searchManagedUsers
     */

    /**
     * Test cases for searchManagedUsers
     * searchManagedUsers_WhenHasFilterStatusAndRoles_ShouldReturnListOfUserResponses: Tests that if pagination and sorting parameters 
     * are provided, they are correctly passed to the repository method for managed users.
     */

    @Test
    void searchManagedUsers_WhenHasFilterStatusAndRoles_ShouldReturnListOfUserResponses() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        User mockManager1 = User.builder()
            .id("managerId1")
            .username("manager_user")
            .role(UserRole.MANAGER)
            .status(UserStatus.VERIFIED)
            .build();

        User mockManager2 = User.builder()
            .id("managerId2")
            .username("manager_user2")
            .role(UserRole.MANAGER)
            .status(UserStatus.VERIFIED)
            .build();

        User mockNewUser1 = User.builder()
            .id("userId1")
            .username("normal_user")
            .role(UserRole.USER)
            .status(UserStatus.PENDING)
            .build();

        UserResponse mockManagerResponse1 = UserResponse.builder()
            .id("managerId1")
            .username("manager_user")
            .role(UserRole.MANAGER.name())
            .status(UserStatus.VERIFIED.name())
            .build();

        UserResponse mockNewUserResponse1 = UserResponse.builder()
            .id("userId1")
            .username("normal_user")
            .role(UserRole.USER.name())
            .status(UserStatus.PENDING.name())
            .build();

        UserResponse mockManagerResponse2 = UserResponse.builder()
            .id("managerId2")
            .username("manager_user2")
            .role(UserRole.MANAGER.name())
            .status(UserStatus.VERIFIED.name())
            .build();


        List<User> mockUsers = List.of(mockManager1, mockManager2, mockNewUser1);
        List<UserResponse> expectedResponses = List.of(
            mockManagerResponse1,
            mockManagerResponse2,
            mockNewUserResponse1
        );

        when(mongoTemplate.count(any(Query.class), eq(User.class))).thenReturn((long) mockUsers.size());
        when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(mockUsers);
        when(userMapper.toUserResponseList(mockUsers)).thenReturn(expectedResponses);
        
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        PageResponse<UserResponse> result = userService.searchManagedUsers(searchManagedUsersRequest, page, size, sortBy, sortDirection);

        assertThat(result).isNotNull();

        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(size);
        assertThat(result.getTotalElements()).isEqualTo(mockUsers.size());
        assertThat(result.getTotalPages()).isEqualTo(1);

        List<UserResponse> actualData = result.getData();
        assertThat(actualData).isNotNull();

        assertThat(actualData).isEqualTo(expectedResponses);
        assertThat(actualData).hasSize(expectedResponses.size());
        assertThat(actualData).extracting(user -> user.getId())
            .containsExactly("managerId1", "managerId2", "userId1");

        verify(mongoTemplate, times(1)).count(any(Query.class), eq(User.class));
        verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(User.class));

        Query capturedQuery = queryCaptor.getValue();
        Document queryCriteria = capturedQuery.getQueryObject();

        assertThat(queryCriteria.containsKey("role")).isTrue();
        assertThat(queryCriteria.containsKey("status")).isTrue();
    }

    /**
     * Test cases for updateUser
     * updateUser_ShouldUpdateUserAndReturnResponse: Tests that the method correctly updates a user's information and returns the updated UserResponse.
     * updateUser_WhenHasUserRole_ShouldUpdateUserAndReturnResponse: Tests that if the user has a specific role, the update is performed correctly and the appropriate UserResponse is returned.
     * updateUser_WhenHasStatus_ShouldUpdateUserAndReturnResponse: Tests that if the user has a specific status, the update is performed correctly and the appropriate UserResponse is returned.
     * updateUser_WhenStatusAndRoleInvalid_SaveDefaultRoleAndStatus: Tests that if the provided status or role is invalid, the default role and status are saved.
     * updateUser_WhenUserNotFound_ShouldThrowAppException: Tests that if the user ID does not exist, an AppException is thrown with the appropriate message and status code.
     */


    @Test
    public void updateUser_ShouldUpdateUserAndReturnResponse() {
        String userId = "userId123";

        User existingUser = User.builder()
            .id(userId)
            .username("old_username")
            .role(UserRole.USER)
            .status(UserStatus.PENDING)
            .build();

        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
            .role("MANAGER")
            .status("VeRiFied")
            .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(existingUser));
        when(userRepo.save(any(User.class))).thenReturn(existingUser);

        userService.updateUser(userId, updateUserRequest);

        verify(userRepo, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getRole()).isEqualTo(UserRole.MANAGER);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.VERIFIED);
        assertThat(savedUser.getUsername()).isEqualTo("old_username");

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
    }

    @Test
    public void updateUser_WhenHasUserRole_ShouldUpdateUserAndReturnResponse() {
        String userId = "userId123";

        User existingUser = User.builder()
            .id(userId)
            .username("old_username")
            .role(UserRole.USER)
            .status(UserStatus.PENDING)
            .build();

        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
            .role("MANAGER")
            .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(existingUser));
        when(userRepo.save(any(User.class))).thenReturn(existingUser);

        userService.updateUser(userId, updateUserRequest);

        verify(userRepo, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getRole()).isEqualTo(UserRole.MANAGER);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(savedUser.getUsername()).isEqualTo("old_username");

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
    }

    @Test
    void updateUser_WhenHasStatus_ShouldUpdateUserAndReturnResponse() {
        String userId = "userId123";

        User existingUser = User.builder()
            .id(userId)
            .username("old_username")
            .role(UserRole.USER)
            .status(UserStatus.PENDING)
            .build();

        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
            .status("VERIFIED")
            .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(existingUser));
        when(userRepo.save(any(User.class))).thenReturn(existingUser);

        userService.updateUser(userId, updateUserRequest);

        verify(userRepo, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.VERIFIED);
        assertThat(savedUser.getUsername()).isEqualTo("old_username");

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
    }

    @Test
    void updateUser_WhenStatusAndRoleInvalid_SaveDefaultRoleAndStatus() {
        String userId = "userId123";

        User existingUser = User.builder()
            .id(userId)
            .username("old_username")
            .role(UserRole.USER)
            .status(UserStatus.VERIFIED)
            .build();

        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
            .role("INVALID_ROLE")
            .status("INVALID_STATUS")
            .build();

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(existingUser));
        when(userRepo.save(any(User.class))).thenReturn(existingUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        userService.updateUser(userId, updateUserRequest);

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
        verify(userRepo, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
        verify(userRepo, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_WhenUserNotFound_ShouldThrowAppException() {
        String userId = "non_existent_user";

        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
            .role("MANAGER")
            .status("VERIFIED")
            .build();

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(userId, updateUserRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("User not found")
                .extracting(ex -> ((AppException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
        verify(userRepo, never()).save(any(User.class));
    }

    /**
     * Test cases for deleteUser
     * deleteUser_ShouldSoftDeleteUser: Tests that the method correctly soft deletes a user by
     * setting the isDeleted flag to true and updating the deletedAt timestamp.
     * deleteUser_WhenUserNotFound_ShouldThrowAppException: Tests that if the user ID does not exist, 
     * an AppException is thrown with the appropriate message and status code.
     */

    @Test
    void deleteUser_ShouldSoftDeleteUser() {
        String userId = "userId123";
        mockUser.setRole(UserRole.USER);

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(mockUser));
        when(userRepo.save(any(User.class))).thenReturn(mockUser);

        userService.deleteUser(userId);

        assertThat(mockUser.getIsDeleted()).isTrue();
        assertThat(mockUser.getDeletedAt()).isNotNull();
        verify(internalWalletService, times(1)).deleteUserWallet(userId);
    }

    @Test
    void deleteUser_WhenUserNotFound_ShouldThrowAppException() {
        String userId = "userId123";

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("User not found")
                .extracting(ex -> ((AppException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepo, times(1)).findByIdAndIsDeletedFalse(userId);
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void deleteUser_WhenUserIsAdmin_ShouldThrowBadRequest() {
        // Arrange
        String userId = "adminId123";
        User adminUser = User.builder().id(userId).role(UserRole.ADMIN).build();

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(adminUser));

        // Act & Assert 
        assertThatThrownBy(() -> userService.deleteUser(userId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Cannot delete admin user")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepo, never()).save(any());
    }

    /**
     * Test cases for updateUserReputation
     * Tests: 
     * - updateUserReputation_Success_ShouldClampAndSave
     */

    @Test
    void updateUserReputation_Success_ShouldClampAndSave() {
        String userId = "userId123";
        double change = 2.5;

        mockUser.setReputation(4.0);

        when(userRepo.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(mockUser));
        when(userRepo.save(mockUser)).thenReturn(mockUser);

        userService.updateUserReputation(userId, change);

        assertThat(mockUser.getReputation()).isEqualTo(6.5);
        verify(userRepo, times(1)).save(mockUser);
    }
}
