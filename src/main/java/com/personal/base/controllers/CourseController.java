package com.personal.base.controllers;

import com.personal.base.dto.common.PageResponse;
import com.personal.base.dto.course.CourseRequest;
import com.personal.base.dto.course.CourseResponse;
import com.personal.base.dto.course.CourseStudentResponse;
import com.personal.base.dto.course.VocabularyItemRequest;
import com.personal.base.dto.course.VocabularyItemResponse;
import com.personal.base.dto.enrollment.LeaderboardEntryResponse;
import com.personal.base.dto.session.LearningSessionSummaryResponse;
import com.personal.base.models.CourseType;
import com.personal.base.services.CourseService;
import com.personal.base.services.EnrollmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/courses")
public class CourseController {

  @Autowired
  private CourseService courseService;

  @Autowired
  private EnrollmentService enrollmentService;

  @GetMapping
  public ResponseEntity<List<CourseResponse>> listCourses(@RequestParam(required = false) CourseType type) {
    return ResponseEntity.ok(courseService.listCourses(type));
  }

  @GetMapping("/{id}")
  public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id) {
    return ResponseEntity.ok(courseService.getCourse(id));
  }

  @GetMapping("/manage")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ResponseEntity<PageResponse<CourseResponse>> listManagedCourses(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size,
          @RequestParam(required = false) String keyword,
          @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(courseService.listManagedCourses(page, size, keyword, currentUser));
  }

  @GetMapping("/{id}/leaderboard")
  public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(@PathVariable Long id) {
    return ResponseEntity.ok(enrollmentService.getLeaderboard(id));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request,
                                                      @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(courseService.createCourse(request, currentUser.getId()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ResponseEntity<CourseResponse> updateCourse(@PathVariable Long id,
                                                      @Valid @RequestBody CourseRequest request,
                                                      @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(courseService.updateCourse(id, request, currentUser));
  }

  @PostMapping("/{id}/vocabulary")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ResponseEntity<VocabularyItemResponse> addVocabularyItem(@PathVariable Long id,
                                                                   @Valid @RequestBody VocabularyItemRequest request,
                                                                   @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(courseService.addVocabularyItem(id, request, currentUser));
  }

  @GetMapping("/{id}/vocabulary")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ResponseEntity<List<VocabularyItemResponse>> listVocabularyItems(@PathVariable Long id,
                                                                           @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(courseService.listVocabularyItems(id, currentUser));
  }

  @PutMapping("/{id}/vocabulary/{itemId}")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ResponseEntity<VocabularyItemResponse> updateVocabularyItem(@PathVariable Long id,
                                                                      @PathVariable Long itemId,
                                                                      @Valid @RequestBody VocabularyItemRequest request,
                                                                      @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(courseService.updateVocabularyItem(id, itemId, request, currentUser));
  }

  @DeleteMapping("/{id}/vocabulary/{itemId}")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ResponseEntity<Void> deleteVocabularyItem(@PathVariable Long id,
                                                    @PathVariable Long itemId,
                                                    @AuthenticationPrincipal UserDetailsImpl currentUser) {
    courseService.deleteVocabularyItem(id, itemId, currentUser);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/students")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ResponseEntity<List<CourseStudentResponse>> listStudents(@PathVariable Long id,
                                                                   @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(courseService.listStudents(id, currentUser));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
    courseService.deleteCourse(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/students/{userId}/sessions")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ResponseEntity<List<LearningSessionSummaryResponse>> listStudentSessions(
          @PathVariable Long id,
          @PathVariable Long userId,
          @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(courseService.listStudentSessions(id, userId, currentUser));
  }
}
