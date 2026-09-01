package com.personal.base.controllers;

import com.personal.base.dto.admin.BudgetOverviewResponse;
import com.personal.base.dto.common.PageResponse;
import com.personal.base.dto.wallet.WalletTransactionResponse;
import com.personal.base.dto.wallet.WithdrawalDecisionRequest;
import com.personal.base.dto.wallet.WithdrawalResponse;
import com.personal.base.services.AdminBudgetService;
import com.personal.base.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/budget")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBudgetController {

  @Autowired
  private AdminBudgetService adminBudgetService;

  @GetMapping("/overview")
  public ResponseEntity<BudgetOverviewResponse> getOverview() {
    return ResponseEntity.ok(adminBudgetService.getOverview());
  }

  @GetMapping("/transactions")
  public ResponseEntity<PageResponse<WalletTransactionResponse>> listTransactions(
          @RequestParam(required = false) String type,
          @RequestParam(required = false) String status,
          @RequestParam(required = false) Long userId,
          @RequestParam(required = false) Instant from,
          @RequestParam(required = false) Instant to,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(adminBudgetService.listTransactions(type, status, userId, from, to, page, size));
  }

  @GetMapping("/withdrawals")
  public ResponseEntity<PageResponse<WithdrawalResponse>> listWithdrawals(
          @RequestParam(required = false) String status,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(adminBudgetService.listWithdrawals(status, page, size));
  }

  @PostMapping("/withdrawals/{id}/approve")
  public ResponseEntity<WithdrawalResponse> approveWithdrawal(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(adminBudgetService.approveWithdrawal(id, currentUser.getId()));
  }

  @PostMapping("/withdrawals/{id}/reject")
  public ResponseEntity<WithdrawalResponse> rejectWithdrawal(@PathVariable Long id,
                                                               @RequestBody(required = false) WithdrawalDecisionRequest request,
                                                               @AuthenticationPrincipal UserDetailsImpl currentUser) {
    String note = request != null ? request.getAdminNote() : null;
    return ResponseEntity.ok(adminBudgetService.rejectWithdrawal(id, currentUser.getId(), note));
  }
}
