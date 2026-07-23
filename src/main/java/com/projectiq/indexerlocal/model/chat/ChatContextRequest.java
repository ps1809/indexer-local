package com.projectiq.indexerlocal.model.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * Request model for generating structured repository context.
 */
@Schema(description = "Request for generating repository context")
public class ChatContextRequest {

    @Schema(description = "List of entity types to include in context (CLASS, INTERFACE, ENUM, RECORD, METHOD, FIELD, ANNOTATION, COMPONENT, ENDPOINT, DEPENDENCY, CONFIGURATION, DATABASE_ARTIFACT)", example = "[\"CLASS\", \"METHOD\"]")
    private List<String> entityTypes;

    @Schema(description = "Optional query to filter context entities", example = "UserService")
    private String query;

    @Schema(description = "Maximum number of entities to return per type", example = "50", defaultValue = "50")
    @Min(1)
    @Max(500)
    private Integer maxEntitiesPerType = 50;

    @Schema(description = "Whether to include repository statistics", example = "true", defaultValue = "true")
    private Boolean includeStatistics = true;

    @Schema(description = "Whether to include project summary", example = "true", defaultValue = "true")
    private Boolean includeProjectSummary = true;

    @Schema(description = "Whether to include technology stack", example = "true", defaultValue = "true")
    private Boolean includeTechnologyStack = true;

    @Schema(description = "Whether to include build system info", example = "true", defaultValue = "true")
    private Boolean includeBuildSystem = true;

    @Schema(description = "Whether to include configuration summary", example = "true", defaultValue = "true")
    private Boolean includeConfigurationSummary = true;

    @Schema(description = "Whether to include database summary", example = "true", defaultValue = "true")
    private Boolean includeDatabaseSummary = true;

    @Schema(description = "Whether to include Spring component summary", example = "true", defaultValue = "true")
    private Boolean includeSpringComponentSummary = true;

    @Schema(description = "Whether to include REST API summary", example = "true", defaultValue = "true")
    private Boolean includeRestApiSummary = true;

    public ChatContextRequest() {
    }

    public List<String> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(List<String> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getMaxEntitiesPerType() {
        return maxEntitiesPerType;
    }

    public void setMaxEntitiesPerType(Integer maxEntitiesPerType) {
        this.maxEntitiesPerType = maxEntitiesPerType;
    }

    public Boolean getIncludeStatistics() {
        return includeStatistics;
    }

    public void setIncludeStatistics(Boolean includeStatistics) {
        this.includeStatistics = includeStatistics;
    }

    public Boolean getIncludeProjectSummary() {
        return includeProjectSummary;
    }

    public void setIncludeProjectSummary(Boolean includeProjectSummary) {
        this.includeProjectSummary = includeProjectSummary;
    }

    public Boolean getIncludeTechnologyStack() {
        return includeTechnologyStack;
    }

    public void setIncludeTechnologyStack(Boolean includeTechnologyStack) {
        this.includeTechnologyStack = includeTechnologyStack;
    }

    public Boolean getIncludeBuildSystem() {
        return includeBuildSystem;
    }

    public void setIncludeBuildSystem(Boolean includeBuildSystem) {
        this.includeBuildSystem = includeBuildSystem;
    }

    public Boolean getIncludeConfigurationSummary() {
        return includeConfigurationSummary;
    }

    public void setIncludeConfigurationSummary(Boolean includeConfigurationSummary) {
        this.includeConfigurationSummary = includeConfigurationSummary;
    }

    public Boolean getIncludeDatabaseSummary() {
        return includeDatabaseSummary;
    }

    public void setIncludeDatabaseSummary(Boolean includeDatabaseSummary) {
        this.includeDatabaseSummary = includeDatabaseSummary;
    }

    public Boolean getIncludeSpringComponentSummary() {
        return includeSpringComponentSummary;
    }

    public void setIncludeSpringComponentSummary(Boolean includeSpringComponentSummary) {
        this.includeSpringComponentSummary = includeSpringComponentSummary;
    }

    public Boolean getIncludeRestApiSummary() {
        return includeRestApiSummary;
    }

    public void setIncludeRestApiSummary(Boolean includeRestApiSummary) {
        this.includeRestApiSummary = includeRestApiSummary;
    }
}