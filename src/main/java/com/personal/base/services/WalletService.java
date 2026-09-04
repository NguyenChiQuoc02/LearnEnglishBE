package com.personal.base.services;

import com.personal.base.dto.common.PageResponse;
import com.personal.base.dto.payment.MomoPaymentResponse;
import com.personal.base.dto.wallet.WalletResponse;
import com.personal.base.dto.wallet.WalletTransactionResponse;
import com.personal.base.dto.wallet.WithdrawalResponse;
import com.personal.base.models.type.PaymentMethod;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class WalletService {

  @Autowired
  private WalletRepository walletRepository;

  @Autowired
  private WalletTransactionRepository walletTransactionRepository;

  @Autowired
  private WithdrawalRequestRepository withdrawalRequestRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private MomoService momoService;

  @Autowired
  private EnrollmentService enrollmentService;

  @Transactional
  public Wallet getOrCreateWallet(Long userId) {
    return walletRepository.findByUserId(userId).orElseGet(() -> {
      User user = userRepository.findById(userId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
      Wallet wallet = new Wallet();
      wallet.setUser(user);
      wallet.setBalance(BigDecimal.ZERO);
      return walletRepository.save(wallet);
    });
  }

  public WalletResponse getMyWallet(Long userId) {
    return WalletResponse.from(getOrCreateWallet(userId));
  }

  public PageResponse<WalletTransactionResponse> listMyTransactions(Long userId, int page, int size) {
    Wallet wallet = getOrCreateWallet(userId);
    Page<WalletTransaction> transactions = walletTransactionRepository
            .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(page, size));
    return PageResponse.of(transactions, tx -> WalletTransactionResponse.from(tx, false));
  }

  @Transactional
  public MomoPaymentResponse createTopup(Long userId, BigDecimal amount) {
    Wallet wallet = getOrCreateWallet(userId);

    WalletTransaction transaction = new WalletTransaction();
    transaction.setWallet(wallet);
    transaction.setType(WalletTransactionType.TOPUP);
    transaction.setStatus(WalletTransactionStatus.PENDING);
    transaction.setMethod(PaymentMethod.MOMO);
    transaction.setAmount(amount);
    walletTransactionRepository.save(transaction);

    MomoService.MomoCreateResult result = momoService.createPayment(amount.longValue(), "Nạp tiền vào ví LearnEnglish");

    transaction.setMomoOrderId(result.getOrderId());
    walletTransactionRepository.save(transaction);

    return new MomoPaymentResponse(result.getOrderId(), result.getPayUrl(), amount);
  }

  @Transactional
  public WithdrawalResponse createWithdrawal(Long userId, BigDecimal amount, String phone) {
    Wallet wallet = getOrCreateWallet(userId);

    if (wallet.getBalance().compareTo(amount) < 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Số dư không đủ");
    }

    wallet.setBalance(wallet.getBalance().subtract(amount));
    walletRepository.save(wallet);

    WalletTransaction transaction = new WalletTransaction();
    transaction.setWallet(wallet);
    transaction.setType(WalletTransactionType.WITHDRAW);
    transaction.setStatus(WalletTransactionStatus.PENDING);
    transaction.setMethod(PaymentMethod.MOMO);
    transaction.setAmount(amount);
    WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);

    WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
    withdrawalRequest.setUser(wallet.getUser());
    withdrawalRequest.setAmount(amount);
    withdrawalRequest.setMomoPhoneNumber(phone);
    withdrawalRequest.setStatus(WithdrawalStatus.PENDING);
    withdrawalRequest.setWalletTransaction(savedTransaction);

    return WithdrawalResponse.from(withdrawalRequestRepository.save(withdrawalRequest));
  }

  // Idempotent: MoMo may retry the IPN, so a transaction already in a terminal
  // state (SUCCESS/FAILED) is left untouched.
  @Transactional
  public void applyMomoIpn(String orderId, String momoTransId, boolean success) {
    WalletTransaction transaction = walletTransactionRepository.findByMomoOrderId(orderId).orElse(null);
    if (transaction == null) {
      return;
    }
    if (transaction.getStatus() == WalletTransactionStatus.SUCCESS
            || transaction.getStatus() == WalletTransactionStatus.FAILED) {
      return;
    }

    transaction.setMomoTransId(momoTransId);

    if (success) {
      transaction.setStatus(WalletTransactionStatus.SUCCESS);
      walletTransactionRepository.save(transaction);

      if (transaction.getType() == WalletTransactionType.TOPUP) {
        Wallet wallet = transaction.getWallet();
        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(wallet);
      } else if (transaction.getType() == WalletTransactionType.COURSE_PAYMENT) {
        Long userId = transaction.getWallet().getUser().getId();
        Long courseId = transaction.getCourse().getId();
        enrollmentService.enrollAfterPayment(userId, courseId);
      }
    } else {
      transaction.setStatus(WalletTransactionStatus.FAILED);
      walletTransactionRepository.save(transaction);
    }
  }
}
