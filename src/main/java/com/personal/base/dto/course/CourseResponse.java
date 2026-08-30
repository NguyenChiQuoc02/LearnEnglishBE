package com.personal.base.dto.course;

import com.personal.base.models.Course;
import com.personal.base.models.CourseLevel;
import com.personal.base.models.CourseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseResponse {
  private Long id;
  private String title;
  private String description;
  private CourseType courseType;
  private CourseLevel level;
  private String thumbnailUrl;
  private Long teacherId;
  private String teacherName;
  private Integer wordsPerSession;
  private Integer pointsPerCorrect;
  private Integer pointsPerWrong;
  private Integer totalWords;
  private Boolean published;
  private Instant createdAt;

  public static CourseResponse from(Course course) {
    return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getCourseType(),
            course.getLevel(),
            course.getThumbnailUrl(),
            course.getTeacher().getId(),
            course.getTeacher().getUsername(),
            course.getWordsPerSession(),
            course.getPointsPerCorrect(),
            course.getPointsPerWrong(),
            course.getTotalWords(),
            course.getPublished(),
            course.getCreatedAt());
  }
}
