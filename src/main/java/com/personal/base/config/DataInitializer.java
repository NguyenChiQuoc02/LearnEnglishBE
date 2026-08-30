package com.personal.base.config;

import com.personal.base.models.ERole;
import com.personal.base.models.Role;
import com.personal.base.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

  @Bean
  CommandLineRunner seedRoles(RoleRepository roleRepository) {
    return args -> {
      for (ERole roleName : ERole.values()) {
        roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
      }
    };
  }
}
