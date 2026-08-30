package com.personal.base.exception;

import java.time.Instant;
import java.util.Map;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// Single response shape for every error the API returns, so FE can rely on
// {status, error, message, path} always being present, and `errors` only
// showing up for field-level validation failures.
@Getter
public class ApiError {
  private final String timestamp;
  private final int status;
  private final String error;
  private final String message;
  private final String path;
  private final Map<String, String> errors;

  private ApiError(HttpStatus status, String message, String path, Map<String, String> errors) {
    this.timestamp = Instant.now().toString();
    this.status = status.value();
    this.error = status.getReasonPhrase();
    this.message = message;
    this.path = path;
    this.errors = errors;
  }

  public static ApiError of(HttpStatus status, String message, String path) {
    return new ApiError(status, message, path, null);
  }

  public static ApiError of(HttpStatus status, String message, String path, Map<String, String> errors) {
    return new ApiError(status, message, path, errors);
  }
}
