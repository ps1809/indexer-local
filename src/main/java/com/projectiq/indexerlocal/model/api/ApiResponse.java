package com.projectiq.indexerlocal.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Standardized API response model for all v1 endpoints.
 */
@Schema(description = "Standard API response wrapper")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Schema(description = "HTTP status code", example = "200")
    private int status;

    @Schema(description = "Response message", example = "Success")
    private String message;

    @Schema(description = "Response data payload")
    private T data;

    @Schema(description = "Error details when status indicates failure")
    private ErrorDetail error;

    @Schema(description = "Timestamp when response was generated", example = "2024-01-01T12:00:00")
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(int status, String message, ErrorDetail error) {
        this.status = status;
        this.message = message;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    // Success responses
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "Created", data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(201, message, data);
    }

    public static <T> ApiResponse<T> successWithMessage(String message) {
        return new ApiResponse<>(200, message, null);
    }

    public static <T> ApiResponse<T> deleted() {
        return new ApiResponse<>(204, "Deleted", null);
    }

    // Error responses
    public static ApiResponse<Object> notFound(String resource) {
        ErrorDetail error = new ErrorDetail(404, "Resource Not Found", resource + " was not found");
        return new ApiResponse<>(404, "Resource Not Found", error);
    }

    public static ApiResponse<Object> badRequest(String field, String message) {
        ErrorDetail error = new ErrorDetail(400, "Bad Request", field + ": " + message);
        return new ApiResponse<>(400, "Bad Request", error);
    }

    public static ApiResponse<Object> internalError(String message) {
        ErrorDetail error = new ErrorDetail(500, "Internal Server Error", message);
        return new ApiResponse<>(500, "Internal Server Error", error);
    }

    // Getters and setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Error detail wrapper for error responses.
     */
    @Schema(description = "Error details for failed requests")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {

        @Schema(description = "HTTP status code", example = "404")
        private int code;

        @Schema(description = "Error type", example = "Resource Not Found")
        private String type;

        @Schema(description = "Detailed error message", example = "File not found with id: 123")
        private String message;

        public ErrorDetail() {
        }

        public ErrorDetail(int code, String type, String message) {
            this.code = code;
            this.type = type;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}