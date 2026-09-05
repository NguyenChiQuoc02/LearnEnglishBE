package com.personal.base.controllers;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.personal.base.config.jwt.JwtUtils;
import com.personal.base.dto.auth.GoogleLoginRequest;
import com.personal.base.dto.auth.JwtResponse;
import com.personal.base.dto.auth.LoginRequest;
import com.personal.base.dto.auth.MessageResponse;
import com.personal.base.dto.auth.SignupRequest;
import com.personal.base.models.type.AuthProvider;
import com.personal.base.models.type.ERole;
import com.personal.base.models.Role;
import com.personal.base.models.User;
import com.personal.base.repository.RoleRepository;
import com.personal.base.repository.UserRepository;
import com.personal.base.services.DiscordNotificationService;
import com.personal.base.services.EmailService;
import com.personal.base.services.GoogleTokenService;
import com.personal.base.services.UserDetailsImpl;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;

  @Autowired
  EmailService emailService;

  @Autowired
  DiscordNotificationService discordNotificationService;

  @Autowired
  GoogleTokenService googleTokenService;

  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtUtils.generateJwtToken(authentication);

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    List<String> roles = userDetails.getAuthorities().stream()
            .map(item -> item.getAuthority())
            .collect(Collectors.toList());

    return ResponseEntity.ok(new JwtResponse(jwt,
            userDetails.getId(),
            userDetails.getUsername(),
            userDetails.getEmail(),
            roles));
  }

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity
              .badRequest()
              .body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity
              .badRequest()
              .body(new MessageResponse("Error: Email is already in use!"));
    }

    // Create new user's account
    User user = new User(signUpRequest.getUsername(),
            signUpRequest.getEmail(),
            encoder.encode(signUpRequest.getPassword()));

    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
          case "admin":
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);

            break;

          case "teacher":
            Role teacherRole = roleRepository.findByName(ERole.ROLE_TEACHER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(teacherRole);

            break;

          default:
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    userRepository.save(user);

    emailService.sendRegistrationSuccessEmail(user.getEmail(), user.getUsername());

    discordNotificationService.sendMessage("Học viên mới đăng ký",
            "Học viên **" + user.getUsername() + "** (" + user.getEmail() + ") vừa đăng ký tài khoản thành công.");

    return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
  }

  // Frontend sends the ID token it got from Google Identity Services after the user
  // picks an account; we verify it server-side, then find-or-create the local user and
  // issue our own app JWT so the rest of the app never needs to know Google was involved.
  @PostMapping("/google")
  public ResponseEntity<?> authenticateGoogleUser(@Valid @RequestBody GoogleLoginRequest request) {
    GoogleIdToken.Payload payload = googleTokenService.verify(request.getIdToken());

    String email = payload.getEmail();
    String googleId = payload.getSubject();

    User user = userRepository.findByEmail(email)
            .orElseGet(() -> registerGoogleUser(email, googleId, payload));

    if (user.getGoogleId() == null) {
      user.setGoogleId(googleId);
      userRepository.save(user);
    }

    String jwt = jwtUtils.generateJwtToken(user.getUsername());
    List<String> roles = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toList());

    return ResponseEntity.ok(new JwtResponse(jwt,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            roles));
  }

  private User registerGoogleUser(String email, String googleId, GoogleIdToken.Payload payload) {
    Role userRole = roleRepository.findByName(ERole.ROLE_USER)
            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

    User user = new User();
    user.setUsername(generateUsernameFromEmail(email));
    user.setEmail(email);
    // Google-authenticated accounts never log in with a password, but the column is
    // NOT NULL, so store an unusable random hash instead of relaxing the constraint.
    user.setPassword(encoder.encode(UUID.randomUUID().toString()));
    user.setAuthProvider(AuthProvider.GOOGLE);
    user.setGoogleId(googleId);
    Object picture = payload.get("picture");
    if (picture != null) {
      user.setAvatarUrl(picture.toString());
    }
    user.setRoles(Set.of(userRole));

    userRepository.save(user);

    discordNotificationService.sendMessage("Học viên mới đăng ký",
            "Học viên **" + user.getUsername() + "** (" + user.getEmail() + ") vừa đăng ký tài khoản bằng Google.");

    return user;
  }

  private String generateUsernameFromEmail(String email) {
    String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9._-]", "");
    if (base.isBlank()) {
      base = "user";
    }
    String candidate = base;
    int suffix = 0;
    while (userRepository.existsByUsername(candidate)) {
      suffix++;
      candidate = base + suffix;
    }
    return candidate;
  }
}