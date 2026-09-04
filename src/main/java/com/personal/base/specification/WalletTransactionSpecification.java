package com.personal.base.specification;

import com.personal.base.models.WalletTransaction;
import com.personal.base.models.type.WalletTransactionStatus;
import com.personal.base.models.type.WalletTransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

// Dynamic filtering for the admin wallet-transactions listing (type/status/userId/date range).
public class WalletTransactionSpecification {

  private WalletTransactionSpecification() {
  }

  public static Specification<WalletTransaction> hasType(WalletTransactionType type) {
    return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
  }

  public static Specification<WalletTransaction> hasStatus(WalletTransactionStatus status) {
    return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
  }

  public static Specification<WalletTransaction> hasUserId(Long userId) {
    return (root, query, cb) -> userId == null ? null : cb.equal(root.get("wallet").get("user").get("id"), userId);
  }

  public static Specification<WalletTransaction> createdBetween(Instant from, Instant to) {
    return (root, query, cb) -> {
      if (from == null && to == null) return null;
      if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
      if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
      return cb.lessThanOrEqualTo(root.get("createdAt"), to);
    };
  }
}
