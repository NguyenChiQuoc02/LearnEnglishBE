package com.personal.base.dto.wallet;

import com.personal.base.models.WithdrawalRequest;
import com.personal.base.models.type.WithdrawalStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawalResponse {
  private Long id;
  private Long userId;
  private String username;
  private BigDecimal amount;
  private String momoPhoneNumber;
  private WithdrawalStatus status;
  private String adminNote;
  private Instant requestedAt;
  private Instant processedAt;

  public static WithdrawalResponse from(WithdrawalRequest request) {
    return new WithdrawalResponse(
            request.getId(),
            request.getUser().getId(),
            request.getUser().getUsername(),
            request.getAmount(),
            request.getMomoPhoneNumber(),
            request.getStatus(),
            request.getAdminNote(),
            request.getRequestedAt(),
            request.getProcessedAt());
  }
}
