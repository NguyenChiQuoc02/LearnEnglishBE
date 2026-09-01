package com.personal.base.dto.wallet;

import com.personal.base.models.Wallet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletResponse {
  private Long id;
  private Long userId;
  private BigDecimal balance;
  private Instant updatedAt;

  public static WalletResponse from(Wallet wallet) {
    return new WalletResponse(
            wallet.getId(),
            wallet.getUser().getId(),
            wallet.getBalance(),
            wallet.getUpdatedAt());
  }
}
