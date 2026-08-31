package com.personal.base.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// Single-row table (id is always 1) holding the OAuth tokens for the app's one Zalo OA.
// A personal project only ever connects one Official Account, so a full multi-tenant
// table would be unused complexity.
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "zalo_oa_credentials")
public class ZaloOaCredential {
  @Id
  private Long id;

  @Column(name = "access_token", length = 500, nullable = false)
  private String accessToken;

  @Column(name = "refresh_token", length = 500, nullable = false)
  private String refreshToken;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
