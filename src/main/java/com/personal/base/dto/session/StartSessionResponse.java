package com.personal.base.dto.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartSessionResponse {
  private Long sessionId;
  private Long courseId;
  private Integer totalWords;
  private List<SessionWordResponse> words;
}
