package com.personal.base.controllers;

import com.personal.base.dto.export.ExportRequest;
import com.personal.base.services.export.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// One request in, the file streamed straight back in the response body — nothing is
// written to disk, so there is no job/status to poll and nothing left over to clean up.
@RestController
@RequestMapping("/api/exports")
@PreAuthorize("hasRole('ADMIN')")
public class ExportController {

  @Autowired
  private ExportService exportService;

  @PostMapping("/users")
  public void exportUsers(@Valid @RequestBody ExportRequest request, HttpServletResponse response) throws IOException {
    var role = exportService.resolveRole(request.getRole()); // throws before any header is set if invalid

    String fileName = exportService.buildFileName(request.getFormat());
    String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

    response.setContentType(exportService.getMediaType(request.getFormat()).toString());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);

    exportService.writeUsersExport(request.getFormat(), role, request.getKeyword(), response.getOutputStream());
  }
}
