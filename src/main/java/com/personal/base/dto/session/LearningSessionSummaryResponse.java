package com.personal.base.dto.session;

import com.personal.base.models.LearningSession;
import com.personal.base.models.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LearningSessionSummaryResponse {
  private Long sessionId;
  private Integer totalWords;
  private Integer correctCount;
  private Integer wrongCount;
  private Integer scoreEarned;
  private SessionStatus status;
  private Instant startedAt;
  private Instant completedAt;

  public static LearningSessionSummaryResponse from(LearningSession session) {
    return new LearningSessionSummaryResponse(
            session.getId(),
            session.getTotalWords(),
            session.getCorrectCount(),
            session.getWrongCount(),
            session.getScoreEarned(),
            session.getStatus(),
            session.getStartedAt(),
            session.getCompletedAt());
  }
}
