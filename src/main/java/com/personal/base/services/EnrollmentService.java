package com.personal.base.services;

import com.personal.base.dto.enrollment.EnrollmentResponse;
import com.personal.base.dto.enrollment.LeaderboardEntryResponse;
import com.personal.base.models.Course;
import com.personal.base.models.Enrollment;
import com.personal.base.models.EnrollmentStatus;
import com.personal.base.models.User;
import com.personal.base.repository.CourseRepository;
import com.personal.base.repository.EnrollmentRepository;
import com.personal.base.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class EnrollmentService {

  @Autowired
  private EnrollmentRepository enrollmentRepository;

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ZaloOaService zaloOaService;

  @Autowired
  private DiscordNotificationService discordNotificationService;

  // Free-enroll entry point: blocks straight away if the course requires payment,
  // unless the caller is already enrolled (idempotent short-circuit).
  @Transactional
  public EnrollmentResponse enroll(Long userId, Long courseId) {
    return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
            .map(EnrollmentResponse::from)
            .orElseGet(() -> {
              Course course = courseRepository.findById(courseId)
                      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
              if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "This course requires payment");
              }
              return doEnroll(userId, course);
            });
  }

  // Called by PaymentService once a course payment has succeeded — no price check,
  // since payment has already been collected.
  @Transactional
  public EnrollmentResponse enrollAfterPayment(Long userId, Long courseId) {
    return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
            .map(EnrollmentResponse::from)
            .orElseGet(() -> {
              Course course = courseRepository.findById(courseId)
                      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
              return doEnroll(userId, course);
            });
  }

  private EnrollmentResponse doEnroll(Long userId, Course course) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

    Enrollment enrollment = new Enrollment();
    enrollment.setUser(user);
    enrollment.setCourse(course);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setTotalScore(0);
    enrollment.setWordsLearnedCount(0);

    Enrollment saved = enrollmentRepository.save(enrollment);

    if (user.getZaloUserId() != null && !user.getZaloUserId().isBlank()) {
      zaloOaService.sendTextMessage(user.getZaloUserId(),
              "Bạn " + user.getUsername() + " đã đăng ký thành công khóa học " + course.getTitle());
    }

    discordNotificationService.sendMessage("Học viên đăng ký khóa học",
            "Học viên **" + user.getUsername() + "** đã đăng ký khóa học **" + course.getTitle() + "**.");

    return EnrollmentResponse.from(saved);
  }

  public List<EnrollmentResponse> listMyEnrollments(Long userId) {
    return enrollmentRepository.findByUserId(userId).stream()
            .map(EnrollmentResponse::from)
            .collect(Collectors.toList());
  }

  public List<LeaderboardEntryResponse> getLeaderboard(Long courseId) {
    List<Enrollment> ranked = enrollmentRepository.findByCourseIdOrderByTotalScoreDesc(courseId);
    return IntStream.range(0, ranked.size())
            .mapToObj(i -> {
              Enrollment e = ranked.get(i);
              return new LeaderboardEntryResponse(i + 1, e.getUser().getId(), e.getUser().getUsername(), e.getTotalScore());
            })
            .collect(Collectors.toList());
  }
}
