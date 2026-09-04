package com.personal.base.dto.minio;

import java.time.Instant;

public record MinioFileResponse(
        String objectName,
        String originalFilename,
        String url,
        long size,
        String contentType,
        Instant uploadedAt
) {
}
