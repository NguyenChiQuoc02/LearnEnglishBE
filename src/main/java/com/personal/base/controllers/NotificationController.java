package com.personal.base.controllers;

import com.personal.base.dto.notification.NotificationRequest;
import com.personal.base.dto.notification.NotificationResponse;
import com.personal.base.services.NotificationService;
import com.personal.base.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {

  @Autowired
  private NotificationService notificationService;

  @PostMapping
  public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request,
                                                                   @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(notificationService.createNotification(request, currentUser.getId()));
  }

  @GetMapping
  public ResponseEntity<List<NotificationResponse>> listNotifications() {
    return ResponseEntity.ok(notificationService.listNotifications());
  }

  @GetMapping("/{id}")
  public ResponseEntity<NotificationResponse> getNotification(@PathVariable Long id) {
    return ResponseEntity.ok(notificationService.getNotification(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<NotificationResponse> updateNotification(@PathVariable Long id,
                                                                   @Valid @RequestBody NotificationRequest request) {
    return ResponseEntity.ok(notificationService.updateNotification(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
    notificationService.deleteNotification(id);
    return ResponseEntity.noContent().build();
  }
}
