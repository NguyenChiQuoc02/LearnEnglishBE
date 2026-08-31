package com.personal.base.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// Posts messages to a Discord channel via an Incoming Webhook.
// https://support.discord.com/hc/en-us/articles/228383668
@Service
public class DiscordNotificationService {

  private static final Logger log = LoggerFactory.getLogger(DiscordNotificationService.class);

  @Autowired
  private RestTemplate restTemplate;

  @Value("${discord.webhookUrl:}")
  private String webhookUrl;

  private static final int MAX_ATTEMPTS = 3;
  private static final long RETRY_DELAY_MS = 1500;

  // Fire-and-forget: a missing/unreachable webhook must never break the calling flow
  // (registration, enrollment, ...), so failures are only logged.
  @Async
  public void sendMessage(String title, String text) {
    if (webhookUrl == null || webhookUrl.isBlank()) {
      log.debug("Discord webhook is not configured, skipping notification: {}", title);
      return;
    }

    String content = "**" + title + "**\n" + text;
    Map<String, Object> payload = Map.of("content", content);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Transient DNS/connection blips (flaky VPN, resolver hiccups) are common on dev
    // machines and would otherwise silently drop the notification; retry a couple of
    // times before giving up. A non-2xx response isn't retried since trying again
    // won't change the outcome.
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        restTemplate.postForObject(webhookUrl, new HttpEntity<>(payload, headers), String.class);
        return;
      } catch (ResourceAccessException e) {
        if (attempt == MAX_ATTEMPTS) {
          log.warn("Failed to send Discord notification after {} attempts: {}", attempt, title, e);
        } else {
          log.debug("Discord notification attempt {} failed, retrying: {}", attempt, title, e);
          sleep(RETRY_DELAY_MS);
        }
      } catch (Exception e) {
        log.warn("Failed to send Discord notification: {}", title, e);
        return;
      }
    }
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }
}
