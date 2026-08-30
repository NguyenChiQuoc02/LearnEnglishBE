package com.personal.base.dto.session;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerRequest {
  @NotNull
  private Long vocabularyItemId;

  private String answer;

  private Boolean skipped = false;

  private Boolean usedHint = false;
}
