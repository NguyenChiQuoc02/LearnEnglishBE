package com.personal.base.repository;

import com.personal.base.models.ZaloOaCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZaloOaCredentialRepository extends JpaRepository<ZaloOaCredential, Long> {
}
