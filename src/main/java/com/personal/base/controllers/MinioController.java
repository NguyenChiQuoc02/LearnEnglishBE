package com.personal.base.controllers;

import com.personal.base.dto.minio.MinioFileResponse;
import com.personal.base.dto.minio.MinioUploadResult;
import com.personal.base.services.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Backs the admin "Upload Minio" test page under Kiểm thử & Test — lets an admin verify the
 * MinIO integration by uploading images/videos/office docs and browsing what's in the bucket.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/minio")
public class MinioController {

  @Autowired
  private MinioService minioService;

  @PostMapping("/upload")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<MinioUploadResult>> upload(@RequestParam("files") MultipartFile[] files) {
    return ResponseEntity.ok(minioService.uploadFiles(files));
  }

  @GetMapping("/files")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<MinioFileResponse>> listFiles() {
    return ResponseEntity.ok(minioService.listFiles());
  }

  @DeleteMapping("/files/{objectName}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteFile(@PathVariable String objectName) {
    minioService.deleteFile(objectName);
    return ResponseEntity.noContent().build();
  }
}
