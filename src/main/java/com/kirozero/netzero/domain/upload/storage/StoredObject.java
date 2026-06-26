package com.kirozero.netzero.domain.upload.storage;

public record StoredObject(
        String objectKey,
        String fileUrl,
        String contentType,
        long size
) {
}
