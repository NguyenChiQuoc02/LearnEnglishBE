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
@Table(name = "provinces")
public class Province {
  @Id
  @Column(length = 20)
  private String code;

  @NotBlank
  private String name;

  @Column(name = "name_en")
  private String nameEn;

  @NotBlank
  @Column(name = "full_name")
  private String fullName;

  @Column(name = "full_name_en")
  private String fullNameEn;

  @Column(name = "code_name")
  private String codeName;

  @Column(name = "postal_code_prefix")
  private String postalCodePrefix;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "administrative_unit_id")
  private AdministrativeUnit administrativeUnit;
}
