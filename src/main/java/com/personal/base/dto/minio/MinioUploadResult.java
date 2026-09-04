package com.personal.base.dto.minio;

public record MinioUploadResult(
        String filename,
        boolean success,
        MinioFileResponse file,
        String error
) {
}
