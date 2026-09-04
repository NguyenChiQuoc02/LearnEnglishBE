package com.personal.base.repository;

import com.personal.base.models.WithdrawalRequest;
import com.personal.base.models.type.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
  Page<WithdrawalRequest> findByUserId(Long userId, Pageable pageable);

  Page<WithdrawalRequest> findByStatus(WithdrawalStatus status, Pageable pageable);

  long countByStatus(WithdrawalStatus status);

  @Query("select coalesce(sum(w.amount), 0) from WithdrawalRequest w where w.status = :status")
  BigDecimal sumAmountByStatus(@Param("status") WithdrawalStatus status);
}
