package com.personal.base.models;

public enum NotificationRecurrenceType {
  // One-time — either sent immediately or at a specific scheduledAt datetime.
  NONE,
  // Repeats every day at dailyTime (e.g. 19:00 study reminder).
  DAILY
}
