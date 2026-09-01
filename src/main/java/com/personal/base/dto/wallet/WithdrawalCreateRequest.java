package com.personal.base.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawalCreateRequest {
  @NotNull
  @DecimalMin("1")
  private BigDecimal amount;

  @NotBlank
  private String momoPhoneNumber;
}
