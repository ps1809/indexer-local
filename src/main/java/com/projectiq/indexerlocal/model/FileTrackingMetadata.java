package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;

/**
 * Represents the tracking metadata for a single file during incremental indexing.
 * Used to detect changes between indexing runs.
 */
public class FileTrackingMetadata {

    private Long id;
    
    /** Repository ID this file belongs to */
    private String repositoryId;
    
    /** Absolute file path on disk */
    private String filePath;
    
    /** Relative path within the repository */
    private String relativePath;
    
    /** File name including extension */
    private String fileName;
    
    /** Last modified timestamp (from file system) */
    private LocalDateTime lastModified;
    
    /** File size in bytes */
    private Long fileSize;
    
    /** Simple checksum (first 8 chars of MD5, optional for MVP) */
    private String checksum;
    
    /** Index status: INDEXED, MODIFIED, DELETED, UNCHANGED */
    private String indexStatus;
    
    /** When this metadata was last updated */
    private LocalDateTime updatedAt;

    public FileTrackingMetadata() {
    }

    public FileTrackingMetadata(String repositoryId, String filePath, String relativePath, String fileName) {
        this.repositoryId = repositoryId;
        this.filePath = filePath;
        this.relativePath = relativePath;
        this.fileName = fileName;
        this.indexStatus = "UNCHANGED";
        this.updatedAt = LocalDateTime.now();
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(String indexStatus) {
        this.indexStatus = indexStatus;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}