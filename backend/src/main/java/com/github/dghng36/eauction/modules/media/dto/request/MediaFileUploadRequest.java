package com.github.dghng36.eauction.modules.media.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class MediaFileUploadRequest {
    @NotBlank(message = "Media code is required")
    String mediaCode;

    @NotBlank(message = "Object key is required")
    String objectKey;
}
