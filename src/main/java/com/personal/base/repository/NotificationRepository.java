package com.personal.base.repository;

import com.personal.base.models.Notification;
import com.personal.base.models.type.NotificationDeliveryType;
import com.personal.base.models.type.NotificationRecurrenceType;
import com.personal.base.models.type.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
  // One-time scheduled notifications whose send time has arrived.
  List<Notification> findByStatusAndDeliveryTypeAndScheduledAtLessThanEqual(
          NotificationStatus status, NotificationDeliveryType deliveryType, Instant now);

  // Daily-recurring notifications still active (checked against dailyTime by the scheduler).
  List<Notification> findByStatusAndRecurrenceType(
          NotificationStatus status, NotificationRecurrenceType recurrenceType);

  List<Notification> findAllByOrderByCreatedAtDesc();
}
