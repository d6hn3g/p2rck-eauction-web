package com.github.dghng36.eauction.modules.identity.user.dto.internal;

import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;

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
public class UserInfo {
    String id;
    String username;

    MediaFile avatar;
}
