package com.github.dghng36.eauction.modules.media.dto.response;

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
public class MediaFileUploadResponse {
    String mediaCode;

    String mediaUrl;

    String originalUrl;

    String objectKey;

    String contentType;

    Long size;
}
