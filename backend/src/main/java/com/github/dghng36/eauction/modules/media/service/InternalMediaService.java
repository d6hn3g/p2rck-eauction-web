package com.github.dghng36.eauction.modules.media.service;

import org.springframework.web.multipart.MultipartFile;

import com.github.dghng36.eauction.modules.media.dto.response.MediaFileUploadResponse;

public interface InternalMediaService {
    MediaFileUploadResponse upload(MultipartFile file);

    String resolve(String mediaCode);

    void delete(String mediaCode);
}
