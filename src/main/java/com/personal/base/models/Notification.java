package com.personal.base.models;

import com.personal.base.models.type.NotificationDeliveryType;
import com.personal.base.models.type.NotificationRecurrenceType;
import com.personal.base.models.type.NotificationStatus;
import com.personal.base.models.type.NotificationTargetType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notifications")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  private String title;

  @Column(length = 4000)
  private String content;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(length = 500)
  private String link;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", length = 20, nullable = false)
  private NotificationTargetType targetType;

  // Only set when targetType == COURSE.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_course_id")
  private Course targetCourse;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_type", length = 20, nullable = false)
  private NotificationDeliveryType deliveryType;

  @Enumerated(EnumType.STRING)
  @Column(name = "recurrence_type", length = 20, nullable = false)
  private NotificationRecurrenceType recurrenceType = NotificationRecurrenceType.NONE;

  // One-time scheduled send time. Only set when deliveryType=SCHEDULED and recurrenceType=NONE.
  @Column(name = "scheduled_at")
  private Instant scheduledAt;

  // Time of day for the daily recurring send (e.g. 19:00). Only set when recurrenceType=DAILY.
  @Column(name = "daily_time")
  private LocalTime dailyTime;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  private NotificationStatus status = NotificationStatus.PENDING;

  @Column(name = "last_sent_at")
  private Instant lastSentAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
