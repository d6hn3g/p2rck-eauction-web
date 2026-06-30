package com.github.dghng36.eauction.e_auction_system.integration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.e_auction_system.integration.support.AbstractIntegrationTest;
import com.github.dghng36.eauction.e_auction_system.integration.support.TestUserSession;
import com.github.dghng36.eauction.modules.identity.auth.dto.request.LoginRequest;
import com.github.dghng36.eauction.modules.identity.auth.dto.response.AuthResponse;
import com.github.dghng36.eauction.modules.identity.user.dto.response.UserResponse;

class IdentityAuthIntegrationTest extends AbstractIntegrationTest {

    private TestUserSession registeredUser;

    @BeforeEach
    void setUpUser() {
        mongoTemplate.getDb().drop();
        registeredUser = registerAndLogin("auth_user");
    }

    @Test
    void registerAndLoginFlow_ShouldReturnAccessTokenAndProfile() {
        ResponseEntity<ApiResponse<UserResponse>> profileResponse = restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(registeredUser.getAccessToken())),
            new ParameterizedTypeReference<>() {}
        );

        assertThat(profileResponse.getBody()).isNotNull();
        assertThat(profileResponse.getBody().getData().getId()).isEqualTo(registeredUser.getUserId());
        assertThat(profileResponse.getBody().getData().getUsername()).isEqualTo(registeredUser.getUsername());

        LoginRequest loginRequest = LoginRequest.builder()
            .identifier(registeredUser.getUsername())
            .password("Password123!")
            .build();

        ResponseEntity<ApiResponse<AuthResponse>> loginResponse = restTemplate.exchange(
            "/api/v1/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(loginRequest),
            new ParameterizedTypeReference<>() {}
        );

        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getData().getAccessToken()).isNotBlank();
        assertThat(loginResponse.getBody().getData().getIsAuthenticated()).isTrue();
    }
}
