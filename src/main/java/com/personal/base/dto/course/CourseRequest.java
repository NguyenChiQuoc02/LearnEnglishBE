package com.personal.base.dto.course;

import com.personal.base.models.CourseLevel;
import com.personal.base.models.CourseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseRequest {
  @NotBlank
  private String title;

  private String description;

  @NotNull
  private CourseType courseType;

  private CourseLevel level;

  private String thumbnailUrl;

  @NotNull
  private Long teacherId;

  private Integer wordsPerSession;

  private Integer pointsPerCorrect;

  private Integer pointsPerWrong;

  private Boolean published;

  private BigDecimal price;
}
