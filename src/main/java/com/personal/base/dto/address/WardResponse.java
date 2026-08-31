package com.personal.base.dto.address;

import com.personal.base.models.Ward;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WardResponse {
  private String code;
  private String name;
  private String fullName;
  private String provinceCode;

  public static WardResponse from(Ward ward) {
    return new WardResponse(
            ward.getCode(),
            ward.getName(),
            ward.getFullName(),
            ward.getProvince() != null ? ward.getProvince().getCode() : null);
  }
}
