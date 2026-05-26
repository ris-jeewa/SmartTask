package com.smarttask.user.exception;

import com.smarttask.user.exception.dto.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(DuplicateRegistrationException.class)
	public ResponseEntity<ApiError> handleDuplicate(DuplicateRegistrationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of(ex.getMessage()));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiError.of(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		StringBuilder msg = new StringBuilder("Validation failed");
		for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
			msg.append("; ").append(fe.getField()).append(": ").append(fe.getDefaultMessage());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(msg.toString()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
		log.warn("Illegal state (often configuration): {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(ex.getMessage()));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ApiError> handleDataAccess(DataAccessException ex) {
		log.error("Database error during request", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of("Database error — check server logs (connection / schema / JDBC URL)."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex) {
		log.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of("Unexpected error — check server logs."));
	}
}
