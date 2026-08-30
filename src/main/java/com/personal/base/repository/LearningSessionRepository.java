package com.personal.base.repository;

import com.personal.base.models.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {
  List<LearningSession> findByEnrollmentId(Long enrollmentId);

  List<LearningSession> findByUserIdAndCourseId(Long userId, Long courseId);

  // Per-student learning history within a course, newest first.
  List<LearningSession> findByUserIdAndCourseIdOrderByStartedAtDesc(Long userId, Long courseId);

  Optional<LearningSession> findByIdAndUser_Id(Long id, Long userId);
}
