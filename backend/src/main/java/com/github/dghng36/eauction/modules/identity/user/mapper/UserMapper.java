package com.github.dghng36.eauction.modules.identity.user.mapper;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.identity.enums.UserStatus;
import com.github.dghng36.eauction.modules.identity.user.dto.request.RegisterRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.UpdateMyProfileRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.response.UserResponse;
import com.github.dghng36.eauction.modules.identity.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UserMapper {

    UserAuctionInfoMapper userAuctionInfoMapper;

    public User toUserEntity(
        RegisterRequest registerRequest, 
        String hashedPassword,
        String encodedNationalId
    ) {
        return User.builder()
            .username(registerRequest.getUsername())
            .passwordHash(hashedPassword)
            .email(registerRequest.getEmail())
            .fullName(registerRequest.getFullName())
            .phoneNumber(registerRequest.getPhoneNumber())
            .nationalId(encodedNationalId)
            .idIssueDate(registerRequest.getIdIssueDate())
            .idIssuePlace(registerRequest.getIdIssuePlace())
            .nationality(registerRequest.getNationality())
            .address(registerRequest.getAddress())
            .dateOfBirth(registerRequest.getDateOfBirth())
            .role(UserRole.USER)
            .status(UserStatus.PENDING)
            .reputation(0.0)
            .lastLoginAt(null)
            .isDeleted(false)
            .deletedAt(null)
            .userAuctionInfo(userAuctionInfoMapper.defaultUserAuctionInfo())
            .build();
    }

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phoneNumber(user.getPhoneNumber())
            .avatarUrl(user.getAvatar() != null ? user.getAvatar().getOriginalUrl() : null)
            .nationalIdHash(user.getNationalId())
            .idIssueDate(user.getIdIssueDate())
            .idIssuePlace(user.getIdIssuePlace())
            .nationality(user.getNationality())
            .address(user.getAddress())
            .dateOfBirth(user.getDateOfBirth())
            .role(user.getRole().name())
            .status(user.getStatus().name())
            .reputation(user.getReputation())
            .userAuctionInfo(user.getUserAuctionInfo())
            .build();
    }

    public List<UserResponse> toUserResponseList(List<User> users) {
        if (users == null) {
            return List.of();
        }

        return users.stream()
            .map(this::toUserResponse)
            .toList();
    }

    public void updateUserEntity(User user, UpdateMyProfileRequest updateMyProfileRequest) {
        if (updateMyProfileRequest == null) {
            return;
        }

        if (StringUtils.hasText(updateMyProfileRequest.getUsername()) 
            && !updateMyProfileRequest.getUsername().equals(user.getUsername())
        ) {
            user.setUsername(updateMyProfileRequest.getUsername());
        }

        if (StringUtils.hasText(updateMyProfileRequest.getEmail()) 
            && !updateMyProfileRequest.getEmail().equals(user.getEmail())
        ) {
            user.setEmail(updateMyProfileRequest.getEmail());
        }

        if (StringUtils.hasText(updateMyProfileRequest.getPhoneNumber()) 
            && !updateMyProfileRequest.getPhoneNumber().equals(user.getPhoneNumber())
        ) {
            user.setPhoneNumber(updateMyProfileRequest.getPhoneNumber());
        }

        if (StringUtils.hasText(updateMyProfileRequest.getFullName()) 
            && !updateMyProfileRequest.getFullName().equals(user.getFullName())
        ) {
            user.setFullName(updateMyProfileRequest.getFullName());
        }

        if (StringUtils.hasText(updateMyProfileRequest.getAddress()) 
            && !updateMyProfileRequest.getAddress().equals(user.getAddress())
        ) {
            user.setAddress(updateMyProfileRequest.getAddress());
        }

        if (updateMyProfileRequest.getIdIssueDate() != null
            && !updateMyProfileRequest.getIdIssueDate().equals(user.getIdIssueDate())
        ) {
            user.setIdIssueDate(updateMyProfileRequest.getIdIssueDate());
        }

        if (StringUtils.hasText(updateMyProfileRequest.getIdIssuePlace()) 
            && !updateMyProfileRequest.getIdIssuePlace().equals(user.getIdIssuePlace())
        ) {
            user.setIdIssuePlace(updateMyProfileRequest.getIdIssuePlace());
        }

        if (StringUtils.hasText(updateMyProfileRequest.getNationality()) 
            && !updateMyProfileRequest.getNationality().equals(user.getNationality())
        ) {
            user.setNationality(updateMyProfileRequest.getNationality());
        }

        if (updateMyProfileRequest.getDateOfBirth() != null
            && !updateMyProfileRequest.getDateOfBirth().equals(user.getDateOfBirth())
        ) {
            user.setDateOfBirth(updateMyProfileRequest.getDateOfBirth());
        }
    }
}
