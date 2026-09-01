package com.personal.base.repository;

import com.personal.base.models.WalletTransaction;
import com.personal.base.models.WalletTransactionStatus;
import com.personal.base.models.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long>, JpaSpecificationExecutor<WalletTransaction> {
  Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

  Optional<WalletTransaction> findByMomoOrderId(String momoOrderId);

  @Query("select coalesce(sum(t.amount), 0) from WalletTransaction t where t.type = :type and t.status = :status")
  BigDecimal sumAmountByTypeAndStatus(@Param("type") WalletTransactionType type, @Param("status") WalletTransactionStatus status);
}
