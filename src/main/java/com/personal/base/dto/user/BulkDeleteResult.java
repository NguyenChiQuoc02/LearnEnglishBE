package com.personal.base.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkDeleteResult {
  private Long id;
  private String username;
  private boolean success;
  private String error;
}
