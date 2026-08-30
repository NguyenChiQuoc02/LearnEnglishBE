package com.personal.base.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "learning_session_items")
public class LearningSessionItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private LearningSession session;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vocabulary_item_id", nullable = false)
  private VocabularyItem vocabularyItem;

  @Column(name = "user_answer")
  private String userAnswer;

  @Column(nullable = false)
  private Boolean correct = false;

  @Column(name = "used_hint", nullable = false)
  private Boolean usedHint = false;

  // "I don't know" — user skipped without answering.
  @Column(nullable = false)
  private Boolean skipped = false;

  @Column(name = "points_earned", nullable = false)
  private Integer pointsEarned = 0;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex = 0;

  @Column(name = "answered_at")
  private Instant answeredAt;
}
