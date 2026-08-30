package com.personal.base.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "courses")
public class Course {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  private String title;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "course_type", length = 20, nullable = false)
  private CourseType courseType;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private CourseLevel level;

  @Column(name = "thumbnail_url")
  private String thumbnailUrl;

  // Owning teacher — must hold ROLE_TEACHER.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", nullable = false)
  private User teacher;

  // Admin who created the course.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "words_per_session", nullable = false)
  private Integer wordsPerSession = 10;

  @Column(name = "points_per_correct", nullable = false)
  private Integer pointsPerCorrect = 10;

  @Column(name = "points_per_wrong", nullable = false)
  private Integer pointsPerWrong = -2;

  @Column(name = "total_words", nullable = false)
  private Integer totalWords = 0;

  @Column(nullable = false)
  private Boolean published = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
