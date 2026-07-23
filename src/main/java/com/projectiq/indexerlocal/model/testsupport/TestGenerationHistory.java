package com.projectiq.indexerlocal.model.testsupport;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Represents a test generation request history record.
 */
@Schema(description = "Test generation request history")
public class TestGenerationHistory {

    @Schema(description = "History record ID")
    private Long id;

    @Schema(description = "Repository ID")
    private String repositoryId;

    @Schema(description = "Request type (CLASSES, CLASS_DETAIL, METHOD_DETAIL, CONTEXT)")
    private String requestType;

    @Schema(description = "Request parameters as JSON")
    private String requestParameters;

    @Schema(description = "Number of classes retrieved")
    private int classesRetrieved;

    @Schema(description = "Number of methods retrieved")
    private int methodsRetrieved;

    @Schema(description = "Response size in bytes")
    private long responseSizeBytes;

    @Schema(description = "Execution duration in milliseconds")
    private long executionDurationMs;

    @Schema(description = "Request timestamp")
    private LocalDateTime requestedAt;

    @Schema(description = "Request status (SUCCESS, ERROR)")
    private String status;

    @Schema(description = "Error message if failed")
    private String errorMessage;

    public TestGenerationHistory() {
        this.requestedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestParameters() {
        return requestParameters;
    }

    public void setRequestParameters(String requestParameters) {
        this.requestParameters = requestParameters;
    }

    public int getClassesRetrieved() {
        return classesRetrieved;
    }

    public void setClassesRetrieved(int classesRetrieved) {
        this.classesRetrieved = classesRetrieved;
    }

    public int getMethodsRetrieved() {
        return methodsRetrieved;
    }

    public void setMethodsRetrieved(int methodsRetrieved) {
        this.methodsRetrieved = methodsRetrieved;
    }

    public long getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public void setResponseSizeBytes(long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }

    public long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}