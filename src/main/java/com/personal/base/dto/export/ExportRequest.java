package com.personal.base.dto.export;

import com.personal.base.models.type.ExportFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportRequest {
  @NotNull
  private ExportFormat format;

  // Optional export conditions: role name (USER/TEACHER/ADMIN) and a free-text
  // keyword matched against username/email/phone.
  private String role;
  private String keyword;
}
