package com.personal.base.dto.user;

import com.personal.base.models.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
  private Long id;
  private String username;
  private String email;
  private String phoneNumber;
  private LocalDate dateOfBirth;
  private String address;
  private String avatarUrl;
  private Set<String> roles;

  public static UserResponse from(User user) {
    Set<String> roles = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());

    return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getDateOfBirth(),
            user.getAddress(),
            user.getAvatarUrl(),
            roles);
  }
}
