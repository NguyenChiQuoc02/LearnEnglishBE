package com.personal.base.repository;

import com.personal.base.models.UserVocabularyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVocabularyProgressRepository extends JpaRepository<UserVocabularyProgress, Long> {
  Optional<UserVocabularyProgress> findByUserIdAndVocabularyItemId(Long userId, Long vocabularyItemId);

  List<UserVocabularyProgress> findByUserIdAndMasteredFalse(Long userId);

  List<UserVocabularyProgress> findByUser_IdAndVocabularyItem_CourseId(Long userId, Long courseId);
}
