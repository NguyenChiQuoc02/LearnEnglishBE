package com.personal.base.dto.user;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {
  private String phoneNumber;

  private LocalDate dateOfBirth;

  private String address;

  private String avatarUrl;

  private String provinceCode;

  private String wardCode;
}
