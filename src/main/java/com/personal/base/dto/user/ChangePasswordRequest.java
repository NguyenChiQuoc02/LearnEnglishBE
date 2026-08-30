package com.personal.base.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
  @NotBlank
  private String oldPassword;

  @NotBlank
  @Size(min = 6, max = 40)
  private String newPassword;
}
