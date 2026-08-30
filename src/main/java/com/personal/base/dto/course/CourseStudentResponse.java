package com.personal.base.dto.course;

import com.personal.base.models.Enrollment;
import com.personal.base.models.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseStudentResponse {
  private Long enrollmentId;
  private Long userId;
  private String username;
  private String email;
  private EnrollmentStatus status;
  private Integer totalScore;
  private Integer wordsLearnedCount;
  private Instant enrolledAt;
  private Instant lastStudiedAt;

  public static CourseStudentResponse from(Enrollment enrollment) {
    return new CourseStudentResponse(
            enrollment.getId(),
            enrollment.getUser().getId(),
            enrollment.getUser().getUsername(),
            enrollment.getUser().getEmail(),
            enrollment.getStatus(),
            enrollment.getTotalScore(),
            enrollment.getWordsLearnedCount(),
            enrollment.getEnrolledAt(),
            enrollment.getLastStudiedAt());
  }
}
