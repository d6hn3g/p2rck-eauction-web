package com.github.dghng36.eauction.e_auction_system.integration.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.github.dghng36.eauction.boot.EAuctionSystemApplication;
import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.CreditUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.github.dghng36.eauction.modules.identity.auth.dto.request.LoginRequest;
import com.github.dghng36.eauction.modules.identity.auth.dto.response.AuthResponse;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.identity.enums.UserStatus;
import com.github.dghng36.eauction.modules.identity.helper.PIICryptoHelper;
import com.github.dghng36.eauction.modules.identity.helper.PasswordHelper;
import com.github.dghng36.eauction.modules.identity.user.dto.request.RegisterRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.response.UserResponse;
import com.github.dghng36.eauction.modules.identity.user.model.User;
import com.github.dghng36.eauction.modules.identity.user.repository.UserRepository;
import com.github.dghng36.eauction.modules.media.model.MediaUrl;
import com.github.dghng36.eauction.modules.media.repository.MediaUrlRepository;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = EAuctionSystemApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT
)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Autowired protected TestRestTemplate restTemplate;
    @Autowired protected MongoTemplate mongoTemplate;
    @Autowired protected UserRepository userRepository;
    @Autowired protected MediaUrlRepository mediaUrlRepository;
    @Autowired protected PasswordHelper passwordHelper;
    @Autowired protected PIICryptoHelper piiCryptoHelper;
    @Autowired protected InternalWalletService internalWalletService;

    protected TestUserSession registerAndLogin(String prefix) {
        RegisterRequest registerRequest = TestDataFactory.buildRegisterRequest(prefix);

        ResponseEntity<ApiResponse<UserResponse>> registerResponse = restTemplate.exchange(
            "/api/v1/users/register",
            HttpMethod.POST,
            new HttpEntity<>(registerRequest),
            new ParameterizedTypeReference<>() {}
        );

        UserResponse user = registerResponse.getBody().getData();

        LoginRequest loginRequest = LoginRequest.builder()
            .identifier(registerRequest.getUsername())
            .password(registerRequest.getPassword())
            .build();

        ResponseEntity<ApiResponse<AuthResponse>> loginResponse = restTemplate.exchange(
            "/api/v1/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(loginRequest),
            new ParameterizedTypeReference<>() {}
        );

        return TestUserSession.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .accessToken(loginResponse.getBody().getData().getAccessToken())
            .build();
    }

    protected TestUserSession seedPrivilegedUser(String username, UserRole role) {
        String encryptedNationalId = piiCryptoHelper.encodeNationalId("SEED" + username);

        User user = User.builder()
            .username(username)
            .email(username + "@seed.test")
            .passwordHash(passwordHelper.hashPassword("Password123!"))
            .phoneNumber("09" + String.format("%08d", username.hashCode() & 0xFFFF))
            .fullName("Seed User " + username)
            .nationalId(encryptedNationalId)
            .idIssueDate(java.time.LocalDate.of(2020, 1, 1))
            .idIssuePlace("Ha Noi")
            .nationality("Vietnamese")
            .address("Seed Address")
            .dateOfBirth(java.time.LocalDate.of(1990, 1, 1))
            .status(UserStatus.VERIFIED)
            .role(role)
            .isDeleted(false)
            .deletedAt(null)
            .build();
        user = userRepository.save(user);
        internalWalletService.createUserWallet(user.getId());

        LoginRequest loginRequest = LoginRequest.builder()
            .identifier(username)
            .password("Password123!")
            .build();

        ResponseEntity<ApiResponse<AuthResponse>> loginResponse = restTemplate.exchange(
            "/api/v1/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(loginRequest),
            new ParameterizedTypeReference<>() {}
        );

        return TestUserSession.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .accessToken(loginResponse.getBody().getData().getAccessToken())
            .build();
    }

    protected void creditWallet(TestUserSession admin, String userId, double amount) {
        CreditUserWalletRequest request = CreditUserWalletRequest.builder()
            .amount(BigDecimal.valueOf(amount))
            .build();

        restTemplate.exchange(
            "/api/v1/admin/management/users/" + userId + "/wallets/credit",
            HttpMethod.PATCH,
            new HttpEntity<>(request, authHeaders(admin.getAccessToken())),
            new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
    }

    protected String seedMediaCode() {
        String mediaCode = TestDataFactory.uniqueMediaCode();
        mediaUrlRepository.save(MediaUrl.builder()
            .mediaCode(mediaCode)
            .originalUrl("https://test-storage.example.com/" + mediaCode + ".jpg")
            .objectKey(mediaCode + ".jpg")
            .active(true)
            .build());
        return mediaCode;
    }

    protected HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    protected <T> ApiResponse<T> postAuthenticated(
        String path,
        Object body,
        String accessToken,
        ParameterizedTypeReference<ApiResponse<T>> responseType
    ) {
        ResponseEntity<ApiResponse<T>> response = restTemplate.exchange(
            path,
            HttpMethod.POST,
            new HttpEntity<>(body, authHeaders(accessToken)),
            responseType
        );
        return response.getBody();
    }

    protected <T> ApiResponse<T> patchAuthenticated(
        String path,
        Object body,
        String accessToken,
        ParameterizedTypeReference<ApiResponse<T>> responseType
    ) {
        ResponseEntity<ApiResponse<T>> response = restTemplate.exchange(
            path,
            HttpMethod.PATCH,
            new HttpEntity<>(body, authHeaders(accessToken)),
            responseType
        );
        return response.getBody();
    }

    protected void prepareAuctionRoomForBidding(String roomId) {
        Instant now = Instant.now();
        mongoTemplate.updateFirst(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(roomId)
            ),
            new org.springframework.data.mongodb.core.query.Update()
                .set("status", "ONGOING")
                .set("startTime", now.minusSeconds(60))
                .set("endTime", now.plusSeconds(3600)),
            "auction_rooms"
        );
    }

    protected List<TestUserSession> registerUsers(String prefix, int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> registerAndLogin(prefix))
            .toList();
    }
}
