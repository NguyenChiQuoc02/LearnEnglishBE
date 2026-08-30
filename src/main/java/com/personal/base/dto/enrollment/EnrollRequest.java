package com.personal.base.dto.enrollment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollRequest {
  @NotNull
  private Long courseId;
}
