package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;

/**
 * Represents generated documentation for a repository.
 * Stores the Markdown content and metadata about the generation.
 */
public class RepositoryDocumentation {

    private Long id;
    private String repositoryId;
    private String markdownContent;
    private LocalDateTime generatedAt;
    private Long contentSize;
    private String generationStatus;

    public RepositoryDocumentation() {
        this.generatedAt = LocalDateTime.now();
        this.generationStatus = "SUCCESS";
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

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
        this.contentSize = markdownContent != null ? (long) markdownContent.length() : 0L;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getContentSize() {
        return contentSize;
    }

    public void setContentSize(Long contentSize) {
        this.contentSize = contentSize;
    }

    public String getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(String generationStatus) {
        this.generationStatus = generationStatus;
    }
}