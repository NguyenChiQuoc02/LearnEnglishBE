package com.personal.base.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.personal.base.dto.zalo.ZaloAuthUrlResponse;
import com.personal.base.dto.zalo.ZaloLinkCodeResponse;
import com.personal.base.dto.zalo.ZaloMeResponse;
import com.personal.base.dto.zalo.ZaloStatusResponse;
import com.personal.base.services.UserDetailsImpl;
import com.personal.base.services.ZaloOaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/zalo")
public class ZaloController {

  private static final Logger log = LoggerFactory.getLogger(ZaloController.class);

  @Autowired
  private ZaloOaService zaloOaService;

  @Value("${app.frontend.url}")
  private String frontendUrl;

  @GetMapping("/auth-url")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ZaloAuthUrlResponse> getAuthorizationUrl() {
    return ResponseEntity.ok(new ZaloAuthUrlResponse(zaloOaService.getAuthorizationUrl()));
  }

  @GetMapping("/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ZaloStatusResponse> getStatus() {
    return ResponseEntity.ok(new ZaloStatusResponse(zaloOaService.isConnected()));
  }

  // Public: Zalo redirects the admin's browser here after they approve the OA connection.
  @GetMapping("/oauth/callback")
  public ResponseEntity<Void> oauthCallback(@RequestParam String code) {
    zaloOaService.handleOAuthCallback(code);
    return ResponseEntity.status(302)
            .location(URI.create(frontendUrl + "/dashboard/notifications?zaloConnected=1"))
            .build();
  }

  // Public: Zalo calls this whenever a follower interacts with the OA (follow, message, ...).
  @PostMapping("/webhook")
  public ResponseEntity<String> webhook(@RequestBody JsonNode payload) {
    try {
      String eventName = payload.path("event_name").asText("");
      String senderId = payload.path("sender").path("id").asText(null);

      if ("user_send_text".equals(eventName) && senderId != null) {
        String text = payload.path("message").path("text").asText(null);
        zaloOaService.tryLinkByCode(senderId, text);
      }
    } catch (Exception e) {
      log.warn("Failed to process Zalo webhook payload", e);
    }
    return ResponseEntity.ok("OK");
  }

  @PostMapping("/link-code")
  public ResponseEntity<ZaloLinkCodeResponse> generateLinkCode(@AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(zaloOaService.generateLinkCode(currentUser.getId()));
  }

  @GetMapping("/me")
  public ResponseEntity<ZaloMeResponse> getMyZaloStatus(@AuthenticationPrincipal UserDetailsImpl currentUser) {
    return ResponseEntity.ok(new ZaloMeResponse(zaloOaService.isLinked(currentUser.getId())));
  }
}
