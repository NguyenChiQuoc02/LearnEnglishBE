package com.personal.base.repository;

import com.personal.base.models.LearningSessionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningSessionItemRepository extends JpaRepository<LearningSessionItem, Long> {
  List<LearningSessionItem> findBySessionIdOrderByOrderIndexAsc(Long sessionId);

  long countBySessionId(Long sessionId);
}
