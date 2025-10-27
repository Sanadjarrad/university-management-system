package com.demo.universityManagementApp.exception;

import com.demo.universityManagementApp.exception.conflict.ConflictException;
import com.demo.universityManagementApp.exception.db.DatabaseDeleteConflictException;
import com.demo.universityManagementApp.exception.db.DatabaseException;
import com.demo.universityManagementApp.exception.domain.classSession.ClassSessionException;
import com.demo.universityManagementApp.exception.domain.validation.InvalidArgsException;
import com.demo.universityManagementApp.exception.notfound.NotFoundException;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.ErrorResponse;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(final MethodArgumentNotValidException ex, final WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapToValidationError)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .path(request.getDescription(false))
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(final ConstraintViolationException ex, final WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());

        List<ErrorResponse.ValidationError> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> ErrorResponse.ValidationError.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Request constraints violated")
                .path(request.getDescription(false))
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse<Void>> handleIllegalArgumentException(final IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleNotFoundException(final NotFoundException ex) {
        log.warn("Not found exception: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<APIResponse<Void>> handleConflictException(final ConflictException ex) {
        log.warn("Conflict exception: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<APIResponse<Void>> handleValidationException(final ValidationException ex) {
        log.warn("Validation exception: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DatabaseDeleteConflictException.class)
    public ResponseEntity<APIResponse<Void>> handleDatabaseDeleteConflictException(final DatabaseDeleteConflictException ex) {
        log.warn("Database delete conflict: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BaseUniversityAppException.class)
    public ResponseEntity<APIResponse<Void>> handleUniversityAppException(final BaseUniversityAppException ex) {
        log.warn("Business exception: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>error(ex.getMessage());
        HttpStatus status = determineHttpStatus(ex);

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<APIResponse<Void>> handleAuthorizationDeniedException(final AuthorizationDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>error("Access denied");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleGenericException(final Exception ex, final WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        APIResponse<Void> response = APIResponse.<Void>error("An unexpected error occurred");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorResponse.ValidationError mapToValidationError(final FieldError fieldError) {
        return ErrorResponse.ValidationError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }

    private HttpStatus determineHttpStatus(final BaseUniversityAppException exception) {
        if (exception instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (exception instanceof InvalidArgsException || exception instanceof ClassSessionException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof ConflictException || exception instanceof DatabaseException) {
            return HttpStatus.CONFLICT;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
