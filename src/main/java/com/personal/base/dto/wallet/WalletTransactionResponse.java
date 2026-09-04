package com.personal.base.dto.wallet;

import com.personal.base.models.type.PaymentMethod;
import com.personal.base.models.WalletTransaction;
import com.personal.base.models.type.WalletTransactionStatus;
import com.personal.base.models.type.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletTransactionResponse {
  private Long id;
  private WalletTransactionType type;
  private WalletTransactionStatus status;
  private PaymentMethod method;
  private BigDecimal amount;
  private Long courseId;
  private String courseTitle;
  private String momoOrderId;
  private String note;
  private Instant createdAt;
  private String username;
  private String userEmail;

  public static WalletTransactionResponse from(WalletTransaction tx) {
    return from(tx, false);
  }

  public static WalletTransactionResponse from(WalletTransaction tx, boolean includeUser) {
    return new WalletTransactionResponse(
            tx.getId(),
            tx.getType(),
            tx.getStatus(),
            tx.getMethod(),
            tx.getAmount(),
            tx.getCourse() != null ? tx.getCourse().getId() : null,
            tx.getCourse() != null ? tx.getCourse().getTitle() : null,
            tx.getMomoOrderId(),
            tx.getNote(),
            tx.getCreatedAt(),
            includeUser && tx.getWallet() != null && tx.getWallet().getUser() != null ? tx.getWallet().getUser().getUsername() : null,
            includeUser && tx.getWallet() != null && tx.getWallet().getUser() != null ? tx.getWallet().getUser().getEmail() : null);
  }
}
