package com.personal.base.controllers;

import com.personal.base.dto.common.PageResponse;
import com.personal.base.dto.payment.MomoPaymentResponse;
import com.personal.base.dto.wallet.TopupRequest;
import com.personal.base.dto.wallet.WalletResponse;
import com.personal.base.dto.wallet.WalletTransactionResponse;
import com.personal.base.dto.wallet.WithdrawalCreateRequest;
import com.personal.base.dto.wallet.WithdrawalResponse;
import com.personal.base.services.UserDetailsImpl;
import com.personal.base.services.WalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

  @Autowired
  private WalletService walletService;

  @GetMapping("/me")
  public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(walletService.getMyWallet(currentUser.getId()));
  }

  @GetMapping("/me/transactions")
  public ResponseEntity<PageResponse<WalletTransactionResponse>> listMyTransactions(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size,
          @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(walletService.listMyTransactions(currentUser.getId(), page, size));
  }

  @PostMapping("/topup")
  public ResponseEntity<MomoPaymentResponse> topup(@Valid @RequestBody TopupRequest request,
                                                     @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(walletService.createTopup(currentUser.getId(), request.getAmount()));
  }

  @PostMapping("/withdraw")
  public ResponseEntity<WithdrawalResponse> withdraw(@Valid @RequestBody WithdrawalCreateRequest request,
                                                       @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(walletService.createWithdrawal(currentUser.getId(), request.getAmount(), request.getMomoPhoneNumber()));
  }
}
