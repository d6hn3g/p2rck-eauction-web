package com.github.dghng36.eauction.modules.identity.auth.dto.request;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class LoginRequest {
    @NotBlank(message = "Identifier is required")
    String identifier;

    @NotBlank(message = "Password is required")
    @Size(
        min = ConstantsUtils.IdentityConstants.MIN_PASSWORD_LENGTH, 
        max = ConstantsUtils.IdentityConstants.MAX_PASSWORD_LENGTH, 
        message = "Password must be between " + ConstantsUtils.IdentityConstants.MIN_PASSWORD_LENGTH +
                " and " + ConstantsUtils.IdentityConstants.MAX_PASSWORD_LENGTH + " characters"
    )
    String password;
}
