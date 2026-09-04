package com.personal.base.controllers;

import com.personal.base.dto.config.UploadMethodRequest;
import com.personal.base.dto.config.UploadMethodResponse;
import com.personal.base.services.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Any authenticated user can read the active upload method (every upload form in the app
// needs it to decide which backend to call), but only an admin can change it.
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/config")
public class SystemConfigController {

  @Autowired
  private SystemConfigService systemConfigService;

  @GetMapping("/upload-method")
  public ResponseEntity<UploadMethodResponse> getUploadMethod() {
    return ResponseEntity.ok(new UploadMethodResponse(systemConfigService.getUploadMethod()));
  }

  @PutMapping("/upload-method")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UploadMethodResponse> updateUploadMethod(@RequestBody UploadMethodRequest request) {
    return ResponseEntity.ok(new UploadMethodResponse(systemConfigService.updateUploadMethod(request.uploadMethod())));
  }
}
