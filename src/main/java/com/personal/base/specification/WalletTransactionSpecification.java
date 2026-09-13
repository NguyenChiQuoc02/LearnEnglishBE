package com.personal.base.specification;

import com.personal.base.models.WalletTransaction;
import com.personal.base.models.type.WalletTransactionStatus;
import com.personal.base.models.type.WalletTransactionType;
import jakarta.persistence.criteria.JoinType;
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

  public static Specification<WalletTransaction> hasWalletId(Long walletId) {
    return (root, query, cb) -> walletId == null ? null : cb.equal(root.get("wallet").get("id"), walletId);
  }

  // Lets a user search their own transaction history by note, MoMo order id, or course title.
  public static Specification<WalletTransaction> matchesKeyword(String keyword) {
    return (root, query, cb) -> {
      if (keyword == null || keyword.isBlank()) return null;
      String like = "%" + keyword.trim().toLowerCase() + "%";
      var courseJoin = root.join("course", JoinType.LEFT);
      return cb.or(
              cb.like(cb.lower(cb.coalesce(root.get("note"), "")), like),
              cb.like(cb.lower(cb.coalesce(root.get("momoOrderId"), "")), like),
              cb.like(cb.lower(cb.coalesce(courseJoin.get("title"), "")), like));
    };
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
