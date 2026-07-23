package com.projectiq.indexerlocal.model.chat;

import java.time.LocalDateTime;

/**
 * Entity representing a persisted chat request history record.
 */
public class ChatHistory {

    private Long id;
    private String repositoryId;
    private String requestType;
    private String query;
    private String entityTypes;
    private Integer entitiesRetrieved;
    private Long executionDurationMs;
    private LocalDateTime requestTimestamp;

    public ChatHistory() {
        this.requestTimestamp = LocalDateTime.now();
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

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(String entityTypes) {
        this.entityTypes = entityTypes;
    }

    public Integer getEntitiesRetrieved() {
        return entitiesRetrieved;
    }

    public void setEntitiesRetrieved(Integer entitiesRetrieved) {
        this.entitiesRetrieved = entitiesRetrieved;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }
}