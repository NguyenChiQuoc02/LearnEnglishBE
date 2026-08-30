package com.personal.base.dto.session;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartSessionRequest {
  @NotNull
  private Long courseId;
}
