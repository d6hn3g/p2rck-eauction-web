package com.github.dghng36.eauction.e_auction_system.integration.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.github.dghng36.eauction.modules.media.dto.internal.UploadResult;
import com.github.dghng36.eauction.modules.media.service.IMediaStorageProcessor;

import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration
public class IntegrationTestConfig {

    @Bean
    @Primary
    S3Client testS3Client() {
        return mock(S3Client.class);
    }

    @Bean
    @Primary
    IMediaStorageProcessor testMediaStorageProcessor() {
        IMediaStorageProcessor processor = mock(IMediaStorageProcessor.class);
        when(processor.upload(any(), anyString())).thenAnswer(invocation ->
            UploadResult.builder()
                .originalUrl("https://test-storage.example.com/" + invocation.getArgument(1))
                .build()
        );
        return processor;
    }
}
