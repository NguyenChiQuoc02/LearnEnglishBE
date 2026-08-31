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

  @Transactional
  public EnrollmentResponse enroll(Long userId, Long courseId) {
    return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
            .map(EnrollmentResponse::from)
            .orElseGet(() -> {
              User user = userRepository.findById(userId)
                      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
              Course course = courseRepository.findById(courseId)
                      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

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
            });
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
