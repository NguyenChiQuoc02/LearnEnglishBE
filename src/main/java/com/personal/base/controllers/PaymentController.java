package com.personal.base.controllers;

import com.personal.base.dto.enrollment.EnrollmentResponse;
import com.personal.base.dto.payment.MomoPaymentResponse;
import com.personal.base.dto.wallet.WalletTransactionResponse;
import com.personal.base.services.MomoService;
import com.personal.base.services.PaymentService;
import com.personal.base.services.UserDetailsImpl;
import com.personal.base.services.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private WalletService walletService;

  @Autowired
  private MomoService momoService;

  @PostMapping("/courses/{id}/wallet")
  public ResponseEntity<EnrollmentResponse> payCourseWithWallet(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(paymentService.payCourseWithWallet(currentUser.getId(), id));
  }

  @PostMapping("/courses/{id}/momo")
  public ResponseEntity<MomoPaymentResponse> payCourseWithMomo(@PathVariable Long id,
                                                                 @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(paymentService.payCourseWithMomo(currentUser.getId(), id));
  }

  @GetMapping("/momo/status/{orderId}")
  public ResponseEntity<WalletTransactionResponse> getMomoStatus(@PathVariable String orderId,
                                                                    @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(paymentService.getStatus(currentUser.getId(), orderId));
  }

  // MoMo posts JSON to this endpoint after a payment completes. Must be whitelisted in
  // WebSecurityConfig and must respond quickly with 2xx, or MoMo will keep retrying.
  @PostMapping("/momo/ipn")
  public ResponseEntity<Void> momoIpn(@RequestBody Map<String, Object> payload) {
    try {
      Map<String, String> params = new java.util.HashMap<>();
      payload.forEach((key, value) -> params.put(key, value == null ? null : String.valueOf(value)));

      if (!momoService.verifyIpnSignature(params)) {
        log.warn("Rejected MoMo IPN with invalid signature: {}", payload);
        return ResponseEntity.status(400).build();
      }

      String orderId = params.get("orderId");
      String transId = params.get("transId");
      int resultCode = params.get("resultCode") != null ? Integer.parseInt(params.get("resultCode")) : -1;

      walletService.applyMomoIpn(orderId, transId, resultCode == 0);
    } catch (Exception e) {
      log.warn("Failed to process MoMo IPN: {}", payload, e);
    }
    return ResponseEntity.noContent().build();
  }
}
