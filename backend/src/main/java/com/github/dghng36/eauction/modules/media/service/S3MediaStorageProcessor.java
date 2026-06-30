package com.github.dghng36.eauction.modules.media.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.media.dto.internal.UploadResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class S3MediaStorageProcessor implements IMediaStorageProcessor{
    S3Client s3Client;

    @NonFinal
    @Value("${aws.s3.bucket-name}")
    String bucketName;

    @NonFinal
    @Value("${aws.s3.region}")
    String region;

    @Override
    public UploadResult upload(
        MultipartFile file,
        String objectKey
    ) {
        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

            s3Client.putObject(putReq, RequestBody.fromBytes(file.getBytes()));

            String fileUrl = getFileUrl(objectKey);

            return UploadResult.builder()
                .objectKey(objectKey)
                .originalUrl(fileUrl)
                .build();

        } catch (IOException e) {
            throw new AppException("Failed to upload media to S3" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (S3Exception e) {
            throw new AppException("S3 error: " + e.awsErrorDetails().errorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
             DeleteObjectRequest delReq =
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build();

            s3Client.deleteObject(delReq);
        } catch (S3Exception e) {
            throw new AppException("S3 error: " + e.awsErrorDetails().errorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String getFileUrl(String objectKey) {
        return String.format(
            "https://%s.s3.%s.amazonaws.com/%s", 
            bucketName, 
            region, 
            objectKey
        );
    }
}
