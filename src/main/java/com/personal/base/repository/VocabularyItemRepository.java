package com.personal.base.repository;

import com.personal.base.models.VocabularyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabularyItemRepository extends JpaRepository<VocabularyItem, Long> {
  List<VocabularyItem> findByCourseIdOrderByOrderIndexAsc(Long courseId);

  void deleteByCourseId(Long courseId);
}
