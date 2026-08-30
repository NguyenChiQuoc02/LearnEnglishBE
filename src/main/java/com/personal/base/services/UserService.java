package com.personal.base.services;

import com.personal.base.dto.user.ChangePasswordRequest;
import com.personal.base.dto.user.UserRequest;
import com.personal.base.dto.user.UserResponse;
import com.personal.base.models.ERole;
import com.personal.base.models.Role;
import com.personal.base.models.User;
import com.personal.base.repository.RoleRepository;
import com.personal.base.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

  public static final String DEFAULT_PASSWORD = "123456";

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PasswordEncoder encoder;

  public List<UserResponse> listUsers() {
    return userRepository.findAll().stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());
  }

  public UserResponse getUser(Long id) {
    return UserResponse.from(getUserEntity(id));
  }

  @Transactional
  public UserResponse createUser(UserRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use");
    }

    User user = new User(request.getUsername(), request.getEmail(), encoder.encode(DEFAULT_PASSWORD));
    user.setPhoneNumber(request.getPhoneNumber());
    user.setDateOfBirth(request.getDateOfBirth());
    user.setAddress(request.getAddress());
    user.setAvatarUrl(request.getAvatarUrl());
    user.setRoles(resolveRoles(request.getRoles()));

    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public UserResponse updateUser(Long id, UserRequest request) {
    User user = getUserEntity(id);

    if (userRepository.existsByUsernameAndIdNot(request.getUsername(), id)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
    }
    if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use");
    }

    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPhoneNumber(request.getPhoneNumber());
    user.setDateOfBirth(request.getDateOfBirth());
    user.setAddress(request.getAddress());
    user.setAvatarUrl(request.getAvatarUrl());
    user.setRoles(resolveRoles(request.getRoles()));

    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public void deleteUser(Long id, Long currentUserId) {
    if (id.equals(currentUserId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
    }
    User user = getUserEntity(id);
    try {
      userRepository.delete(user);
      userRepository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a user that owns existing data");
    }
  }

  @Transactional
  public void changePassword(Long userId, ChangePasswordRequest request) {
    User user = getUserEntity(userId);
    if (!encoder.matches(request.getOldPassword(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
    }
    user.setPassword(encoder.encode(request.getNewPassword()));
    userRepository.save(user);
  }

  private User getUserEntity(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  private Set<Role> resolveRoles(Set<String> roleNames) {
    Set<Role> roles = new HashSet<>();

    if (roleNames == null || roleNames.isEmpty()) {
      roles.add(findRole(ERole.ROLE_USER));
      return roles;
    }

    for (String roleName : roleNames) {
      roles.add(findRole(parseRole(roleName)));
    }
    return roles;
  }

  private ERole parseRole(String roleName) {
    try {
      return ERole.valueOf(roleName.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName);
    }
  }

  private Role findRole(ERole roleName) {
    return roleRepository.findByName(roleName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role is not found"));
  }
}
