package com.github.dghng36.eauction.e_auction_system.unit.modules.identity.user.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.GlobalExceptionHandler;
import com.github.dghng36.eauction.infra.config.security.resolver.AuthInfoArgumentResolver;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.identity.enums.UserStatus;
import com.github.dghng36.eauction.modules.identity.user.controller.v1.UserController;
import com.github.dghng36.eauction.modules.identity.user.dto.request.UpdateMyProfileRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.response.UserResponse;
import com.github.dghng36.eauction.modules.identity.user.service.UserService;


@ExtendWith(MockitoExtension.class)
public class UserControllerTest {
    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .build();

    private UsernamePasswordAuthenticationToken mockAuthentication;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        this.mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
            .setCustomArgumentResolvers(new AuthInfoArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();

        Map<String, Object> principal = Map.of(
            "userId", "userIdTest",
            "username", "userControllerTest",
            "email", "userControllerTest@test.com",
            "role", "ROLE_USER"
        );

        var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
        mockAuthentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    /**
     * Test cases of UserController for getMyProfile
     * Tests:
     * - getMyProfile_WithValidAuthContext_ShouldBindIdAndReturnProfile
     * - getMyProfile_WhenNoAuthentication_ShouldReturn401Unauthorized
     * - getMyProfile_WhenPrincipalMapMissingField_ShouldThrowAppException
     */

    @Test
    void getMyProfile_WithValidAuthContext_ShouldBindIdAndReturnProfile() throws Exception {
        // Arrange
        UserResponse mockUserResp = UserResponse.builder()
            .id("userIdTest")
            .username("userControllerTest")
            .email("userControllerTest@test.com")
            .fullName("User Controller Test")
            .phoneNumber("0123456789")
            .avatarUrl("http://example.com/avatar.jpg")
            .nationalIdHash("hashedNationalId")
            .idIssueDate(LocalDate.of(2020, 1, 1))
            .idIssuePlace("City A")
            .nationality("USA")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .address("123 Main St")
            .role(UserRole.USER.name())
            .status(UserStatus.VERIFIED.name())
            .reputation(5.0)
            .userAuctionInfo(null)
            .build();

        when(userService.getMyProfile("userIdTest")).thenReturn(mockUserResp);

        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);

        // Act & Assert
        try {
            mockMvc.perform(get("/api/v1/users/me")
                .with(authentication(mockAuthentication))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusHttpCode").value(200))
                .andExpect(jsonPath("$.message").value("User profile retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value("userIdTest"))
                .andExpect(jsonPath("$.data.username").value("userControllerTest")
            );

            verify(userService, times(1)).getMyProfile("userIdTest");

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void getMyProfile_WhenNoAuthentication_ShouldReturn401Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/me")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void getMyProfile_WhenPrincipalMapMissingField_ShouldThrowAppException() throws Exception {
        // Arrange
        Map<String, Object> incompletePrincipal = Map.of(
            "username", "userControllerTest",
            "email", "userControllerTest@example.com",
            "role", "ROLE_USER"
        );

        var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");

        UsernamePasswordAuthenticationToken incompleteAuth = new UsernamePasswordAuthenticationToken(incompletePrincipal, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(incompleteAuth);

        // Act & Assert
        try {
            mockMvc.perform(get("/api/v1/users/me")
                .with(authentication(incompleteAuth))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusHttpCode").value(401))
                .andExpect(jsonPath("$.message").value("User unauthorized"))
                .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(userService);

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Test cases of UserController for updateMyProfile
     * Tests:
     * - updateMyProfile_WithValidAuthContext_ShouldReturn200Ok
     * - updateMyProfile_WhenInvalidInput_ShouldReturn400BadRequest
     */

    @Test
    void updateMyProfile_WithValidAuthContext_ShouldReturn200Ok() throws Exception {
        // Arrange
       UpdateMyProfileRequest updateMyProfileRequest = UpdateMyProfileRequest.builder()
            .fullName("Updated User Controller Test")
            .phoneNumber("0987654321")
            .address("456 Another St")
            .dateOfBirth(LocalDate.of(1992, 3, 16))
            .build();

        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);
        
        // Act & Assert
        try {
            mockMvc.perform(patch("/api/v1/users/me")
                .with(authentication(mockAuthentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateMyProfileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusHttpCode").value(200))
                .andExpect(jsonPath("$.message").value("User profile updated successfully"))
                .andExpect(jsonPath("$.data").doesNotExist()
            );

            verify(userService, times(1)).updateMyProfile(eq("userIdTest"), any(UpdateMyProfileRequest.class));

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    @Test
    void updateMyProfile_WhenInvalidInput_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        UpdateMyProfileRequest updateMyProfileRequest = UpdateMyProfileRequest.builder()
            .username("up")
            .fullName("Updated")
            .phoneNumber("0987654321")
            .address(null)
            .dateOfBirth(LocalDate.of(1992, 3, 16))
            .build();

        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);

        // Act & Assert
        try {
            mockMvc.perform(patch("/api/v1/users/me")
                .with(authentication(mockAuthentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateMyProfileRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusHttpCode").value(400))
                .andExpect(jsonPath("$.message").value("Invalid input arguments"))
                .andExpect(jsonPath("$.data.username").value("Username must be between 6 and 20 characters")
            );

            verifyNoInteractions(userService);

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Test cases of UserController for getPublicUsers
     * Tests:
     * - getPublicUsers_WithValidRequest_ShouldReturn200Ok
     * - getPublicUsers_WhenInvalidPaginationParams_ShouldReturn400BadRequest
     * - getPublicUsers_WhenNoSortParams_ShouldReturn200OkWithDefaultSorting
     */

    @Test
    void getPublicUsers_WithValidRequest_ShouldReturn200Ok() throws Exception {
        // Arrange
        PageResponse<UserResponse> mockPageResponse = PageResponse.<UserResponse> builder()
            .currentPage(2)
            .totalPages(5)
            .totalElements(50)
            .pageSize(10)
            .data(
                List.of(
                    UserResponse.builder()
                        .id("userId1")
                        .username("user1")
                        .build(),

                    UserResponse.builder()
                        .id("userId2")
                        .username("user2")
                        .build(),

                    UserResponse.builder()
                        .id("userId3")
                        .username("user3")
                        .build()
                )
            )
            .build();

        when(userService.getPublicUsers(2, 10, "createdAt", "desc")).thenReturn(mockPageResponse);


        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);
        // Act & Assert
        
        try {
            mockMvc.perform(get("/api/v1/users")
                .with(authentication(mockAuthentication))
                .param("page", "2")
                .param("size", "10")
                .param("sortBy", "createdAt")
                .param("sortDirection", "desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusHttpCode").value(200))
                .andExpect(jsonPath("$.message").value("All public users retrieved successfully"))
                .andExpect(jsonPath("$.data.currentPage").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(50))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.data[0].id").value("userId1"))
                .andExpect(jsonPath("$.data.data[0].username").value("user1"))
                .andExpect(jsonPath("$.data.data[1].id").value("userId2"))
                .andExpect(jsonPath("$.data.data[1].username").value("user2"))
                .andExpect(jsonPath("$.data.data[2].id").value("userId3"))
                .andExpect(jsonPath("$.data.data[2].username").value("user3")
            );

            verify(userService, times(1)).getPublicUsers(2, 10, "createdAt", "desc");

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void getPublicUsers_WhenInvalidPaginationParams_ShouldReturn400BadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
            .with(authentication(mockAuthentication))
            .param("page", "-1")
            .param("size", "0")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusHttpCode").value(400))
            .andExpect(jsonPath("$.message").value("Invalid input arguments"))
            .andExpect(jsonPath("$.data.page").value("Page index must not be less than 0"))
            .andExpect(jsonPath("$.data.size").value("Page size must not be less than 1"));

        verifyNoInteractions(userService);
    }

    @Test
    void getPublicUsers_WhenNoSortParams_ShouldReturn200OkWithDefaultSorting() throws Exception {
        // Arrange
        PageResponse<UserResponse> mockPageResponse = PageResponse.<UserResponse> builder()
            .currentPage(0)
            .totalPages(1)
            .totalElements(2)
            .pageSize(10)
            .data(
                List.of(
                    UserResponse.builder()
                        .id("userId1")
                        .username("user1")
                        .build(),

                    UserResponse.builder()
                        .id("userId2")
                        .username("user2")
                        .build()
                )
            )
            .build();

        when(userService.getPublicUsers(0, 10, "createdAt", "desc")).thenReturn(mockPageResponse);

        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);
        // Act & Assert
        try {
            mockMvc.perform(get("/api/v1/users")
                .with(authentication(mockAuthentication))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusHttpCode").value(200))
                .andExpect(jsonPath("$.message").value("All public users retrieved successfully"))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.data[0].id").value("userId1"))
                .andExpect(jsonPath("$.data.data[0].username").value("user1"))
                .andExpect(jsonPath("$.data.data[1].id").value("userId2"))
                .andExpect(jsonPath("$.data.data[1].username").value("user2")
            );

            verify(userService, times(1)).getPublicUsers(0, 10, "createdAt", "desc");

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
