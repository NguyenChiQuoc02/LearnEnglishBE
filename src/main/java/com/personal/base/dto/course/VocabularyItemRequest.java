package com.personal.base.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VocabularyItemRequest {
  @NotBlank
  private String word;

  private String phonetic;

  private String partOfSpeech;

  private String meaning;

  private String exampleSentence;

  private String exampleTranslation;

  private String imageUrl;

  private String audioUrl;

  private Integer orderIndex;
}
