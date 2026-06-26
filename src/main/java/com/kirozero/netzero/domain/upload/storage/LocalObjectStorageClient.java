package com.kirozero.netzero.domain.upload.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "netzero3.storage.s3", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalObjectStorageClient implements ObjectStorageClient {

    private final String publicBaseUrl;

    public LocalObjectStorageClient(
            @Value("${netzero3.storage.s3.public-base-url:http://localhost:8080/mock-s3}") String publicBaseUrl
    ) {
        this.publicBaseUrl = StringUtils.hasText(publicBaseUrl)
                ? publicBaseUrl.replaceAll("/+$", "")
                : "http://localhost:8080/mock-s3";
    }

    @Override
    public StoredObject upload(String objectKey, String contentType, long size, InputStream inputStream) {
        try {
            StreamUtils.copyToByteArray(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read upload input stream.", e);
        }
        return new StoredObject(objectKey, publicBaseUrl + "/" + objectKey, contentType, size);
    }
}
