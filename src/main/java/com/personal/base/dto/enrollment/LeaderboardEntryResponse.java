package com.personal.base.dto.enrollment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardEntryResponse {
  private int rank;
  private Long userId;
  private String username;
  private Integer totalScore;
}
