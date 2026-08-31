package com.personal.base.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserImportRowResult {
  private int rowNumber;
  private String username;
  private String email;
  private String phoneNumber;
  private String dateOfBirth;
  private String address;
  private String role;
  private boolean valid;
  private String error;
}
