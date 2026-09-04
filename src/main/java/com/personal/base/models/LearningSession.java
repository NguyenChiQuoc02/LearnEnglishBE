package com.personal.base.models;

import com.personal.base.models.type.SessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "learning_sessions")
public class LearningSession {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private Enrollment enrollment;

  @Column(name = "total_words", nullable = false)
  private Integer totalWords;

  @Column(name = "correct_count", nullable = false)
  private Integer correctCount = 0;

  @Column(name = "wrong_count", nullable = false)
  private Integer wrongCount = 0;

  @Column(name = "score_earned", nullable = false)
  private Integer scoreEarned = 0;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  private SessionStatus status = SessionStatus.IN_PROGRESS;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt = Instant.now();

  @Column(name = "completed_at")
  private Instant completedAt;
}
