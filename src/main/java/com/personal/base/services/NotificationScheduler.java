package com.personal.base.services;

import com.personal.base.models.Notification;
import com.personal.base.models.NotificationDeliveryType;
import com.personal.base.models.NotificationRecurrenceType;
import com.personal.base.models.NotificationStatus;
import com.personal.base.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class NotificationScheduler {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private NotificationService notificationService;

  // Picks up one-time SCHEDULED notifications whose scheduledAt has arrived.
  @Scheduled(cron = "0 * * * * *")
  public void dispatchOneTimeScheduledNotifications() {
    List<Notification> due = notificationRepository.findByStatusAndDeliveryTypeAndScheduledAtLessThanEqual(
            NotificationStatus.PENDING, NotificationDeliveryType.SCHEDULED, Instant.now());
    if (!due.isEmpty()) {
      notificationService.dispatchAll(due);
    }
  }

  // Fires DAILY-recurring notifications once per day, at the minute matching their dailyTime.
  @Scheduled(cron = "0 * * * * *")
  public void dispatchDailyRecurringNotifications() {
    LocalTime now = LocalTime.now().withSecond(0).withNano(0);
    LocalDate today = LocalDate.now();

    List<Notification> active = notificationRepository.findByStatusAndRecurrenceType(
            NotificationStatus.ACTIVE, NotificationRecurrenceType.DAILY);

    List<Notification> due = new ArrayList<>();
    for (Notification notification : active) {
      LocalTime dailyTime = notification.getDailyTime();
      if (dailyTime == null) continue;

      boolean timeMatches = dailyTime.getHour() == now.getHour() && dailyTime.getMinute() == now.getMinute();
      boolean alreadySentToday = notification.getLastSentAt() != null
              && notification.getLastSentAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(today);

      if (timeMatches && !alreadySentToday) {
        due.add(notification);
      }
    }

    if (!due.isEmpty()) {
      notificationService.dispatchAll(due);
    }
  }
}
