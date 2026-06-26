package com.kirozero.netzero.domain.upload.controller;

import com.kirozero.netzero.domain.upload.dto.ImageUploadResponse;
import com.kirozero.netzero.domain.upload.enums.UploadPurpose;
import com.kirozero.netzero.domain.upload.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping
    public ImageUploadResponse uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "GENERAL") UploadPurpose purpose
    ) {
        return imageUploadService.upload(file, purpose);
    }
}
