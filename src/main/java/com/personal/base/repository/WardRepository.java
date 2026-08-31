package com.personal.base.repository;

import com.personal.base.models.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WardRepository extends JpaRepository<Ward, String> {
  List<Ward> findByProvinceCode(String provinceCode);
}
