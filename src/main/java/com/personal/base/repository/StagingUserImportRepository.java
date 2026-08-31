package com.personal.base.repository;

import com.personal.base.models.StagingUserImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface StagingUserImportRepository extends JpaRepository<StagingUserImport, Long> {
  @Modifying
  void deleteByBatchId(String batchId);
}
