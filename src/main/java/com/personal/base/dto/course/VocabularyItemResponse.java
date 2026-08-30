package com.personal.base.dto.course;

import com.personal.base.models.VocabularyItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VocabularyItemResponse {
  private Long id;
  private String word;
  private String phonetic;
  private String partOfSpeech;
  private String meaning;
  private String exampleSentence;
  private String exampleTranslation;
  private String imageUrl;
  private String audioUrl;
  private Integer orderIndex;

  public static VocabularyItemResponse from(VocabularyItem item) {
    return new VocabularyItemResponse(
            item.getId(),
            item.getWord(),
            item.getPhonetic(),
            item.getPartOfSpeech(),
            item.getMeaning(),
            item.getExampleSentence(),
            item.getExampleTranslation(),
            item.getImageUrl(),
            item.getAudioUrl(),
            item.getOrderIndex());
  }
}
