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
@Table(name = "user_vocabulary_progress",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "vocabulary_item_id"})
        })
public class UserVocabularyProgress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vocabulary_item_id", nullable = false)
  private VocabularyItem vocabularyItem;

  @Column(name = "times_correct", nullable = false)
  private Integer timesCorrect = 0;

  @Column(name = "times_wrong", nullable = false)
  private Integer timesWrong = 0;

  @Column(nullable = false)
  private Boolean mastered = false;

  @Column(name = "last_reviewed_at")
  private Instant lastReviewedAt;
}
