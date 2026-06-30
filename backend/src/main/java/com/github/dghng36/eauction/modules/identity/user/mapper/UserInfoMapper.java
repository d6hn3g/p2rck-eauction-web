package com.github.dghng36.eauction.modules.identity.user.mapper;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.model.User;

@Component
public class UserInfoMapper {
    public UserInfo toUserInfo(User user) {
        if (user == null) {
            return UserInfo.builder()
                .id("N/A")
                .username("N/A")
                .avatar(null)
                .build();
        }

        return UserInfo.builder()
            .id(user.getId())
            .username(user.getUsername())
            .avatar(user.getAvatar())
            .build();
    }
}
