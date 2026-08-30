package com.personal.base.controllers;

import com.personal.base.dto.user.ChangePasswordRequest;
import com.personal.base.dto.user.TeacherResponse;
import com.personal.base.dto.user.UserRequest;
import com.personal.base.dto.user.UserResponse;
import com.personal.base.models.ERole;
import com.personal.base.repository.UserRepository;
import com.personal.base.services.UserDetailsImpl;
import com.personal.base.services.UserService;
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
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserService userService;

  @GetMapping("/teachers")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TeacherResponse>> listTeachers() {
    List<TeacherResponse> teachers = userRepository.findByRoles_Name(ERole.ROLE_TEACHER).stream()
            .map(TeacherResponse::from)
            .collect(Collectors.toList());
    return ResponseEntity.ok(teachers);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResponse>> listUsers() {
    return ResponseEntity.ok(userService.listUsers());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUser(id));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
    return ResponseEntity.ok(userService.createUser(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
    return ResponseEntity.ok(userService.updateUser(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetailsImpl currentUser) {
    userService.deleteUser(id, currentUser.getId());
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/me/password")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                              @AuthenticationPrincipal UserDetailsImpl currentUser) {
    userService.changePassword(currentUser.getId(), request);
    return ResponseEntity.noContent().build();
  }
}
