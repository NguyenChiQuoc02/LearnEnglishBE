package com.personal.base.models;

public enum NotificationStatus {
  // One-time scheduled notification waiting for its scheduledAt to arrive.
  PENDING,
  // Daily-recurring notification that keeps firing every day until cancelled.
  ACTIVE,
  // One-time notification (immediate or scheduled) that has been dispatched.
  SENT,
  CANCELLED
}
