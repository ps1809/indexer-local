package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;

/**
 * Represents a persistent SHA-256 hash record for a single file.
 * Used by the Hash Engine to detect changes between indexing runs.
 */
public class FileHashRecord {

    private Long id;

    /** Repository ID this file belongs to */
    private String repositoryId;

    /** Absolute file path on disk */
    private String filePath;

    /** SHA-256 hash of the file content */
    private String sha256Hash;

    /** File size in bytes at last hash */
    private long fileSize;

    /** Last modified timestamp from the file system */
    private LocalDateTime lastModified;

    /** When this file was last indexed */
    private LocalDateTime lastIndexedAt;

    /** Processing status: PENDING, PROCESSING, INDEXED, FAILED */
    private String processingStatus;

    /** When this record was created */
    private LocalDateTime createdAt;

    /** When this record was last updated */
    private LocalDateTime updatedAt;

    public FileHashRecord() {
        this.processingStatus = "PENDING";
    }

    public FileHashRecord(String repositoryId, String filePath) {
        this.repositoryId = repositoryId;
        this.filePath = filePath;
        this.processingStatus = "PENDING";
        this.createdAt = LocalDateTime.now();
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

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public LocalDateTime getLastIndexedAt() {
        return lastIndexedAt;
    }

    public void setLastIndexedAt(LocalDateTime lastIndexedAt) {
        this.lastIndexedAt = lastIndexedAt;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Check if the file has been modified compared to stored metadata.
     *
     * @param currentLastModified the current last modified timestamp
     * @param currentFileSize the current file size in bytes
     * @return true if the file is potentially modified based on metadata
     */
    public boolean isPotentiallyModified(LocalDateTime currentLastModified, long currentFileSize) {
        if (this.lastModified == null) {
            return true;
        }
        if (this.fileSize != currentFileSize) {
            return true;
        }
        return !this.lastModified.equals(currentLastModified);
    }

    /**
     * Determine if this record represents a change in hash.
     *
     * @param newHash the newly computed SHA-256 hash
     * @return true if the hash has changed
     */
    public boolean hasHashChanged(String newHash) {
        return this.sha256Hash == null || !this.sha256Hash.equals(newHash);
    }

    @Override
    public String toString() {
        return "FileHashRecord{" +
                "id=" + id +
                ", repositoryId='" + repositoryId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", processingStatus='" + processingStatus + '\'' +
                '}';
    }
}