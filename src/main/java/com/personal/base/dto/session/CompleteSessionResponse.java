package com.personal.base.dto.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteSessionResponse {
  private Long sessionId;
  private Integer totalWords;
  private Integer correctCount;
  private Integer wrongCount;
  private Integer scoreEarned;
  private Integer enrollmentTotalScore;
}
