package com.personal.base.controllers;

import com.personal.base.dto.enrollment.EnrollRequest;
import com.personal.base.dto.enrollment.EnrollmentResponse;
import com.personal.base.services.EnrollmentService;
import com.personal.base.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

  @Autowired
  private EnrollmentService enrollmentService;

  @PostMapping
  public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody EnrollRequest request,
                                                    @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(enrollmentService.enroll(currentUser.getId(), request.getCourseId()));
  }

  @GetMapping("/me")
  public ResponseEntity<List<EnrollmentResponse>> myEnrollments(@AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(enrollmentService.listMyEnrollments(currentUser.getId()));
  }
}
