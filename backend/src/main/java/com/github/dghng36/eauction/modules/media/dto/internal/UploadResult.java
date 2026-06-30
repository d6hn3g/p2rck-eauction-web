package com.github.dghng36.eauction.modules.media.dto.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UploadResult {
    String objectKey;

    String originalUrl;
}
