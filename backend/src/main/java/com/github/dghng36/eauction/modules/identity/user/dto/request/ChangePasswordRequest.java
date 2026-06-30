package com.github.dghng36.eauction.modules.identity.user.dto.request;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.NotBlank;
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
public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    @Size(
        min = ConstantsUtils.IdentityConstants.MIN_PASSWORD_LENGTH, 
        max = ConstantsUtils.IdentityConstants.MAX_PASSWORD_LENGTH,
        message = "Current password must be between " + ConstantsUtils.IdentityConstants.MIN_PASSWORD_LENGTH +
                " and " + ConstantsUtils.IdentityConstants.MAX_PASSWORD_LENGTH + " characters")
    String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(
        min = ConstantsUtils.IdentityConstants.MIN_PASSWORD_LENGTH, 
        max = ConstantsUtils.IdentityConstants.MAX_PASSWORD_LENGTH,
        message = "New password must be between " + ConstantsUtils.IdentityConstants.MIN_PASSWORD_LENGTH +
                " and " + ConstantsUtils.IdentityConstants.MAX_PASSWORD_LENGTH + " characters")
    String newPassword;
}
