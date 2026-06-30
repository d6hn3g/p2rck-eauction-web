package com.github.dghng36.eauction.modules.identity.auth.dto.internal;

import java.time.Instant;

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
public class JwtGeneratedResult {
    String accessToken;
    
    String refreshToken;
    Instant refreshTokenExpiryDate;
}
