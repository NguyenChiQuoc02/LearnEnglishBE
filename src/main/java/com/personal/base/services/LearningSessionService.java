package com.personal.base.services;

import com.personal.base.dto.session.CompleteSessionResponse;
import com.personal.base.dto.session.SessionWordResponse;
import com.personal.base.dto.session.StartSessionResponse;
import com.personal.base.dto.session.SubmitAnswerRequest;
import com.personal.base.dto.session.SubmitAnswerResponse;
import com.personal.base.models.Course;
import com.personal.base.models.Enrollment;
import com.personal.base.models.LearningSession;
import com.personal.base.models.LearningSessionItem;
import com.personal.base.models.SessionStatus;
import com.personal.base.models.User;
import com.personal.base.models.UserVocabularyProgress;
import com.personal.base.models.VocabularyItem;
import com.personal.base.repository.EnrollmentRepository;
import com.personal.base.repository.LearningSessionItemRepository;
import com.personal.base.repository.LearningSessionRepository;
import com.personal.base.repository.UserRepository;
import com.personal.base.repository.UserVocabularyProgressRepository;
import com.personal.base.repository.VocabularyItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LearningSessionService {

  @Autowired
  private EnrollmentRepository enrollmentRepository;

  @Autowired
  private VocabularyItemRepository vocabularyItemRepository;

  @Autowired
  private UserVocabularyProgressRepository progressRepository;

  @Autowired
  private LearningSessionRepository sessionRepository;

  @Autowired
  private LearningSessionItemRepository sessionItemRepository;

  @Autowired
  private UserRepository userRepository;

  @Transactional
  public StartSessionResponse startSession(Long userId, Long courseId) {
    Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enrolled in this course"));

    Course course = enrollment.getCourse();
    List<VocabularyItem> allWords = vocabularyItemRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    if (allWords.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course has no vocabulary yet");
    }

    Set<Long> masteredIds = progressRepository.findByUser_IdAndVocabularyItem_CourseId(userId, courseId).stream()
            .filter(UserVocabularyProgress::getMastered)
            .map(p -> p.getVocabularyItem().getId())
            .collect(Collectors.toSet());

    List<VocabularyItem> unmastered = allWords.stream()
            .filter(w -> !masteredIds.contains(w.getId()))
            .collect(Collectors.toList());
    List<VocabularyItem> mastered = allWords.stream()
            .filter(w -> masteredIds.contains(w.getId()))
            .collect(Collectors.toList());

    Collections.shuffle(unmastered);
    Collections.shuffle(mastered);

    int wordsPerSession = Math.min(course.getWordsPerSession(), allWords.size());
    List<VocabularyItem> picked = new ArrayList<>(unmastered.subList(0, Math.min(wordsPerSession, unmastered.size())));
    if (picked.size() < wordsPerSession) {
      int remaining = wordsPerSession - picked.size();
      picked.addAll(mastered.subList(0, Math.min(remaining, mastered.size())));
    }

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

    LearningSession session = new LearningSession();
    session.setUser(user);
    session.setCourse(course);
    session.setEnrollment(enrollment);
    session.setTotalWords(picked.size());
    session.setCorrectCount(0);
    session.setWrongCount(0);
    session.setScoreEarned(0);
    session.setStatus(SessionStatus.IN_PROGRESS);
    LearningSession savedSession = sessionRepository.save(session);

    List<SessionWordResponse> words = new ArrayList<>();
    for (int i = 0; i < picked.size(); i++) {
      VocabularyItem w = picked.get(i);
      words.add(new SessionWordResponse(
              w.getId(), w.getWord(), w.getPhonetic(), w.getPartOfSpeech(), w.getMeaning(), w.getImageUrl(), w.getAudioUrl(), i));
    }

    return new StartSessionResponse(savedSession.getId(), course.getId(), savedSession.getTotalWords(), words);
  }

  @Transactional
  public SubmitAnswerResponse submitAnswer(Long userId, Long sessionId, SubmitAnswerRequest request) {
    LearningSession session = sessionRepository.findByIdAndUser_Id(sessionId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (session.getStatus() != SessionStatus.IN_PROGRESS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not in progress");
    }

    VocabularyItem vocabularyItem = vocabularyItemRepository.findById(request.getVocabularyItemId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
    if (!vocabularyItem.getCourse().getId().equals(session.getCourse().getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word does not belong to this session's course");
    }

    boolean skipped = Boolean.TRUE.equals(request.getSkipped());
    boolean correct = !skipped && request.getAnswer() != null
            && request.getAnswer().trim().equalsIgnoreCase(vocabularyItem.getWord());

    Course course = session.getCourse();
    int points = correct ? course.getPointsPerCorrect() : course.getPointsPerWrong();

    LearningSessionItem item = new LearningSessionItem();
    item.setSession(session);
    item.setVocabularyItem(vocabularyItem);
    item.setUserAnswer(request.getAnswer());
    item.setCorrect(correct);
    item.setUsedHint(Boolean.TRUE.equals(request.getUsedHint()));
    item.setSkipped(skipped);
    item.setPointsEarned(points);
    item.setOrderIndex((int) sessionItemRepository.countBySessionId(sessionId));
    item.setAnsweredAt(Instant.now());
    sessionItemRepository.save(item);

    if (correct) {
      session.setCorrectCount(session.getCorrectCount() + 1);
    } else {
      session.setWrongCount(session.getWrongCount() + 1);
    }
    session.setScoreEarned(session.getScoreEarned() + points);
    sessionRepository.save(session);

    updateProgress(userId, vocabularyItem, correct);

    return new SubmitAnswerResponse(correct, vocabularyItem.getWord(), points, session.getScoreEarned());
  }

  private void updateProgress(Long userId, VocabularyItem vocabularyItem, boolean correct) {
    UserVocabularyProgress progress = progressRepository
            .findByUserIdAndVocabularyItemId(userId, vocabularyItem.getId())
            .orElseGet(() -> {
              UserVocabularyProgress p = new UserVocabularyProgress();
              User user = userRepository.findById(userId)
                      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
              p.setUser(user);
              p.setVocabularyItem(vocabularyItem);
              p.setTimesCorrect(0);
              p.setTimesWrong(0);
              p.setMastered(false);
              return p;
            });

    if (correct) {
      progress.setTimesCorrect(progress.getTimesCorrect() + 1);
    } else {
      progress.setTimesWrong(progress.getTimesWrong() + 1);
    }
    progress.setMastered(progress.getTimesCorrect() >= 3);
    progress.setLastReviewedAt(Instant.now());
    progressRepository.save(progress);
  }

  @Transactional
  public CompleteSessionResponse completeSession(Long userId, Long sessionId) {
    LearningSession session = sessionRepository.findByIdAndUser_Id(sessionId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (session.getStatus() != SessionStatus.IN_PROGRESS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session already completed");
    }

    long answered = sessionItemRepository.countBySessionId(sessionId);
    if (answered < session.getTotalWords()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not all words answered yet");
    }

    session.setStatus(SessionStatus.COMPLETED);
    session.setCompletedAt(Instant.now());
    sessionRepository.save(session);

    Enrollment enrollment = session.getEnrollment();
    enrollment.setTotalScore(enrollment.getTotalScore() + session.getScoreEarned());
    enrollment.setWordsLearnedCount(enrollment.getWordsLearnedCount() + session.getCorrectCount());
    enrollment.setLastStudiedAt(Instant.now());
    enrollmentRepository.save(enrollment);

    return new CompleteSessionResponse(
            session.getId(), session.getTotalWords(), session.getCorrectCount(),
            session.getWrongCount(), session.getScoreEarned(), enrollment.getTotalScore());
  }
}
