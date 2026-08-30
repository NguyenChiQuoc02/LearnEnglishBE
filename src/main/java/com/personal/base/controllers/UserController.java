package com.personal.base.controllers;

import com.personal.base.dto.user.TeacherResponse;
import com.personal.base.models.ERole;
import com.personal.base.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired
  private UserRepository userRepository;

  @GetMapping("/teachers")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TeacherResponse>> listTeachers() {
    List<TeacherResponse> teachers = userRepository.findByRoles_Name(ERole.ROLE_TEACHER).stream()
            .map(TeacherResponse::from)
            .collect(Collectors.toList());
    return ResponseEntity.ok(teachers);
  }
}
