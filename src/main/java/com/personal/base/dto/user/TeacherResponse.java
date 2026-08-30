package com.personal.base.dto.user;

import com.personal.base.models.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherResponse {
  private Long id;
  private String username;
  private String email;

  public static TeacherResponse from(User user) {
    return new TeacherResponse(user.getId(), user.getUsername(), user.getEmail());
  }
}
