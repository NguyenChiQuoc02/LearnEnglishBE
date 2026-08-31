package com.personal.base.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserImportResponse {
  private List<UserImportRowResult> rows;
  private int successCount;
  private int failureCount;

  public static UserImportResponse of(List<UserImportRowResult> rows) {
    int success = (int) rows.stream().filter(UserImportRowResult::isValid).count();
    return new UserImportResponse(rows, success, rows.size() - success);
  }
}
