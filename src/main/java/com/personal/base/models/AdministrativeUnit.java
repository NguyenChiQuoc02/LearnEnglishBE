package com.personal.base.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "administrative_units")
public class AdministrativeUnit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "full_name")
  private String fullName;

  @Column(name = "full_name_en")
  private String fullNameEn;

  @Column(name = "short_name")
  private String shortName;

  @Column(name = "short_name_en")
  private String shortNameEn;

  @Column(name = "code_name")
  private String codeName;

  @Column(name = "code_name_en")
  private String codeNameEn;
}
