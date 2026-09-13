package com.personal.base.services;

import com.personal.base.dto.admin.BudgetOverviewResponse;
import com.personal.base.dto.common.PageResponse;
import com.personal.base.dto.wallet.TransferResponse;
import com.personal.base.dto.wallet.WalletTransactionResponse;
import com.personal.base.dto.wallet.WithdrawalResponse;
import com.personal.base.models.User;
import com.personal.base.models.Wallet;
import com.personal.base.models.WalletTransaction;
import com.personal.base.models.type.WalletTransactionStatus;
import com.personal.base.models.type.WalletTransactionType;
import com.personal.base.models.WithdrawalRequest;
import com.personal.base.models.type.WithdrawalStatus;
import com.personal.base.repository.UserRepository;
import com.personal.base.repository.WalletRepository;
import com.personal.base.repository.WalletTransactionRepository;
import com.personal.base.repository.WithdrawalRequestRepository;
import com.personal.base.specification.WalletTransactionSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AdminBudgetService {

  @Autowired
  private WalletRepository walletRepository;

  @Autowired
  private WalletTransactionRepository walletTransactionRepository;

  @Autowired
  private WithdrawalRequestRepository withdrawalRequestRepository;

  @Autowired
  private UserRepository userRepository;

  public BudgetOverviewResponse getOverview() {
    return new BudgetOverviewResponse(
            walletTransactionRepository.sumAmountByTypeAndStatus(WalletTransactionType.COURSE_PAYMENT, WalletTransactionStatus.SUCCESS),
            walletRepository.sumAllBalances(),
            withdrawalRequestRepository.countByStatus(WithdrawalStatus.PENDING),
            withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PENDING),
            walletTransactionRepository.sumAmountByTypeAndStatus(WalletTransactionType.TOPUP, WalletTransactionStatus.SUCCESS),
            walletTransactionRepository.sumAmountByTypeAndStatus(WalletTransactionType.WITHDRAW, WalletTransactionStatus.SUCCESS));
  }

  public PageResponse<WalletTransactionResponse> listTransactions(String type, String status, Long userId,
                                                                    Instant from, Instant to, int page, int size) {
    WalletTransactionType typeEnum = parseEnum(WalletTransactionType.class, type);
    WalletTransactionStatus statusEnum = parseEnum(WalletTransactionStatus.class, status);

    Specification<WalletTransaction> spec = Specification
            .where(WalletTransactionSpecification.hasType(typeEnum))
            .and(WalletTransactionSpecification.hasStatus(statusEnum))
            .and(WalletTransactionSpecification.hasUserId(userId))
            .and(WalletTransactionSpecification.createdBetween(from, to));

    Page<WalletTransaction> transactions = walletTransactionRepository.findAll(spec, PageRequest.of(page, size));
    return PageResponse.of(transactions, tx -> WalletTransactionResponse.from(tx, true));
  }

  public PageResponse<WithdrawalResponse> listWithdrawals(String status, int page, int size) {
    WithdrawalStatus statusEnum = parseEnum(WithdrawalStatus.class, status);
    Page<WithdrawalRequest> withdrawals = statusEnum != null
            ? withdrawalRequestRepository.findByStatus(statusEnum, PageRequest.of(page, size))
            : withdrawalRequestRepository.findAll(PageRequest.of(page, size));
    return PageResponse.of(withdrawals, WithdrawalResponse::from);
  }

  @Transactional
  public WithdrawalResponse approveWithdrawal(Long id, Long adminId) {
    WithdrawalRequest request = withdrawalRequestRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdrawal request not found"));
    if (request.getStatus() != WithdrawalStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Withdrawal request has already been processed");
    }

    User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

    WalletTransaction transaction = request.getWalletTransaction();
    transaction.setStatus(WalletTransactionStatus.SUCCESS);
    walletTransactionRepository.save(transaction);

    request.setStatus(WithdrawalStatus.APPROVED);
    request.setProcessedAt(Instant.now());
    request.setProcessedBy(admin);

    return WithdrawalResponse.from(withdrawalRequestRepository.save(request));
  }

  @Transactional
  public WithdrawalResponse rejectWithdrawal(Long id, Long adminId, String note) {
    WithdrawalRequest request = withdrawalRequestRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdrawal request not found"));
    if (request.getStatus() != WithdrawalStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Withdrawal request has already been processed");
    }

    User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

    WalletTransaction transaction = request.getWalletTransaction();
    transaction.getWallet().setBalance(transaction.getWallet().getBalance().add(transaction.getAmount()));
    transaction.setStatus(WalletTransactionStatus.FAILED);
    walletTransactionRepository.save(transaction);

    request.setStatus(WithdrawalStatus.REJECTED);
    request.setAdminNote(note);
    request.setProcessedAt(Instant.now());
    request.setProcessedBy(admin);

    return WithdrawalResponse.from(withdrawalRequestRepository.save(request));
  }

  // Admins share a single wallet as the "budget" source: the amount is debited from the
  // acting admin's own wallet and credited to the target user's wallet, both recorded as
  // TRANSFER transactions so they show up in the budget transaction history.
  @Transactional
  public TransferResponse transferToUser(Long adminId, Long targetUserId, BigDecimal amount, String note) {
    if (adminId.equals(targetUserId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chuyển tiền cho chính mình");
    }

    Wallet adminWallet = walletRepository.findByUserId(adminId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
    if (adminWallet.getBalance().compareTo(amount) < 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Số dư ví admin không đủ");
    }

    User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    Wallet targetWallet = walletRepository.findByUserId(targetUserId).orElseGet(() -> {
      Wallet wallet = new Wallet();
      wallet.setUser(targetUser);
      wallet.setBalance(BigDecimal.ZERO);
      return walletRepository.save(wallet);
    });

    adminWallet.setBalance(adminWallet.getBalance().subtract(amount));
    walletRepository.save(adminWallet);

    WalletTransaction debit = new WalletTransaction();
    debit.setWallet(adminWallet);
    debit.setType(WalletTransactionType.TRANSFER);
    debit.setStatus(WalletTransactionStatus.SUCCESS);
    debit.setAmount(amount);
    debit.setNote(note != null && !note.isBlank() ? note : "Chuyển tiền cho " + targetUser.getUsername());
    walletTransactionRepository.save(debit);

    targetWallet.setBalance(targetWallet.getBalance().add(amount));
    walletRepository.save(targetWallet);

    WalletTransaction credit = new WalletTransaction();
    credit.setWallet(targetWallet);
    credit.setType(WalletTransactionType.TRANSFER);
    credit.setStatus(WalletTransactionStatus.SUCCESS);
    credit.setAmount(amount);
    credit.setNote(note != null && !note.isBlank() ? note : "Nhận tiền từ admin");
    WalletTransaction savedCredit = walletTransactionRepository.save(credit);

    return new TransferResponse(
            savedCredit.getId(),
            targetUser.getId(),
            targetUser.getUsername(),
            amount,
            savedCredit.getNote(),
            adminWallet.getBalance(),
            savedCredit.getCreatedAt());
  }

  private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return Enum.valueOf(enumClass, value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value: " + value);
    }
  }
}
