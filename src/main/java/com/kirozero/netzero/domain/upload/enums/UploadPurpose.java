package com.kirozero.netzero.domain.upload.enums;

public enum UploadPurpose {
    COOKED_PHOTO("cooked-photo"),
    AFTER_PHOTO("after-photo"),
    GENERAL("general");

    private final String folderName;

    UploadPurpose(String folderName) {
        this.folderName = folderName;
    }

    public String folderName() {
        return folderName;
    }
}
