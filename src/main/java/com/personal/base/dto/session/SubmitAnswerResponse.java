package com.personal.base.dto.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitAnswerResponse {
  private Boolean correct;
  private String correctWord;
  private Integer pointsEarned;
  private Integer sessionScoreSoFar;
}
