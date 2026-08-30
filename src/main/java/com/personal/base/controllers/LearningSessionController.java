package com.personal.base.controllers;

import com.personal.base.dto.session.CompleteSessionResponse;
import com.personal.base.dto.session.StartSessionRequest;
import com.personal.base.dto.session.StartSessionResponse;
import com.personal.base.dto.session.SubmitAnswerRequest;
import com.personal.base.dto.session.SubmitAnswerResponse;
import com.personal.base.services.LearningSessionService;
import com.personal.base.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/learning-sessions")
public class LearningSessionController {

  @Autowired
  private LearningSessionService learningSessionService;

  @PostMapping("/start")
  public ResponseEntity<StartSessionResponse> start(@Valid @RequestBody StartSessionRequest request,
                                                      @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(learningSessionService.startSession(currentUser.getId(), request.getCourseId()));
  }

  @PostMapping("/{sessionId}/answer")
  public ResponseEntity<SubmitAnswerResponse> answer(@PathVariable Long sessionId,
                                                      @Valid @RequestBody SubmitAnswerRequest request,
                                                      @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(learningSessionService.submitAnswer(currentUser.getId(), sessionId, request));
  }

  @PostMapping("/{sessionId}/complete")
  public ResponseEntity<CompleteSessionResponse> complete(@PathVariable Long sessionId,
                                                           @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(learningSessionService.completeSession(currentUser.getId(), sessionId));
  }
}
