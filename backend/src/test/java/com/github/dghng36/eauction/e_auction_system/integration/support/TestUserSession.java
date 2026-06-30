package com.github.dghng36.eauction.e_auction_system.integration.support;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestUserSession {
    String userId;
    String username;
    String accessToken;
}
