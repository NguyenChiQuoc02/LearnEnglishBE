package com.personal.base.repository;

import com.personal.base.models.AdministrativeUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdministrativeUnitRepository extends JpaRepository<AdministrativeUnit, Integer> {
}
