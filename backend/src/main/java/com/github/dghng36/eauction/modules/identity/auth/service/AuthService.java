package com.github.dghng36.eauction.modules.identity.auth.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.modules.identity.auth.dto.internal.AuthResult;
import com.github.dghng36.eauction.modules.identity.auth.dto.internal.JwtGeneratedResult;
import com.github.dghng36.eauction.modules.identity.auth.dto.request.LoginRequest;
import com.github.dghng36.eauction.modules.identity.auth.model.RefreshToken;
import com.github.dghng36.eauction.modules.identity.auth.service.jwt.JwtAdapter;
import com.github.dghng36.eauction.modules.identity.enums.UserStatus;
import com.github.dghng36.eauction.modules.identity.helper.PasswordHelper;
import com.github.dghng36.eauction.modules.identity.reputation.service.ReputationProcessor;
import com.github.dghng36.eauction.modules.identity.user.model.User;
import com.github.dghng36.eauction.modules.identity.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService {
    MongoTemplate mongoTemplate;
    UserRepository userRepo;

    RefreshTokenProcessor refreshTokenProcessor;
    InvalidatedTokenProcessor invalidatedTokenProcessor;

    ReputationProcessor reputationProcessor;

    PasswordHelper passwordHelper;

    JwtAdapter jwtAdapter;

    public AuthResult loginUser(LoginRequest loginRequest) {
        // Check if user exists
        User user = userRepo.findByIdentifier(loginRequest.getIdentifier())
            .orElseThrow(() -> new AppException("Invalid username or password", HttpStatus.UNAUTHORIZED));

        // Check if user is banned or blocked
        if (UserStatus.BANNED.equals(user.getStatus()) || UserStatus.BLOCKED.equals(user.getStatus())) {
            log.warn("User [{}] is banned or blocked", user.getId());

            throw new AppException("User is banned or blocked", HttpStatus.FORBIDDEN);
        }

        // Check if password is correct
        if (!passwordHelper.matchPassword(loginRequest.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid username or password for user: [{}]", user.getId());

            throw new AppException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        if (isNewWeek(user.getLastLoginAt(), Instant.now())) {
            // Increment reputation for weekly login
            reputationProcessor.awardWeeklyLoginBonus(user.getId());

            log.info("Awarded weekly login bonus for user [{}]", user.getId());
        }

        // Set last login time
        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update()
            .set("lastLoginAt", Instant.now());

        mongoTemplate.updateFirst(query, update, User.class);

        // Generate new access token and refresh token
        JwtGeneratedResult jwtTokenResult = jwtAdapter.generateToken(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().name()
        );

        // Save refresh token to database
        refreshTokenProcessor.createRefreshToken(
            user.getId(),
            jwtTokenResult.getRefreshToken(),
            jwtTokenResult.getRefreshTokenExpiryDate()
        );

        log.info("User [{}] logged in successfully", user.getId());

        return AuthResult.builder()
            .accessToken(jwtTokenResult.getAccessToken())
            .refreshToken(jwtTokenResult.getRefreshToken())
            .isAuthenticated(true)
            .build();
    }

    @Transactional
    public void logoutUser(String refreshToken) {
        // Check if refresh token is valid
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new AppException("Refresh token is required", HttpStatus.BAD_REQUEST);
        }

        // Check if refresh token is already invalidated
        if (invalidatedTokenProcessor.isTokenInvalidated(refreshToken)) {
            throw new AppException("Refresh token is already invalidated", HttpStatus.UNAUTHORIZED);
        }

        // Check refresh token exists in database and delete
        RefreshToken existingToken = refreshTokenProcessor.invalidateRefreshToken(refreshToken);

        // Invalidate refresh token
        invalidatedTokenProcessor.invalidateToken(refreshToken, existingToken.getExpiryTime());
        
        log.info("User [{}] logged out successfully", existingToken.getUserId());
    }

    @Transactional
    public AuthResult refreshToken(String oldRefreshToken, String oldAccessToken) {
        // Check if refresh token is valid
        if (oldRefreshToken == null || oldAccessToken == null 
            || invalidatedTokenProcessor.isTokenInvalidated(oldRefreshToken) 
            || invalidatedTokenProcessor.isTokenInvalidated(jwtAdapter.extractData(oldAccessToken, "jti"))
        ) {
            throw new AppException("Invalid token", HttpStatus.UNAUTHORIZED);
        }

        // Validate old access token signature and expiration
        if (!jwtAdapter.validateAccessToken(oldAccessToken)) {
            throw new AppException("Invalid access token", HttpStatus.UNAUTHORIZED);
        }
        
        // Check if refresh token exists in database and is expired
        refreshTokenProcessor.isRefreshTokenExpired(oldRefreshToken);

        // Generate new access token and refresh token
        String userId = jwtAdapter.extractData(oldAccessToken, "userId");
        User user = userRepo.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        JwtGeneratedResult jwtTokenResult = jwtAdapter.generateToken(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().name()
        );

        // Invalidate old refresh token and access token
        refreshTokenProcessor.invalidateRefreshToken(oldRefreshToken);

        invalidatedTokenProcessor.invalidateToken(
            oldRefreshToken, 
            Instant.now().plusSeconds(Long.parseLong(ConstantsUtils.AuthenticationConstants.INVALIDATED_EXPIRY_DURATION))
        );
        invalidatedTokenProcessor.invalidateToken(
            jwtAdapter.extractData(oldAccessToken, "jti"), 
            Instant.ofEpochSecond(Long.parseLong(ConstantsUtils.AuthenticationConstants.INVALIDATED_EXPIRY_DURATION)).plusSeconds(Instant.now().getEpochSecond())
        );

        // Save new refresh token to database
        refreshTokenProcessor.createRefreshToken(
            user.getId(),
            jwtTokenResult.getRefreshToken(),
            jwtTokenResult.getRefreshTokenExpiryDate()
        );

        return AuthResult.builder()
            .accessToken(jwtTokenResult.getAccessToken())
            .refreshToken(jwtTokenResult.getRefreshToken())
            .isAuthenticated(true)
            .build();
    }

    // Utility method
    private boolean isNewWeek(Instant lastAwarded, Instant now) {
        if (lastAwarded == null || now == null) {
            return false;
        }

        ZonedDateTime lastAwardedDate = lastAwarded.atZone(ZoneId.of("UTC"));
        ZonedDateTime nowDate = now.atZone(ZoneId.of("UTC"));

        TemporalField weekBasedYearField = WeekFields.of(Locale.getDefault()).weekBasedYear();
        TemporalField weekOfWeekBasedYearField = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
        
        int lastWeekYear = lastAwardedDate.get(weekBasedYearField);
        int currentWeekYear = nowDate.get(weekBasedYearField);

        int lastWeek = lastAwardedDate.get(weekOfWeekBasedYearField);
        int currentWeek = nowDate.get(weekOfWeekBasedYearField);

        if (currentWeekYear > lastWeekYear) {
            return true;
        } else if (currentWeekYear == lastWeekYear) {
            return currentWeek > lastWeek;
        }

        return false;
    }
}
