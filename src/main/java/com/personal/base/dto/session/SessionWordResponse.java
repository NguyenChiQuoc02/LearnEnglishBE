package com.personal.base.dto.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// The word is included so the client can compute letter-by-letter hints
// locally, but it must not be rendered in the UI before the learner answers —
// the server still independently validates the answer in SubmitAnswerRequest.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionWordResponse {
  private Long vocabularyItemId;
  private String word;
  private String phonetic;
  private String partOfSpeech;
  private String meaning;
  private String imageUrl;
  private String audioUrl;
  private Integer orderIndex;
}
