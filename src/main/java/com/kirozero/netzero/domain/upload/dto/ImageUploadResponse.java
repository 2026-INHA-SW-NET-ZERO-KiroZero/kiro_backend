package com.kirozero.netzero.domain.upload.dto;

public record ImageUploadResponse(
        String fileUrl,
        String objectKey,
        String contentType,
        long size
) {
}
