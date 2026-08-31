package com.personal.base.dto.notification;

import com.personal.base.models.NotificationDeliveryType;
import com.personal.base.models.NotificationRecurrenceType;
import com.personal.base.models.NotificationTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.time.LocalTime;

@Data
public class NotificationRequest {
  @NotBlank
  private String title;

  @NotBlank
  private String content;

  private String imageUrl;

  private String link;

  @NotNull
  private NotificationTargetType targetType;

  // Required when targetType = COURSE.
  private Long targetCourseId;

  @NotNull
  private NotificationDeliveryType deliveryType;

  // Required when deliveryType = SCHEDULED. Defaults to NONE otherwise.
  private NotificationRecurrenceType recurrenceType;

  // Required when deliveryType=SCHEDULED and recurrenceType=NONE (one-time future send).
  private Instant scheduledAt;

  // Required when deliveryType=SCHEDULED and recurrenceType=DAILY (e.g. 19:00 every day).
  private LocalTime dailyTime;
}
