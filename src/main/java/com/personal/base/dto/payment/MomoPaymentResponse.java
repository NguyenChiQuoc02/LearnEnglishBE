package com.personal.base.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MomoPaymentResponse {
  private String orderId;
  private String payUrl;
  private BigDecimal amount;
}
