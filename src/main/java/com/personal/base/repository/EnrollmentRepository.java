package com.personal.base.repository;

import com.personal.base.models.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
  Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

  Boolean existsByUserIdAndCourseId(Long userId, Long courseId);

  Boolean existsByCourseId(Long courseId);

  List<Enrollment> findByUserId(Long userId);

  // Course leaderboard/ranking — highest cumulative score first.
  List<Enrollment> findByCourseIdOrderByTotalScoreDesc(Long courseId);

  // Course management — students roster, most recently enrolled first.
  List<Enrollment> findByCourseIdOrderByEnrolledAtDesc(Long courseId);
}
