package com.personal.base.repository;

import com.personal.base.models.ERole;
import com.personal.base.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long > {
  Optional<User> findByUsername(String username);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);

  Boolean existsByUsernameAndIdNot(String username, Long id);

  Boolean existsByEmailAndIdNot(String email, Long id);

  List<User> findByRoles_Name(ERole name);
}
