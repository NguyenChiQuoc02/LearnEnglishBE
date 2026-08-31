package com.personal.base.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "administrative_regions")
public class AdministrativeRegion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotBlank
  private String name;

  @NotBlank
  @Column(name = "name_en")
  private String nameEn;

  @Column(name = "code_name")
  private String codeName;

  @Column(name = "code_name_en")
  private String codeNameEn;
}
