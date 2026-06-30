package com.github.dghng36.eauction.modules.media.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.modules.media.dto.response.MediaFileUploadResponse;
import com.github.dghng36.eauction.modules.media.service.InternalMediaService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;



@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MediaController {
    InternalMediaService mediaService;

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @PostMapping(
        value = "/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ResponseEntity<ApiResponse<MediaFileUploadResponse>> uploadMedia(
        @RequestPart("file") MultipartFile file
    ) {
        log.info("Uploading media file: {}", file.getOriginalFilename());

        MediaFileUploadResponse mediaUploadResp = mediaService.upload(file);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/media/view/{mediaCode}")
            .buildAndExpand(mediaUploadResp.getMediaCode())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(ApiResponse.success("Media uploaded successfully", mediaUploadResp));
    }

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @GetMapping("/view/{mediaCode}")
    ResponseEntity<Void> viewMedia(@PathVariable String mediaCode) {
        String originalUrl = mediaService.resolve(mediaCode);

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build();
    }
    
}
