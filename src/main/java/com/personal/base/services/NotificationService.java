package com.personal.base.services;

import com.personal.base.dto.notification.NotificationRequest;
import com.personal.base.dto.notification.NotificationResponse;
import com.personal.base.models.Course;
import com.personal.base.models.ERole;
import com.personal.base.models.Enrollment;
import com.personal.base.models.Notification;
import com.personal.base.models.NotificationDeliveryType;
import com.personal.base.models.NotificationRecurrenceType;
import com.personal.base.models.NotificationStatus;
import com.personal.base.models.NotificationTargetType;
import com.personal.base.models.User;
import com.personal.base.repository.CourseRepository;
import com.personal.base.repository.EnrollmentRepository;
import com.personal.base.repository.NotificationRepository;
import com.personal.base.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EnrollmentRepository enrollmentRepository;

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private EmailService emailService;

  @Transactional
  public NotificationResponse createNotification(NotificationRequest request, Long adminId) {
    User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

    Notification notification = new Notification();
    notification.setCreatedBy(admin);
    applyRequestFields(notification, request);

    notification = notificationRepository.save(notification);
    if (notification.getDeliveryType() == NotificationDeliveryType.IMMEDIATE) {
      dispatch(notification);
    }

    return NotificationResponse.from(notification);
  }

  @Transactional
  public NotificationResponse updateNotification(Long id, NotificationRequest request) {
    Notification notification = getNotificationEntity(id);
    if (notification.getStatus() == NotificationStatus.SENT) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit a notification that has already been sent");
    }

    applyRequestFields(notification, request);
    notification = notificationRepository.save(notification);
    if (notification.getDeliveryType() == NotificationDeliveryType.IMMEDIATE) {
      dispatch(notification);
    }

    return NotificationResponse.from(notification);
  }

  @Transactional(readOnly = true)
  public NotificationResponse getNotification(Long id) {
    return NotificationResponse.from(getNotificationEntity(id));
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> listNotifications() {
    return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(NotificationResponse::from)
            .collect(Collectors.toList());
  }

  @Transactional
  public void deleteNotification(Long id) {
    notificationRepository.delete(getNotificationEntity(id));
  }

  private Notification getNotificationEntity(Long id) {
    return notificationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
  }

  // Shared by create and update: validates the request against the notification's
  // delivery/target rules and writes every field, resetting whichever schedule/target
  // fields no longer apply so a prior edit can't leave stale values behind.
  private void applyRequestFields(Notification notification, NotificationRequest request) {
    notification.setTitle(request.getTitle());
    notification.setContent(request.getContent());
    notification.setImageUrl(request.getImageUrl());
    notification.setLink(request.getLink());
    notification.setTargetType(request.getTargetType());
    notification.setDeliveryType(request.getDeliveryType());

    if (request.getTargetType() == NotificationTargetType.COURSE) {
      if (request.getTargetCourseId() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetCourseId is required when targetType is COURSE");
      }
      Course course = courseRepository.findById(request.getTargetCourseId())
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course not found"));
      notification.setTargetCourse(course);
    } else {
      notification.setTargetCourse(null);
    }

    if (request.getDeliveryType() == NotificationDeliveryType.SCHEDULED) {
      NotificationRecurrenceType recurrenceType = request.getRecurrenceType() != null
              ? request.getRecurrenceType() : NotificationRecurrenceType.NONE;
      notification.setRecurrenceType(recurrenceType);

      if (recurrenceType == NotificationRecurrenceType.DAILY) {
        if (request.getDailyTime() == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dailyTime is required when recurrenceType is DAILY");
        }
        notification.setDailyTime(request.getDailyTime());
        notification.setScheduledAt(null);
        notification.setStatus(NotificationStatus.ACTIVE);
      } else {
        if (request.getScheduledAt() == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduledAt is required for a one-time scheduled notification");
        }
        if (request.getScheduledAt().isBefore(Instant.now())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduledAt must be in the future");
        }
        notification.setScheduledAt(request.getScheduledAt());
        notification.setDailyTime(null);
        notification.setStatus(NotificationStatus.PENDING);
      }
    } else {
      notification.setRecurrenceType(NotificationRecurrenceType.NONE);
      notification.setScheduledAt(null);
      notification.setDailyTime(null);
      notification.setStatus(NotificationStatus.PENDING);
    }
  }

  // Resolves recipients and fires off the (async) emails. Called either right away for
  // IMMEDIATE notifications, or by NotificationScheduler once a SCHEDULED one is due.
  @Transactional
  public void dispatch(Notification notification) {
    dispatchAll(List.of(notification));
  }

  // Same as dispatch(), but for a batch: saves all notifications in a single saveAll()
  // instead of one repository round-trip per notification.
  @Transactional
  public void dispatchAll(List<Notification> notifications) {
    for (Notification notification : notifications) {
      List<User> recipients = resolveRecipients(notification);
      for (User recipient : recipients) {
        if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
          emailService.sendNotificationEmail(recipient.getEmail(), notification.getTitle(),
                  notification.getContent(), notification.getImageUrl(), notification.getLink());
        }
      }

      notification.setLastSentAt(Instant.now());
      if (notification.getRecurrenceType() == NotificationRecurrenceType.NONE) {
        notification.setStatus(NotificationStatus.SENT);
      }
    }
    notificationRepository.saveAll(notifications);
  }

  private List<User> resolveRecipients(Notification notification) {
    switch (notification.getTargetType()) {
      case ALL:
        return userRepository.findAll();
      case TEACHERS:
        return userRepository.findByRoles_Name(ERole.ROLE_TEACHER);
      case STUDENTS:
        return userRepository.findByRoles_Name(ERole.ROLE_USER);
      case COURSE:
        return enrollmentRepository.findByCourseIdOrderByEnrolledAtDesc(notification.getTargetCourse().getId())
                .stream()
                .map(Enrollment::getUser)
                .collect(Collectors.toList());
      default:
        return List.of();
    }
  }
}
