package com.kirozero.netzero.domain.upload.storage;

import java.io.InputStream;

public interface ObjectStorageClient {

    StoredObject upload(String objectKey, String contentType, long size, InputStream inputStream);
}
