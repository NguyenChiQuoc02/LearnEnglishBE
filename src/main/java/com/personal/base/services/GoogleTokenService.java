package com.personal.base.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

// Verifies ID tokens issued by Google Identity Services on the frontend, so the backend
// can trust the email/subject claims without ever seeing the user's Google password.
@Service
public class GoogleTokenService {

  @Value("${google.oauth.clientId}")
  private String clientId;

  private volatile GoogleIdTokenVerifier verifier;

  public GoogleIdToken.Payload verify(String idTokenString) {
    try {
      GoogleIdToken idToken = verifier().verify(idTokenString);
      if (idToken == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google ID token không hợp lệ hoặc đã hết hạn");
      }
      return idToken.getPayload();
    } catch (GeneralSecurityException | IOException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không thể xác minh Google ID token");
    }
  }

  private GoogleIdTokenVerifier verifier() {
    if (verifier == null) {
      if (clientId == null || clientId.isBlank()) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google Sign-In chưa được cấu hình (google.oauth.clientId)");
      }
      synchronized (this) {
        if (verifier == null) {
          verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                  .setAudience(Collections.singletonList(clientId))
                  .build();
        }
      }
    }
    return verifier;
  }
}
