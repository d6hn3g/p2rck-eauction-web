package com.github.dghng36.eauction.modules.identity.user.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(
        min = ConstantsUtils.IdentityConstants.MIN_USERNAME_LENGTH,
        max = ConstantsUtils.IdentityConstants.MAX_USERNAME_LENGTH,
        message = "Username must be between " + ConstantsUtils.IdentityConstants.MIN_USERNAME_LENGTH +
                " and " + ConstantsUtils.IdentityConstants.MAX_USERNAME_LENGTH + " characters"
    )
    String username;

    @NotBlank(message = "Password is required")
    @Size(
        min = ConstantsUtils.IdentityConstants.MIN_PASSWORD_LENGTH,
        max = ConstantsUtils.IdentityConstants.MAX_PASSWORD_LENGTH,
        message = "Password must be between " + ConstantsUtils.IdentityConstants.MIN_PASSWORD_LENGTH +
                " and " + ConstantsUtils.IdentityConstants.MAX_PASSWORD_LENGTH + " characters"
    )
    String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email;

    @NotBlank(message = "Full name is required")
    String fullName;

    @NotBlank(message = "Phone number is required")
    @Size(
        min = ConstantsUtils.IdentityConstants.MIN_PHONE_NUMBER_LENGTH,
        max = ConstantsUtils.IdentityConstants.MAX_PHONE_NUMBER_LENGTH,
        message = "Phone number must be between " + ConstantsUtils.IdentityConstants.MIN_PHONE_NUMBER_LENGTH +
                " and " + ConstantsUtils.IdentityConstants.MAX_PHONE_NUMBER_LENGTH + " characters"
    )
    String phoneNumber;

    @NotBlank(message = "National ID is required")
    String nationalId;

    @NotNull(message = "ID issue date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate idIssueDate;

    @NotBlank(message = "ID issue place is required")
    String idIssuePlace;

    @NotBlank(message = "Address is required")
    String address;

    @NotNull(message = "Date of birth is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dateOfBirth;

    @NotBlank(message = "Nationality is required")
    String nationality;

    
}
