package com.personal.base.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transient scratch table for the user-import pipeline: Excel rows land here
// first (raw, unvalidated), get validated + annotated with errorMessage in
// place, and only rows with errorMessage == null are transformed and loaded
// into `users`. Rows are deleted once a batch finishes processing.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "staging_users_import")
public class StagingUserImport {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "batch_id", nullable = false, length = 36)
  private String batchId;

  // Named "row_num", not "row_number" — the latter is a reserved keyword in
  // MySQL 8+ (ROW_NUMBER() window function) and breaks unquoted DDL/DML.
  @Column(name = "row_num", nullable = false)
  private Integer rowNumber;

  private String username;
  private String email;
  private String password;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "date_of_birth_raw")
  private String dateOfBirthRaw;

  private String address;
  private String role;

  @Column(name = "error_message", length = 500)
  private String errorMessage;
}
