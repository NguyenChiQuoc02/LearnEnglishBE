package com.personal.base.services.export;

import com.personal.base.models.type.ERole;
import com.personal.base.models.type.ExportFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Synchronous export: one HTTP request in, the file streamed straight back out in the
// same response — nothing is persisted on the server (no job table, no exports/ folder).
@Service
public class ExportService {

  @Autowired
  private UserExportWriter userExportWriter;

  // Resolves + validates the filter up front, before the controller commits any
  // response headers, so an invalid role still comes back as a clean 400 JSON body
  // instead of a half-written attachment.
  public ERole resolveRole(String roleParam) {
    return parseRole(roleParam);
  }

  public void writeUsersExport(ExportFormat format, ERole role, String keywordParam, OutputStream out) throws java.io.IOException {
    String keyword = normalizeKeyword(keywordParam);

    switch (format) {
      case EXCEL -> userExportWriter.writeExcel(role, keyword, out);
      case WORD -> userExportWriter.writeWord(role, keyword, out);
      case PDF -> userExportWriter.writePdf(role, keyword, out);
    }
  }

  public String buildFileName(ExportFormat format) {
    String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(Instant.now().atZone(ZoneId.systemDefault()));
    String extension = switch (format) {
      case EXCEL -> "xlsx";
      case WORD -> "docx";
      case PDF -> "pdf";
    };
    return "users_export_" + timestamp + "." + extension;
  }

  public MediaType getMediaType(ExportFormat format) {
    return switch (format) {
      case EXCEL -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      case WORD -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
      case PDF -> MediaType.APPLICATION_PDF;
    };
  }

  private ERole parseRole(String role) {
    if (role == null || role.isBlank()) return null;
    String normalized = role.trim().toUpperCase();
    if (!normalized.startsWith("ROLE_")) {
      normalized = "ROLE_" + normalized;
    }
    try {
      return ERole.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ: " + role);
    }
  }

  private String normalizeKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) return null;
    return keyword.trim().toLowerCase();
  }
}
