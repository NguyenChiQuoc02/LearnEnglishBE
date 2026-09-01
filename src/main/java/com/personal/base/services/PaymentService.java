package com.personal.base.services;

import com.personal.base.dto.enrollment.EnrollmentResponse;
import com.personal.base.dto.payment.MomoPaymentResponse;
import com.personal.base.dto.wallet.WalletTransactionResponse;
import com.personal.base.models.Course;
import com.personal.base.models.PaymentMethod;
import com.personal.base.models.Wallet;
import com.personal.base.models.WalletTransaction;
import com.personal.base.models.WalletTransactionStatus;
import com.personal.base.models.WalletTransactionType;
import com.personal.base.repository.CourseRepository;
import com.personal.base.repository.WalletRepository;
import com.personal.base.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class PaymentService {

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private WalletRepository walletRepository;

  @Autowired
  private WalletTransactionRepository walletTransactionRepository;

  @Autowired
  private WalletService walletService;

  @Autowired
  private MomoService momoService;

  @Autowired
  private EnrollmentService enrollmentService;

  @Transactional
  public EnrollmentResponse payCourseWithWallet(Long userId, Long courseId) {
    Course course = getPayableCourse(courseId);

    Wallet wallet = walletService.getOrCreateWallet(userId);
    if (wallet.getBalance().compareTo(course.getPrice()) < 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Số dư không đủ");
    }

    wallet.setBalance(wallet.getBalance().subtract(course.getPrice()));
    walletRepository.save(wallet);

    WalletTransaction transaction = new WalletTransaction();
    transaction.setWallet(wallet);
    transaction.setType(WalletTransactionType.COURSE_PAYMENT);
    transaction.setStatus(WalletTransactionStatus.SUCCESS);
    transaction.setMethod(PaymentMethod.WALLET);
    transaction.setAmount(course.getPrice());
    transaction.setCourse(course);
    walletTransactionRepository.save(transaction);

    return enrollmentService.enrollAfterPayment(userId, courseId);
  }

  @Transactional
  public MomoPaymentResponse payCourseWithMomo(Long userId, Long courseId) {
    Course course = getPayableCourse(courseId);

    // Wallet still needed as the FK owner even though this is a MoMo payment.
    Wallet wallet = walletService.getOrCreateWallet(userId);

    WalletTransaction transaction = new WalletTransaction();
    transaction.setWallet(wallet);
    transaction.setType(WalletTransactionType.COURSE_PAYMENT);
    transaction.setStatus(WalletTransactionStatus.PENDING);
    transaction.setMethod(PaymentMethod.MOMO);
    transaction.setAmount(course.getPrice());
    transaction.setCourse(course);
    walletTransactionRepository.save(transaction);

    MomoService.MomoCreateResult result = momoService.createPayment(
            course.getPrice().longValue(), "Thanh toán khóa học " + course.getTitle());

    transaction.setMomoOrderId(result.getOrderId());
    walletTransactionRepository.save(transaction);

    return new MomoPaymentResponse(result.getOrderId(), result.getPayUrl(), course.getPrice());
  }

  public WalletTransactionResponse getStatus(Long userId, String orderId) {
    WalletTransaction transaction = walletTransactionRepository.findByMomoOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

    if (!transaction.getWallet().getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this transaction");
    }

    return WalletTransactionResponse.from(transaction, false);
  }

  private Course getPayableCourse(Long courseId) {
    Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course is free");
    }
    return course;
  }
}
