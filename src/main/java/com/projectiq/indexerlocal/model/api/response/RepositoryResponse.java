package com.projectiq.indexerlocal.model.api.response;

import com.projectiq.indexerlocal.model.RepositoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response body for repository information.
 */
@Schema(description = "Repository information response")
public class RepositoryResponse {

    @Schema(description = "Internal ID", example = "1")
    private Long id;

    @Schema(description = "Unique repository identifier", example = "repo_a1b2c3d4")
    private String repositoryId;

    @Schema(description = "Repository name", example = "my-app")
    private String repositoryName;

    @Schema(description = "Original filesystem path", example = "/home/user/projects/my-app")
    private String originalPath;

    @Schema(description = "Workspace path", example = "/workspace/my-app")
    private String workspacePath;

    @Schema(description = "Registration timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime registrationTimestamp;

    @Schema(description = "Last updated timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime lastUpdatedTimestamp;

    @Schema(description = "Current repository status", example = "REGISTERED")
    private RepositoryStatus status;

    @Schema(description = "Build system type", example = "Unknown")
    private String buildSystem;

    @Schema(description = "Technology stack", example = "Unknown")
    private String technologyStack;

    public RepositoryResponse() {
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

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public LocalDateTime getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public void setRegistrationTimestamp(LocalDateTime registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    public LocalDateTime getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(LocalDateTime lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    public RepositoryStatus getStatus() {
        return status;
    }

    public void setStatus(RepositoryStatus status) {
        this.status = status;
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
}