package com.personal.base.controllers;

import com.personal.base.dto.common.PageResponse;
import com.personal.base.dto.user.BulkDeleteRequest;
import com.personal.base.dto.user.BulkDeleteResponse;
import com.personal.base.dto.user.ChangePasswordRequest;
import com.personal.base.dto.user.TeacherResponse;
import com.personal.base.dto.user.UserImportResponse;
import com.personal.base.dto.user.UserRequest;
import com.personal.base.dto.user.UserResponse;
import com.personal.base.models.ERole;
import com.personal.base.repository.UserRepository;
import com.personal.base.services.UserDetailsImpl;
import com.personal.base.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

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
  public ResponseEntity<PageResponse<UserResponse>> listUsers(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size,
          @RequestParam(required = false) String keyword) {
    return ResponseEntity.ok(userService.listUsers(page, size, keyword));
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

  @PostMapping("/bulk-delete")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<BulkDeleteResponse> bulkDeleteUsers(@Valid @RequestBody BulkDeleteRequest request,
                                                             @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(userService.bulkDeleteUsers(request.getIds(), currentUser.getId()));
  }

  @PutMapping("/me/password")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                              @AuthenticationPrincipal UserDetailsImpl currentUser) {
    userService.changePassword(currentUser.getId(), request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/import/template")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Resource> downloadImportTemplate() {
    Resource resource = userService.getImportTemplate();
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"template_user.xlsx\"")
            .body(resource);
  }

  @PostMapping("/import/preview")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserImportResponse> previewImportUsers(@RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(userService.previewImportUsers(file));
  }

  @PostMapping("/import")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserImportResponse> importUsers(@RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(userService.commitImportUsers(file));
  }
}
