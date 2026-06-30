package com.github.dghng36.eauction.modules.identity.user.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserAuctionInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    
    String username;

    String email;
    String fullName;
    String phoneNumber;

    String avatarUrl;

    String nationalIdHash;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate idIssueDate;
    String idIssuePlace;

    String nationality;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dateOfBirth;
    String address;

    String role;
    String status;

    Double reputation;

    UserAuctionInfo userAuctionInfo;
}
