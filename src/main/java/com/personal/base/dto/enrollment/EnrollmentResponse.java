package com.personal.base.dto.enrollment;

import com.personal.base.models.Enrollment;
import com.personal.base.models.type.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnrollmentResponse {
  private Long id;
  private Long courseId;
  private String courseTitle;
  private EnrollmentStatus status;
  private Integer totalScore;
  private Integer wordsLearnedCount;
  private Instant enrolledAt;
  private Instant lastStudiedAt;

  public static EnrollmentResponse from(Enrollment enrollment) {
    return new EnrollmentResponse(
            enrollment.getId(),
            enrollment.getCourse().getId(),
            enrollment.getCourse().getTitle(),
            enrollment.getStatus(),
            enrollment.getTotalScore(),
            enrollment.getWordsLearnedCount(),
            enrollment.getEnrolledAt(),
            enrollment.getLastStudiedAt());
  }
}
