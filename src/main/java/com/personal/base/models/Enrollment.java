package com.personal.base.models;

import com.personal.base.models.type.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "course_id"})
        })
public class Enrollment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

  // Cumulative score within this course — drives the course leaderboard/ranking.
  @Column(name = "total_score", nullable = false)
  private Integer totalScore = 0;

  @Column(name = "words_learned_count", nullable = false)
  private Integer wordsLearnedCount = 0;

  @Column(name = "enrolled_at", nullable = false, updatable = false)
  private Instant enrolledAt = Instant.now();

  @Column(name = "last_studied_at")
  private Instant lastStudiedAt;
}
