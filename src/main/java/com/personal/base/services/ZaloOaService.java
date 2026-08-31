package com.personal.base.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.personal.base.dto.zalo.ZaloLinkCodeResponse;
import com.personal.base.models.User;
import com.personal.base.models.ZaloOaCredential;
import com.personal.base.repository.UserRepository;
import com.personal.base.repository.ZaloOaCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
public class ZaloOaService {

  private static final Logger log = LoggerFactory.getLogger(ZaloOaService.class);
  private static final Long CREDENTIAL_ID = 1L;
  private static final String OAUTH_TOKEN_URL = "https://oauth.zaloapp.com/v4/oa/access_token";
  private static final String OAUTH_PERMISSION_URL = "https://oauth.zaloapp.com/v4/oa/permission";
  private static final String SEND_MESSAGE_URL = "https://openapi.zalo.me/v3.0/oa/message/cs";
  private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private ZaloOaCredentialRepository credentialRepository;

  @Autowired
  private UserRepository userRepository;

  @Value("${zalo.oa.appId}")
  private String appId;

  @Value("${zalo.oa.secretKey}")
  private String secretKey;

  @Value("${zalo.oa.redirectUri}")
  private String redirectUri;

  @Value("${zalo.oa.followUrl}")
  private String followUrl;

  private final SecureRandom random = new SecureRandom();

  public String getAuthorizationUrl() {
    return UriComponentsBuilder.fromUriString(OAUTH_PERMISSION_URL)
            .queryParam("app_id", appId)
            .queryParam("redirect_uri", redirectUri)
            .build()
            .toUriString();
  }

  public boolean isConnected() {
    return credentialRepository.existsById(CREDENTIAL_ID);
  }

  @Transactional
  public void handleOAuthCallback(String code) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("code", code);
    body.add("app_id", appId);
    body.add("grant_type", "authorization_code");

    JsonNode response = postForm(body);
    saveCredential(response);
  }

  @Transactional
  public void refreshAccessToken(ZaloOaCredential credential) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("refresh_token", credential.getRefreshToken());
    body.add("app_id", appId);
    body.add("grant_type", "refresh_token");

    JsonNode response = postForm(body);
    saveCredential(response);
  }

  private JsonNode postForm(MultiValueMap<String, String> body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.add("secret_key", secretKey);

    JsonNode response = restTemplate.postForObject(OAUTH_TOKEN_URL, new HttpEntity<>(body, headers), JsonNode.class);
    if (response == null || !response.hasNonNull("access_token")) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Zalo did not return an access token");
    }
    return response;
  }

  private void saveCredential(JsonNode response) {
    ZaloOaCredential credential = credentialRepository.findById(CREDENTIAL_ID).orElse(new ZaloOaCredential());
    credential.setId(CREDENTIAL_ID);
    credential.setAccessToken(response.get("access_token").asText());
    credential.setRefreshToken(response.get("refresh_token").asText());
    long expiresInSeconds = response.has("expires_in") ? response.get("expires_in").asLong(3600) : 3600;
    credential.setExpiresAt(Instant.now().plusSeconds(expiresInSeconds));
    credential.setUpdatedAt(Instant.now());
    credentialRepository.save(credential);
  }

  private String getValidAccessToken() {
    ZaloOaCredential credential = credentialRepository.findById(CREDENTIAL_ID)
            .orElseThrow(() -> new IllegalStateException("Zalo OA is not connected yet"));
    // Refresh a little before actual expiry so a send request never races an expired token.
    if (Instant.now().isAfter(credential.getExpiresAt().minusSeconds(60))) {
      refreshAccessToken(credential);
      credential = credentialRepository.findById(CREDENTIAL_ID).orElseThrow();
    }
    return credential.getAccessToken();
  }

  // Best-effort send: swallows failures (expired 7-day interaction window, unfollowed, etc.)
  // so one bad recipient never breaks a notification's fan-out to the rest.
  public void sendTextMessage(String zaloUserId, String text) {
    try {
      String accessToken = getValidAccessToken();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.add("access_token", accessToken);

      var recipient = java.util.Map.of("user_id", zaloUserId);
      var message = java.util.Map.of("text", text);
      var payload = java.util.Map.of("recipient", recipient, "message", message);

      JsonNode response = restTemplate.postForObject(SEND_MESSAGE_URL, new HttpEntity<>(payload, headers), JsonNode.class);
      if (response != null && response.has("error") && response.get("error").asInt() != 0) {
        log.warn("Zalo send to {} failed: {}", zaloUserId, response.get("message"));
      }
    } catch (Exception e) {
      log.warn("Zalo send to {} failed", zaloUserId, e);
    }
  }

  @Transactional
  public ZaloLinkCodeResponse generateLinkCode(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    String code = randomCode();
    Instant expiresAt = Instant.now().plusSeconds(15 * 60);
    user.setZaloLinkCode(code);
    user.setZaloLinkCodeExpiresAt(expiresAt);
    userRepository.save(user);

    return new ZaloLinkCodeResponse(code, followUrl, expiresAt);
  }

  public boolean isLinked(Long userId) {
    return userRepository.findById(userId)
            .map(u -> u.getZaloUserId() != null)
            .orElse(false);
  }

  // Called from the webhook when the user texts the OA. If the message matches an
  // unexpired link code, associates that Zalo follower with the corresponding account.
  @Transactional
  public void tryLinkByCode(String zaloUserId, String messageText) {
    if (messageText == null) return;
    String code = messageText.trim().toUpperCase();

    Optional<User> match = userRepository.findByZaloLinkCode(code);
    match.ifPresent(user -> {
      if (user.getZaloLinkCodeExpiresAt() != null && user.getZaloLinkCodeExpiresAt().isAfter(Instant.now())) {
        user.setZaloUserId(zaloUserId);
        user.setZaloLinkCode(null);
        user.setZaloLinkCodeExpiresAt(null);
        userRepository.save(user);
        sendTextMessage(zaloUserId, "Liên kết tài khoản Learn English thành công!");
      }
    });
  }

  private String randomCode() {
    StringBuilder sb = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
    }
    return sb.toString();
  }
}
