package com.github.dghng36.eauction.modules.media.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.modules.media.dto.internal.UploadResult;
import com.github.dghng36.eauction.modules.media.dto.response.MediaFileUploadResponse;
import com.github.dghng36.eauction.modules.media.helper.MediaCodeGeneratorHelper;
import com.github.dghng36.eauction.modules.media.model.MediaUrl;
import com.github.dghng36.eauction.modules.media.repository.MediaUrlRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MediaService implements InternalMediaService {
    MediaUrlRepository mediaUrlRepo;
    
    IMediaStorageProcessor iMediaStorageProcessor;

    MediaCodeGeneratorHelper mediaCodeGeneratorHelper;

    @NonFinal
    @Value("${app.base-url}")
    String baseUrl;

    @Override
    public MediaFileUploadResponse upload(MultipartFile file) {
        // Validate file (size, type, etc.)
        validateMediaFile(file);

        // Generate object key
        String objectKey = generateObjectKey(file);

        // Upload to media storage

        UploadResult uploadResult = iMediaStorageProcessor.upload(file, objectKey);

        // Generate unique media code
        String mediaCode = generateUniqueMediaCode(uploadResult.getOriginalUrl());

        // Save media URL mapping to database
        mediaUrlRepo.save(
            MediaUrl.builder()
                .mediaCode(mediaCode)
                .originalUrl(uploadResult.getOriginalUrl())
                .objectKey(objectKey)
                .active(true)
                .build()
        );

        log.info("Media file uploaded successfully: {}", uploadResult.getOriginalUrl());

        return MediaFileUploadResponse.builder()
            .mediaCode(mediaCode)
            .mediaUrl(baseUrl + "/api/v1/media/view" + mediaCode)
            .originalUrl(uploadResult.getOriginalUrl())
            .objectKey(objectKey)
            .contentType(file.getContentType())
            .size(file.getSize())
            .build();
    }

    @Override
    public String resolve(String mediaCode) {
        MediaUrl mediaUrl = mediaUrlRepo.findByMediaCodeAndActiveTrue(mediaCode)
            .orElseThrow(() -> new AppException("Media file not found or inactive", HttpStatus.NOT_FOUND));

        return mediaUrl.getOriginalUrl();
    }

    @Async
    @Override
    public void delete(String mediaCode) {
       MediaUrl mediaUrl = mediaUrlRepo.findByMediaCodeAndActiveTrue(mediaCode)
            .orElseThrow(() -> new AppException("Media file not found or inactive", HttpStatus.NOT_FOUND));


        iMediaStorageProcessor.delete(mediaUrl.getObjectKey());

        // Mark as inactive in database
        mediaUrl.setActive(false);
        mediaUrlRepo.save(mediaUrl);

        // Delete from media storage
        iMediaStorageProcessor.delete(mediaUrl.getObjectKey());

        log.info("Media file deleted successfully: {}", mediaUrl.getOriginalUrl());
    }

    // Utility methods
    private void validateMediaFile(MultipartFile file) {
        // Implement validation logic (e.g., check file size, type, etc.)
        if (file == null || file.isEmpty()) {
            throw new AppException("File is empty", HttpStatus.BAD_REQUEST);
        }

        // Limit file size
        if (file.getSize() > ConstantsUtils.MediaFileConstants.MAX_MEDIA_FILE_SIZE_BYTES) {
            throw new AppException("File size exceeds the maximum limit", HttpStatus.BAD_REQUEST);
        }
    }

    private String generateObjectKey(MultipartFile file) {
        // Use random UUID
        return UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
    }

    private String generateUniqueMediaCode(String originalUrl) {
        String codeGenerated = null;
        
        do {
            codeGenerated = mediaCodeGeneratorHelper.generate(originalUrl);
        } while (mediaUrlRepo.existsByMediaCode(codeGenerated));
        
        return codeGenerated;
    }
}
