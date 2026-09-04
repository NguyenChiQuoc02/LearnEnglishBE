package com.personal.base.services;

import com.personal.base.dto.minio.MinioFileResponse;
import com.personal.base.dto.minio.MinioUploadResult;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class MinioService {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
          "jpg", "jpeg", "png", "gif", "webp",
          "mp4", "mov", "avi", "webm",
          "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf"
  );

  @Autowired
  private MinioClient minioClient;

  @Value("${minio.bucket}")
  private String bucket;

  @Value("${minio.endpoint}")
  private String endpoint;

  public MinioFileResponse uploadFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn file để upload");
    }

    String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
    String extension = extractExtension(originalFilename);
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Định dạng file không được hỗ trợ. Chỉ chấp nhận ảnh, video, Word, Excel, PowerPoint, PDF.");
    }

    String objectName = UUID.randomUUID() + "_" + sanitizeFilename(originalFilename);

    try (InputStream inputStream = file.getInputStream()) {
      minioClient.putObject(PutObjectArgs.builder()
              .bucket(bucket)
              .object(objectName)
              .stream(inputStream, file.getSize(), -1)
              .contentType(file.getContentType())
              .build());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload lên MinIO thất bại: " + e.getMessage());
    }

    return new MinioFileResponse(
            objectName,
            originalFilename,
            buildPublicUrl(objectName),
            file.getSize(),
            file.getContentType(),
            Instant.now()
    );
  }

  // Uploads each file independently and keeps going on a per-file failure (e.g. one file has an
  // unsupported extension) so a bad file in the batch doesn't block the rest from uploading.
  public List<MinioUploadResult> uploadFiles(MultipartFile[] files) {
    List<MinioUploadResult> results = new ArrayList<>();
    for (MultipartFile file : files) {
      String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
      try {
        MinioFileResponse response = uploadFile(file);
        results.add(new MinioUploadResult(filename, true, response, null));
      } catch (ResponseStatusException e) {
        results.add(new MinioUploadResult(filename, false, null, e.getReason()));
      }
    }
    return results;
  }

  public List<MinioFileResponse> listFiles() {
    try {
      Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).build());

      return StreamSupport.stream(results.spliterator(), false)
              .map(this::toFileResponse)
              .sorted(Comparator.comparing(MinioFileResponse::uploadedAt).reversed())
              .collect(Collectors.toList());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lấy được danh sách file: " + e.getMessage());
    }
  }

  public void deleteFile(String objectName) {
    try {
      minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Xóa file thất bại: " + e.getMessage());
    }
  }

  private MinioFileResponse toFileResponse(Result<Item> result) {
    try {
      Item item = result.get();
      return new MinioFileResponse(
              item.objectName(),
              extractOriginalFilename(item.objectName()),
              buildPublicUrl(item.objectName()),
              item.size(),
              null,
              item.lastModified() != null ? item.lastModified().toInstant() : Instant.now()
      );
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không đọc được thông tin file: " + e.getMessage());
    }
  }

  private String buildPublicUrl(String objectName) {
    return endpoint + "/" + bucket + "/" + objectName;
  }

  private String extractOriginalFilename(String objectName) {
    int idx = objectName.indexOf('_');
    return idx >= 0 && idx < objectName.length() - 1 ? objectName.substring(idx + 1) : objectName;
  }

  private String extractExtension(String filename) {
    int idx = filename.lastIndexOf('.');
    return idx >= 0 && idx < filename.length() - 1 ? filename.substring(idx + 1).toLowerCase() : "";
  }

  private String sanitizeFilename(String filename) {
    return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
