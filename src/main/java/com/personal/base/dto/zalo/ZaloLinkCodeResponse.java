package com.personal.base.dto.zalo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZaloLinkCodeResponse {
  private String code;
  private String followUrl;
  private Instant expiresAt;
}
