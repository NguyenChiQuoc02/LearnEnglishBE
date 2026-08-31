package com.personal.base.repository;

import com.personal.base.models.ERole;
import com.personal.base.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long > {
  Optional<User> findByUsername(String username);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);

  Boolean existsByUsernameAndIdNot(String username, Long id);

  Boolean existsByEmailAndIdNot(String email, Long id);

  Boolean existsByPhoneNumber(String phoneNumber);

  List<User> findByRoles_Name(ERole name);

  // Bulk existence checks used by user import: one query per field instead of
  // one exists-query per row, so validating N rows no longer costs 3N round trips.
  List<User> findByUsernameIn(Collection<String> usernames);

  List<User> findByEmailIn(Collection<String> emails);

  List<User> findByPhoneNumberIn(Collection<String> phoneNumbers);

  // Used by the export worker to read users in fixed-size pages instead of loading
  // the whole table into memory. Both filters are optional (pass null to skip).
  @Query("""
      select distinct u from User u
      left join u.roles r
      where (:role is null or r.name = :role)
        and (:keyword is null
             or lower(u.username) like concat('%', :keyword, '%')
             or lower(u.email) like concat('%', :keyword, '%')
             or u.phoneNumber like concat('%', :keyword, '%'))
      order by u.id asc
      """)
  Page<User> searchForExport(@Param("role") ERole role, @Param("keyword") String keyword, Pageable pageable);

  // Backs the admin Users list page's server-side pagination + search box.
  // No role join needed here (unlike searchForExport), so no distinct/fanout to worry about.
  @Query("""
      select u from User u
      where (:keyword is null
             or lower(u.username) like concat('%', :keyword, '%')
             or lower(u.email) like concat('%', :keyword, '%')
             or u.phoneNumber like concat('%', :keyword, '%'))
      order by u.id asc
      """)
  Page<User> search(@Param("keyword") String keyword, Pageable pageable);
}
