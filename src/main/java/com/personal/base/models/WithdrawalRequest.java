package com.personal.base.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "withdrawal_requests")
public class WithdrawalRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "momo_phone_number", nullable = false, length = 20)
  private String momoPhoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  private WithdrawalStatus status = WithdrawalStatus.PENDING;

  @Column(name = "admin_note", length = 500)
  private String adminNote;

  // The debit (WITHDRAW) transaction created at request time.
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_transaction_id")
  private WalletTransaction walletTransaction;

  @Column(name = "requested_at", nullable = false, updatable = false)
  private Instant requestedAt = Instant.now();

  @Column(name = "processed_at")
  private Instant processedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_by")
  private User processedBy;
}
