package com.personal.base.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {
  private Long transactionId;
  private Long targetUserId;
  private String targetUsername;
  private BigDecimal amount;
  private String note;
  private BigDecimal adminRemainingBalance;
  private Instant createdAt;
}
