package com.projectiq.indexerlocal.exception;

import com.projectiq.indexerlocal.model.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.NoSuchElementException;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across the application.
 * 
 * <p>Handles common failure scenarios including:</p>
 * <ul>
 *   <li>Validation errors from @Valid annotated DTOs</li>
 *   <li>Malformed JSON request bodies</li>
 *   <li>Type mismatch errors in request parameters</li>
 *   <li>Resource not found exceptions</li>
 *   <li>Access denied and authorization failures</li>
 *   <li>I/O and file system errors</li>
 *   <li>General unhandled exceptions</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== Validation Errors ====================

    /**
     * Handles validation errors from @Valid annotated DTOs.
     * Collects all field-level validation errors into a structured response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        StringBuilder messageBuilder = new StringBuilder("Validation failed: ");
        int errorCount = 0;
        for (var error : ex.getBindingResult().getAllErrors()) {
            String field = error.getCodes() != null && error.getCodes().length > 0 
                    ? error.getCodes()[0] : "unknown";
            messageBuilder.append(field).append(":").append(error.getDefaultMessage()).append("; ");
            errorCount++;
        }

        log.warn("Validation failed ({} errors): {}", errorCount, messageBuilder.toString());

        ApiResponse<Object> response = ApiResponse.badRequest(
            "validation",
            messageBuilder.toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== Bad Request Errors ====================

    /**
     * Handles malformed JSON request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {
        
        String message = "Malformed request body: " + ex.getMessage();
        log.warn("Bad request: {}", message);

        ApiResponse<Object> response = new ApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            new ApiResponse.ErrorDetail(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message
            )
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles file upload size limit exceeded.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex) {
        
        log.warn("File upload size exceeds maximum allowed size");

        ApiResponse<Object> response = new ApiResponse<>(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "Payload Too Large",
            new ApiResponse.ErrorDetail(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "FileTooLarge",
                "Uploaded file exceeds the maximum allowed size"
            )
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    /**
     * Handles type mismatch errors in request parameters.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        
        String message = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();
        log.warn("Type mismatch: {}", message);

        ApiResponse<Object> response = new ApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            new ApiResponse.ErrorDetail(
                HttpStatus.BAD_REQUEST.value(),
                "InvalidParameter",
                message
            )
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles invalid argument values (e.g., null where not expected).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        
        log.warn("Illegal argument: {}", ex.getMessage());

        ApiResponse<Object> response = new ApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            new ApiResponse.ErrorDetail(
                HttpStatus.BAD_REQUEST.value(),
                "InvalidArgument",
                ex.getMessage() != null ? ex.getMessage() : "Invalid argument value"
            )
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== Resource Not Found Errors ====================

    /**
     * Handles resource not found exceptions.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFoundException(
            NoSuchElementException ex) {
        
        log.warn("Resource not found: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.notFound("resource");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles missing file errors.
     */
    @ExceptionHandler(NoSuchFileException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoSuchFile(
            NoSuchFileException ex) {
        
        log.warn("File not found: {}", ex.getFile());

        ApiResponse<Object> response = new ApiResponse<>(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            new ApiResponse.ErrorDetail(
                HttpStatus.NOT_FOUND.value(),
                "FileNotFound",
                "Requested file does not exist: " + ex.getFile()
            )
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ==================== Access Control Errors ====================

    /**
     * Handles access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex) {
        
        log.warn("Access denied: {}", ex.getMessage());

        ApiResponse<Object> response = new ApiResponse<>(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            new ApiResponse.ErrorDetail(
                HttpStatus.FORBIDDEN.value(),
                "AccessDenied",
                "Access to the requested resource is denied"
            )
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ==================== I/O Errors ====================

    /**
     * Handles general I/O exceptions.
     */
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ApiResponse<Object>> handleIOException(
            java.io.IOException ex) {
        
        log.error("I/O error: {}", ex.getMessage(), ex);

        ApiResponse<Object> response = new ApiResponse<>(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            new ApiResponse.ErrorDetail(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "IOError",
                "An I/O error occurred: " + ex.getMessage()
            )
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ==================== System Errors ====================

    /**
     * Handles null pointer exceptions (should not happen in production code).
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Object>> handleNullPointerException(
            NullPointerException ex) {
        
        log.error("NullPointerException (unexpected): {}", ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.internalError("Internal server error occurred");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles general exceptions not specifically caught above.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.internalError(
            "An unexpected error occurred: " + sanitizeExceptionMessage(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Sanitizes exception messages to prevent leaking internal details.
     */
    private String sanitizeExceptionMessage(String message) {
        if (message == null) {
            return "An unexpected error occurred";
        }
        // Limit message length and remove potential path information
        if (message.length() > 200) {
            return message.substring(0, 200) + "...";
        }
        return message;
    }
}