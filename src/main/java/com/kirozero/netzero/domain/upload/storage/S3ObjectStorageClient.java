package com.kirozero.netzero.domain.upload.storage;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@ConditionalOnProperty(prefix = "netzero3.storage.s3", name = "enabled", havingValue = "true")
public class S3ObjectStorageClient implements ObjectStorageClient {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;
    private final String publicBaseUrl;

    public S3ObjectStorageClient(
            @Value("${netzero3.storage.s3.bucket}") String bucket,
            @Value("${netzero3.storage.s3.region}") String region,
            @Value("${netzero3.storage.s3.public-base-url:}") String publicBaseUrl
    ) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
        this.bucket = bucket;
        this.region = region;
        this.publicBaseUrl = StringUtils.hasText(publicBaseUrl) ? publicBaseUrl.replaceAll("/+$", "") : null;
    }

    @Override
    public StoredObject upload(String objectKey, String contentType, long size, InputStream inputStream) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(size)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));

        return new StoredObject(objectKey, buildPublicUrl(objectKey), contentType, size);
    }

    private String buildPublicUrl(String objectKey) {
        if (publicBaseUrl != null) {
            return publicBaseUrl + "/" + objectKey;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + objectKey;
    }
}
