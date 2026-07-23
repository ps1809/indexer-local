package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;

/**
 * Represents a repository refresh history entry.
 * Tracks when and how a repository was refreshed or re-indexed.
 */
public class RepositoryRefreshHistory {

    private Long id;
    private String repositoryId;
    private String refreshType; // REFRESH, FULL_REINDEX, INCREMENTAL_REINDEX
    private String status; // SUCCESS, FAILED, IN_PROGRESS
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long executionDurationMs;
    private String errorMessage;
    private LocalDateTime createdAt;

    public RepositoryRefreshHistory() {
        this.createdAt = LocalDateTime.now();
    }

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

    public String getRefreshType() {
        return refreshType;
    }

    public void setRefreshType(String refreshType) {
        this.refreshType = refreshType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}