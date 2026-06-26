package com.kirozero.netzero.domain.upload.service;

import com.kirozero.netzero.domain.upload.dto.ImageUploadResponse;
import com.kirozero.netzero.domain.upload.enums.UploadPurpose;
import com.kirozero.netzero.domain.upload.storage.ObjectStorageClient;
import com.kirozero.netzero.domain.upload.storage.StoredObject;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ObjectStorageClient objectStorageClient;

    public ImageUploadResponse upload(MultipartFile file, UploadPurpose purpose) {
        validate(file);

        String contentType = file.getContentType();
        String objectKey = buildObjectKey(file.getOriginalFilename(), purpose);

        try {
            StoredObject storedObject = objectStorageClient.upload(
                    objectKey,
                    contentType,
                    file.getSize(),
                    file.getInputStream()
            );
            return new ImageUploadResponse(
                    storedObject.fileUrl(),
                    storedObject.objectKey(),
                    storedObject.contentType(),
                    storedObject.size()
            );
        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 upload failed.", e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo file is required.");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only jpg, png, and webp images are allowed.");
        }
    }

    private String buildObjectKey(String originalFilename, UploadPurpose purpose) {
        String extension = extensionOf(originalFilename);
        return "uploads/" + purpose.folderName() + "/" + UUID.randomUUID() + "." + extension;
    }

    private String extensionOf(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(extension)) {
            return "jpg";
        }

        String normalized = extension.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return StringUtils.hasText(normalized) ? normalized : "jpg";
    }
}
