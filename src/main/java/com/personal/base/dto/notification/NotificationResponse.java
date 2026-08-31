package com.personal.base.dto.notification;

import com.personal.base.models.Notification;
import com.personal.base.models.NotificationDeliveryType;
import com.personal.base.models.NotificationRecurrenceType;
import com.personal.base.models.NotificationStatus;
import com.personal.base.models.NotificationTargetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
  private Long id;
  private String title;
  private String content;
  private String imageUrl;
  private String link;
  private NotificationTargetType targetType;
  private Long targetCourseId;
  private String targetCourseTitle;
  private NotificationDeliveryType deliveryType;
  private NotificationRecurrenceType recurrenceType;
  private Instant scheduledAt;
  private LocalTime dailyTime;
  private NotificationStatus status;
  private Instant lastSentAt;
  private Long createdById;
  private String createdByUsername;
  private Instant createdAt;

  public static NotificationResponse from(Notification n) {
    return new NotificationResponse(
            n.getId(),
            n.getTitle(),
            n.getContent(),
            n.getImageUrl(),
            n.getLink(),
            n.getTargetType(),
            n.getTargetCourse() != null ? n.getTargetCourse().getId() : null,
            n.getTargetCourse() != null ? n.getTargetCourse().getTitle() : null,
            n.getDeliveryType(),
            n.getRecurrenceType(),
            n.getScheduledAt(),
            n.getDailyTime(),
            n.getStatus(),
            n.getLastSentAt(),
            n.getCreatedBy() != null ? n.getCreatedBy().getId() : null,
            n.getCreatedBy() != null ? n.getCreatedBy().getUsername() : null,
            n.getCreatedAt());
  }
}
