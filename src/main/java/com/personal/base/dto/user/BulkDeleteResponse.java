package com.personal.base.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkDeleteResponse {
  private List<BulkDeleteResult> results;
  private int successCount;
  private int failureCount;

  public static BulkDeleteResponse of(List<BulkDeleteResult> results) {
    int success = (int) results.stream().filter(BulkDeleteResult::isSuccess).count();
    return new BulkDeleteResponse(results, success, results.size() - success);
  }
}
