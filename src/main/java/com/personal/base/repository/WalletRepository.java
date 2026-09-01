package com.personal.base.repository;

import com.personal.base.models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
  Optional<Wallet> findByUserId(Long userId);

  @Query("select coalesce(sum(w.balance), 0) from Wallet w")
  BigDecimal sumAllBalances();
}
