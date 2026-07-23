package com.projectiq.indexerlocal.model.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request model for searching indexed entities via chat API.
 */
@Schema(description = "Request for searching indexed entities")
public class ChatSearchRequest {

    @Schema(description = "Search query text (case-insensitive, supports exact and partial matching)", example = "UserService")
    private String query;

    @Schema(description = "Entity type filter (CLASS, INTERFACE, ENUM, RECORD, METHOD, FIELD, ANNOTATION, COMPONENT, ENDPOINT, DEPENDENCY, CONFIGURATION, DATABASE_ARTIFACT)", example = "CLASS")
    private String entityType;

    @Schema(description = "Package name filter", example = "com.example.service")
    private String packageName;

    @Schema(description = "Annotation filter", example = "Service")
    private String annotation;

    @Schema(description = "HTTP method filter for endpoints (GET, POST, PUT, DELETE, PATCH)", example = "GET")
    private String httpMethod;

    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    @Min(0)
    private Integer page = 0;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    @Min(1)
    @Max(100)
    private Integer size = 20;

    public ChatSearchRequest() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}