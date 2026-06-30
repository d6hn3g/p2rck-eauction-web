package com.github.dghng36.eauction.modules.identity.user.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;
import com.github.dghng36.eauction.modules.identity.enums.UserStatus;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserAuctionInfo;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class User extends BaseEntity{
    @Indexed(unique = true)
    String username;

    @JsonIgnore
    String passwordHash;

    @Indexed(unique = true)
    String email;

    @Indexed(unique = true)
    String fullName;

    @Indexed(unique = true)
    String phoneNumber;

    MediaFile avatar;

    @Indexed(unique = true)
    String nationalId;

    LocalDate idIssueDate;
    String idIssuePlace;

    String nationality;
    String address;
    LocalDate dateOfBirth;
    
    UserRole role;
    UserStatus status;

    Double reputation;

    UserAuctionInfo userAuctionInfo;

    Instant lastLoginAt;
}
