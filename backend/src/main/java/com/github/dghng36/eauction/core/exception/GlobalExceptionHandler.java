package com.github.dghng36.eauction.core.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.github.dghng36.eauction.core.base.ApiResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        log.error("AppException occurred: [{}]: ", ex.getMessage(), ex);

        return ResponseEntity.status(ex.getStatus())
            .body(ApiResponse.error(ex.getMessage(), ex.getStatus().value()));
    }

    // You can add more exception handlers here for other types of exceptions
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex
    ) {
        log.error("Validation failed for method arguments: [{}]: ", ex.getMessage(), ex);

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Invalid input arguments", HttpStatus.BAD_REQUEST.value(), errors));       
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException ex
    ) {
        log.error("HTTP message not readable (Malformed JSON body): [{}]: ", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Invalid request body dynamic structure", HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
        ConstraintViolationException ex
    ) {
        log.error("Constraint violation exception occurred: [{}]: ", ex.getMessage(), ex);
        
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(violation -> {
            String paramName = violation.getPropertyPath().toString();
            if (paramName.contains(".")) {
                paramName = paramName.substring(paramName.lastIndexOf(".") + 1);
            }
            errors.put(paramName, violation.getMessage());
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Invalid request parameters", HttpStatus.BAD_REQUEST.value(), errors));       
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleHandlerMethodValidationException(
        HandlerMethodValidationException ex
    ) {
        log.error("Handler method validation failed: [{}]: ", ex.getMessage(), ex);
        
        Map<String, String> errors = new HashMap<>();

        ex.getParameterValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();

            result.getResolvableErrors().forEach(error ->
                errors.put(paramName, error.getDefaultMessage())
            );
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Invalid input arguments", HttpStatus.BAD_REQUEST.value(), errors));       
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied for the current authenticated user: [{}]: ", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("You do not have permission to access this resource", HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException ex) {
        log.error("Database infrastructure or concurrency conflict occurred: [{}]: ", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("A database error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUncaughtException(Exception ex) {
        log.error("An unexpected internal server error occurred: [{}]: ", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Internal server error. Please contact administrative support.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        log.error("Concurrency conflict detected via Optimistic Locking: [{}]: ", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("Data has conflict by other process", HttpStatus.CONFLICT.value()));
    }
}
