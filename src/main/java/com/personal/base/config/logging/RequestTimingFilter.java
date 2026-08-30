package com.personal.base.config.logging;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

// Logs how long every /api/** call took. Anything over SLOW_THRESHOLD_MS is
// logged as a warning so slow endpoints stand out in the console/log files.
public class RequestTimingFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(RequestTimingFilter.class);
  private static final long SLOW_THRESHOLD_MS = 1000;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
    long startTime = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - startTime;
      String message = String.format("%s %s -> %d (%d ms)",
              request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);

      if (durationMs > SLOW_THRESHOLD_MS) {
        logger.warn("[SLOW API] {}", message);
      } else {
        logger.info("{}", message);
      }
    }
  }
}
