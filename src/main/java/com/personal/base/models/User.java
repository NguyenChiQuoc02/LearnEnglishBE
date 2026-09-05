package com.personal.base.models;


import com.personal.base.models.type.AuthProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        })
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  private String username;

  @NotBlank
  @Email
  private String email;

  @NotBlank
  private String password;

  @Column(name = "phone_number", length = 20)
  private String phoneNumber;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Column(length = 255)
  private String address;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  // Zalo OA follower id, set once the user links their account via the /api/zalo/link-code flow.
  @Column(name = "zalo_user_id", length = 64)
  private String zaloUserId;

  // Short-lived code shown to the user to type into the Zalo OA chat, so the webhook can
  // correlate the Zalo follower who sent it back to this account.
  @Column(name = "zalo_link_code", length = 12)
  private String zaloLinkCode;

  @Column(name = "zalo_link_code_expires_at")
  private Instant zaloLinkCodeExpiresAt;

  // How this account authenticates. Google-only accounts still get a random,
  // unusable password hash so the @NotBlank/JPA validation above stays satisfied.
  @Enumerated(EnumType.STRING)
  @Column(name = "auth_provider", nullable = false, length = 20)
  private AuthProvider authProvider = AuthProvider.LOCAL;

  // Google account's stable "sub" claim, set the first time a user signs in with Google.
  @Column(name = "google_id", length = 64, unique = true)
  private String googleId;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(  name = "user_roles",
          joinColumns = @JoinColumn(name = "user_id"),
          inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "province_code")
  private Province province;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ward_code")
  private Ward ward;


  public User(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }

}
