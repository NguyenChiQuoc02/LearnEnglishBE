package com.personal.base.dto.address;

import com.personal.base.models.Province;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProvinceResponse {
  private String code;
  private String name;
  private String fullName;

  public static ProvinceResponse from(Province province) {
    return new ProvinceResponse(province.getCode(), province.getName(), province.getFullName());
  }
}
