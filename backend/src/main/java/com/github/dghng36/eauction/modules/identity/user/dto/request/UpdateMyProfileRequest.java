package com.github.dghng36.eauction.modules.identity.user.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateMyProfileRequest {
    @Size(
        min = ConstantsUtils.IdentityConstants.MIN_USERNAME_LENGTH, 
        max = ConstantsUtils.IdentityConstants.MAX_USERNAME_LENGTH, 
        message = "Username must be between " + ConstantsUtils.IdentityConstants.MIN_USERNAME_LENGTH +
                " and " + ConstantsUtils.IdentityConstants.MAX_USERNAME_LENGTH + " characters")
    String username;

    @Email(message = "Email should be valid")
    String email;

    @Size(
        min = ConstantsUtils.IdentityConstants.MIN_PHONE_NUMBER_LENGTH, 
        max = ConstantsUtils.IdentityConstants.MAX_PHONE_NUMBER_LENGTH, 
        message = "Phone number must be between " + ConstantsUtils.IdentityConstants.MIN_PHONE_NUMBER_LENGTH +
                " and " + ConstantsUtils.IdentityConstants.MAX_PHONE_NUMBER_LENGTH + " characters")
    String phoneNumber;

    String fullName;

    String address;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate idIssueDate;

    String idIssuePlace;

    String nationality;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dateOfBirth;

}
