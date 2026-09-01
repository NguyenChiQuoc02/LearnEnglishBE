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
@Table(name = "wallet_transactions")
public class WalletTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private Wallet wallet;

  @Enumerated(EnumType.STRING)
  @Column(length = 30, nullable = false)
  private WalletTransactionType type;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  private WalletTransactionStatus status;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private PaymentMethod method;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  // Set only for COURSE_PAYMENT transactions.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id")
  private Course course;

  @Column(name = "momo_order_id", unique = true)
  private String momoOrderId;

  @Column(name = "momo_trans_id")
  private String momoTransId;

  @Column(length = 500)
  private String note;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
