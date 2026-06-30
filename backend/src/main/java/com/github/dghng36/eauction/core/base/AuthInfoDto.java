package com.github.dghng36.eauction.core.base;

import com.github.dghng36.eauction.modules.identity.enums.UserRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthInfoDto {
    String id;
    String username;
    UserRole role;
}
