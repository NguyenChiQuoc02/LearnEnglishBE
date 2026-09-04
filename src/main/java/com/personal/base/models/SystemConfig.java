package com.personal.base.models;

import com.personal.base.models.type.UploadMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Single-row table (id is always 1) holding app-wide settings, e.g. which storage
// backend file uploads should go through. A personal project only ever needs one
// active configuration, so a full settings-per-tenant table would be unused complexity.
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "system_configs")
public class SystemConfig {
  @Id
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "upload_method", length = 20, nullable = false)
  private UploadMethod uploadMethod;
}
