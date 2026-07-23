package com.projectiq.indexerlocal.model.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response model for chat entity search.
 */
@Schema(description = "Response containing search results for indexed entities")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSearchResponse {

    @Schema(description = "Repository ID")
    private String repositoryId;

    @Schema(description = "Matching entities")
    private List<Map<String, Object>> entities;

    @Schema(description = "Total number of matching entities")
    private Long totalResults;

    @Schema(description = "Current page number")
    private Integer page;

    @Schema(description = "Page size")
    private Integer size;

    @Schema(description = "Total pages")
    private Integer totalPages;

    @Schema(description = "Entity type searched")
    private String entityTypeSearched;

    @Schema(description = "Query used")
    private String queryUsed;

    @Schema(description = "Generation timestamp")
    private LocalDateTime generatedAt;

    @Schema(description = "Execution duration in milliseconds")
    private Long executionDurationMs;

    public ChatSearchResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public List<Map<String, Object>> getEntities() {
        return entities;
    }

    public void setEntities(List<Map<String, Object>> entities) {
        this.entities = entities;
    }

    public Long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Long totalResults) {
        this.totalResults = totalResults;
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

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public String getEntityTypeSearched() {
        return entityTypeSearched;
    }

    public void setEntityTypeSearched(String entityTypeSearched) {
        this.entityTypeSearched = entityTypeSearched;
    }

    public String getQueryUsed() {
        return queryUsed;
    }

    public void setQueryUsed(String queryUsed) {
        this.queryUsed = queryUsed;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }
}