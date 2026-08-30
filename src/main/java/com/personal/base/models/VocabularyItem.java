package com.personal.base.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "vocabulary_items")
public class VocabularyItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @NotBlank
  private String word;

  private String phonetic;

  @Column(name = "part_of_speech", length = 20)
  private String partOfSpeech;

  @Column(length = 1000)
  private String meaning;

  @Column(name = "example_sentence", length = 1000)
  private String exampleSentence;

  @Column(name = "example_translation", length = 1000)
  private String exampleTranslation;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "audio_url")
  private String audioUrl;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex = 0;
}
