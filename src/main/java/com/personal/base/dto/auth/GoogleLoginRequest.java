package com.personal.base.dto.auth;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GoogleLoginRequest {
  // ID token returned by Google Identity Services on the frontend, to be verified server-side.
  @NotBlank
  private String idToken;
}
