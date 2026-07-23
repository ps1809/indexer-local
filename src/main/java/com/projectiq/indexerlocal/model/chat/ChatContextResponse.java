package com.projectiq.indexerlocal.model.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response model for repository context generation.
 * Contains structured context objects built from persisted indexed metadata.
 */
@Schema(description = "Response containing structured repository context")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatContextResponse {

    @Schema(description = "Repository metadata")
    private RepositoryMetadata repositoryMetadata;

    @Schema(description = "Matching entities grouped by type")
    private Map<String, List<Map<String, Object>>> entities;

    @Schema(description = "Context summary information")
    private ContextSummary contextSummary;

    @Schema(description = "Repository statistics")
    private Map<String, Object> statistics;

    @Schema(description = "Generation timestamp")
    private LocalDateTime generatedAt;

    @Schema(description = "Execution duration in milliseconds")
    private Long executionDurationMs;

    public ChatContextResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    public RepositoryMetadata getRepositoryMetadata() {
        return repositoryMetadata;
    }

    public void setRepositoryMetadata(RepositoryMetadata repositoryMetadata) {
        this.repositoryMetadata = repositoryMetadata;
    }

    public Map<String, List<Map<String, Object>>> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, List<Map<String, Object>>> entities) {
        this.entities = entities;
    }

    public ContextSummary getContextSummary() {
        return contextSummary;
    }

    public void setContextSummary(ContextSummary contextSummary) {
        this.contextSummary = contextSummary;
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
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

    /**
     * Repository metadata included in the context response.
     */
    @Schema(description = "Repository metadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RepositoryMetadata {

        @Schema(description = "Repository ID")
        private String repositoryId;

        @Schema(description = "Repository name")
        private String repositoryName;

        @Schema(description = "Build system")
        private String buildSystem;

        @Schema(description = "Technology stack")
        private String technologyStack;

        @Schema(description = "Project summary")
        private String projectSummary;

        @Schema(description = "Configuration summary")
        private Map<String, Object> configurationSummary;

        @Schema(description = "Database summary")
        private Map<String, Object> databaseSummary;

        @Schema(description = "Spring component summary")
        private Map<String, Object> springComponentSummary;

        @Schema(description = "REST API summary")
        private Map<String, Object> restApiSummary;

        public String getRepositoryId() {
            return repositoryId;
        }

        public void setRepositoryId(String repositoryId) {
            this.repositoryId = repositoryId;
        }

        public String getRepositoryName() {
            return repositoryName;
        }

        public void setRepositoryName(String repositoryName) {
            this.repositoryName = repositoryName;
        }

        public String getBuildSystem() {
            return buildSystem;
        }

        public void setBuildSystem(String buildSystem) {
            this.buildSystem = buildSystem;
        }

        public String getTechnologyStack() {
            return technologyStack;
        }

        public void setTechnologyStack(String technologyStack) {
            this.technologyStack = technologyStack;
        }

        public String getProjectSummary() {
            return projectSummary;
        }

        public void setProjectSummary(String projectSummary) {
            this.projectSummary = projectSummary;
        }

        public Map<String, Object> getConfigurationSummary() {
            return configurationSummary;
        }

        public void setConfigurationSummary(Map<String, Object> configurationSummary) {
            this.configurationSummary = configurationSummary;
        }

        public Map<String, Object> getDatabaseSummary() {
            return databaseSummary;
        }

        public void setDatabaseSummary(Map<String, Object> databaseSummary) {
            this.databaseSummary = databaseSummary;
        }

        public Map<String, Object> getSpringComponentSummary() {
            return springComponentSummary;
        }

        public void setSpringComponentSummary(Map<String, Object> springComponentSummary) {
            this.springComponentSummary = springComponentSummary;
        }

        public Map<String, Object> getRestApiSummary() {
            return restApiSummary;
        }

        public void setRestApiSummary(Map<String, Object> restApiSummary) {
            this.restApiSummary = restApiSummary;
        }
    }

    /**
     * Context summary with counts and high-level information.
     */
    @Schema(description = "Context summary")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContextSummary {

        @Schema(description = "Total number of entities retrieved")
        private Integer totalEntitiesRetrieved;

        @Schema(description = "Entity types included in context")
        private List<String> entityTypesIncluded;

        @Schema(description = "Entity counts by type")
        private Map<String, Integer> entityCountsByType;

        @Schema(description = "Query used for filtering (if any)")
        private String queryUsed;

        @Schema(description = "Maximum entities per type limit applied")
        private Integer maxEntitiesPerType;

        public Integer getTotalEntitiesRetrieved() {
            return totalEntitiesRetrieved;
        }

        public void setTotalEntitiesRetrieved(Integer totalEntitiesRetrieved) {
            this.totalEntitiesRetrieved = totalEntitiesRetrieved;
        }

        public List<String> getEntityTypesIncluded() {
            return entityTypesIncluded;
        }

        public void setEntityTypesIncluded(List<String> entityTypesIncluded) {
            this.entityTypesIncluded = entityTypesIncluded;
        }

        public Map<String, Integer> getEntityCountsByType() {
            return entityCountsByType;
        }

        public void setEntityCountsByType(Map<String, Integer> entityCountsByType) {
            this.entityCountsByType = entityCountsByType;
        }

        public String getQueryUsed() {
            return queryUsed;
        }

        public void setQueryUsed(String queryUsed) {
            this.queryUsed = queryUsed;
        }

        public Integer getMaxEntitiesPerType() {
            return maxEntitiesPerType;
        }

        public void setMaxEntitiesPerType(Integer maxEntitiesPerType) {
            this.maxEntitiesPerType = maxEntitiesPerType;
        }
    }
}