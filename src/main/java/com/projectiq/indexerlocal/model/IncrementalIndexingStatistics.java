package com.projectiq.indexerlocal.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Statistics for incremental indexing operations.
 * Tracks the results of each incremental indexing run.
 */
public class IncrementalIndexingStatistics {

    /** Repository ID this statistics belongs to */
    private String repositoryId;
    
    /** Number of files added (newly indexed) */
    private long filesAdded;
    
    /** Number of files modified (re-indexed) */
    private long filesModified;
    
    /** Number of files deleted (removed from index) */
    private long filesDeleted;
    
    /** Number of files unchanged (skipped) */
    private long filesUnchanged;
    
    /** Total files processed (added + modified) */
    private long totalFilesProcessed;
    
    /** Time taken for the indexing operation */
    private Duration processingTime;
    
    /** When the last incremental indexing started */
    private LocalDateTime startTime;
    
    /** When the last incremental indexing completed */
    private LocalDateTime endTime;
    
    /** Status of the last incremental indexing: SUCCESS, FAILED, IN_PROGRESS */
    private String status;
    
    /** Error message if indexing failed */
    private String errorMessage;

    public IncrementalIndexingStatistics() {
        this.filesAdded = 0;
        this.filesModified = 0;
        this.filesDeleted = 0;
        this.filesUnchanged = 0;
        this.totalFilesProcessed = 0;
        this.processingTime = Duration.ZERO;
        this.status = "SUCCESS";
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public long getFilesAdded() {
        return filesAdded;
    }

    public void setFilesAdded(long filesAdded) {
        this.filesAdded = filesAdded;
    }

    public long getFilesModified() {
        return filesModified;
    }

    public void setFilesModified(long filesModified) {
        this.filesModified = filesModified;
    }

    public long getFilesDeleted() {
        return filesDeleted;
    }

    public void setFilesDeleted(long filesDeleted) {
        this.filesDeleted = filesDeleted;
    }

    public long getFilesUnchanged() {
        return filesUnchanged;
    }

    public void setFilesUnchanged(long filesUnchanged) {
        this.filesUnchanged = filesUnchanged;
    }

    public long getTotalFilesProcessed() {
        return totalFilesProcessed;
    }

    public void setTotalFilesProcessed(long totalFilesProcessed) {
        this.totalFilesProcessed = totalFilesProcessed;
    }

    public Duration getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Duration processingTime) {
        this.processingTime = processingTime;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Calculate total files scanned (processed + unchanged).
     */
    public long getTotalFilesScanned() {
        return totalFilesProcessed + filesUnchanged + filesDeleted;
    }

    /**
     * Format processing time as human-readable string.
     */
    public String getFormattedProcessingTime() {
        if (processingTime == null) {
            return "N/A";
        }
        long hours = processingTime.toHours();
        long minutes = processingTime.toMinutes() % 60;
        long seconds = processingTime.getSeconds() % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}