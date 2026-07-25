package com.projectiq.indexerlocal.model.repositorysearch;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Search result model for the Repository Search Engine.
 * Contains all information about a discovered repository resource.
 */
@Schema(description = "Repository search result containing resource metadata")
public class RepositorySearchResult {

    @Schema(description = "Repository name", example = "my-project")
    private String repositoryName;

    @Schema(description = "Repository ID", example = "repo-123")
    private String repositoryId;

    @Schema(description = "Absolute path on disk", example = "C:/projects/my-project")
    private String absolutePath;

    @Schema(description = "Relative path within the repository", example = "src/main/java/com/example/App.java")
    private String relativePath;

    @Schema(description = "File or directory name", example = "App.java")
    private String name;

    @Schema(description = "Type of resource: FILE, FOLDER, or REPOSITORY", example = "FILE")
    private String resourceType;

    @Schema(description = "File extension (for files)", example = "java")
    private String extension;

    @Schema(description = "Programming language (for files)", example = "Java")
    private String language;

    @Schema(description = "Module name (if applicable)", example = "core")
    private String module;

    @Schema(description = "File size in bytes", example = "2048")
    private Long fileSize;

    @Schema(description = "Last modified timestamp")
    private LocalDateTime lastModified;

    @Schema(description = "Indexed timestamp")
    private LocalDateTime indexedTimestamp;

    @Schema(description = "Directory classification (for folders)", example = "SOURCE_ROOT")
    private String directoryClassification;

    @Schema(description = "File classification (for files)", example = "SOURCE_FILE")
    private String fileClassification;

    @Schema(description = "Depth from repository root", example = "3")
    private Integer depth;

    @Schema(description = "Whether the resource is hidden", example = "false")
    private Boolean isHidden;

    @Schema(description = "List of languages found in the repository (for repository-level results)")
    private List<String> languages;

    @Schema(description = "List of file extensions found in the repository (for repository-level results)")
    private List<String> extensions;

    @Schema(description = "Repository status", example = "INDEXED")
    private String repositoryStatus;

    public RepositorySearchResult() {
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public LocalDateTime getIndexedTimestamp() {
        return indexedTimestamp;
    }

    public void setIndexedTimestamp(LocalDateTime indexedTimestamp) {
        this.indexedTimestamp = indexedTimestamp;
    }

    public String getDirectoryClassification() {
        return directoryClassification;
    }

    public void setDirectoryClassification(String directoryClassification) {
        this.directoryClassification = directoryClassification;
    }

    public String getFileClassification() {
        return fileClassification;
    }

    public void setFileClassification(String fileClassification) {
        this.fileClassification = fileClassification;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Boolean getHidden() {
        return isHidden;
    }

    public void setHidden(Boolean hidden) {
        isHidden = hidden;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    public String getRepositoryStatus() {
        return repositoryStatus;
    }

    public void setRepositoryStatus(String repositoryStatus) {
        this.repositoryStatus = repositoryStatus;
    }
}