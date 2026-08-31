package com.personal.base.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

// Central place that turns every exception the API can throw into the same
// {timestamp, status, error, message, path} JSON shape, with the correct HTTP
// status, so FE always gets a real message instead of a blanket 401/500.
//
// Auth failures at the filter-chain level (missing/invalid/expired token) are
// handled separately by AuthEntryPointJwt, which uses the same ApiError shape.
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // Thrown explicitly by services with a specific status + reason
  // (e.g. 404 "Course not found", 409 "Cannot delete a user that owns existing data").
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
    return ResponseEntity.status(status).body(ApiError.of(status, message, request.getRequestURI()));
  }

  // @Valid failures on @RequestBody DTOs (e.g. blank username, invalid email).
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    ApiError body = ApiError.of(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", request.getRequestURI(), fieldErrors);
    return ResponseEntity.badRequest().body(body);
  }

  // Body is missing / not valid JSON.
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableBody(HttpServletRequest request) {
    ApiError body = ApiError.of(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không đúng định dạng", request.getRequestURI());
    return ResponseEntity.badRequest().body(body);
  }

  // Required query/path param missing.
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
    ApiError body = ApiError.of(HttpStatus.BAD_REQUEST, "Thiếu tham số bắt buộc: " + ex.getParameterName(), request.getRequestURI());
    return ResponseEntity.badRequest().body(body);
  }

  // DB-level unique/foreign key violations that slip past service-level checks
  // (e.g. race condition on username/email uniqueness).
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
    logger.warn("Data integrity violation: {}", ex.getMessage());
    ApiError body = ApiError.of(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc vi phạm ràng buộc", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  // Wrong username/password on /api/auth/signin.
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(HttpServletRequest request) {
    ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
  }

  // Any other authentication failure thrown outside the JWT filter (e.g. disabled/locked account).
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
    ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
  }

  // Authenticated but lacks the required role, e.g. a non-admin calling an
  // ADMIN-only endpoint (@PreAuthorize). Must stay 403, not fall through to 401.
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
    ApiError body = ApiError.of(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
  }

  // Endpoint exists but was called with the wrong HTTP method (e.g. GET on a POST-only route).
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    ApiError body = ApiError.of(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức " + ex.getMethod() + " không được hỗ trợ cho endpoint này", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
  }

  // Content-Type of the request body isn't accepted (e.g. missing application/json).
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpServletRequest request) {
    ApiError body = ApiError.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng nội dung gửi lên không được hỗ trợ", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
  }

  // No route matches at all (requires spring.mvc.throw-exception-if-no-handler-found=true).
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ApiError> handleNoHandlerFound(HttpServletRequest request) {
    ApiError body = ApiError.of(HttpStatus.NOT_FOUND, "Không tìm thấy endpoint", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  // Last resort — never leak the raw exception message/stack trace to the client.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
    logger.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
    ApiError body = ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "Đã có lỗi xảy ra, vui lòng thử lại sau", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
