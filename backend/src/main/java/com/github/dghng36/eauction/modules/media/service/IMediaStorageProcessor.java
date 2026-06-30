package com.github.dghng36.eauction.modules.media.service;

import org.springframework.web.multipart.MultipartFile;

import com.github.dghng36.eauction.modules.media.dto.internal.UploadResult;

public interface IMediaStorageProcessor {
    UploadResult upload(MultipartFile file, String objectKey);

    void delete(String objectKey);

    String getFileUrl(String objectKey);
}
