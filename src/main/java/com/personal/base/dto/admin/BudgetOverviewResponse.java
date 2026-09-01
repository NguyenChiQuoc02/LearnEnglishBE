package com.personal.base.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetOverviewResponse {
  private BigDecimal totalRevenue;
  private BigDecimal totalWalletBalance;
  private long pendingWithdrawalsCount;
  private BigDecimal pendingWithdrawalsAmount;
  private BigDecimal totalTopupAmount;
  private BigDecimal totalWithdrawnAmount;
}
