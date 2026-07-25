package com.projectiq.indexerlocal.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Statistics for the hash-based change detection engine.
 * Tracks performance metrics and file processing results.
 */
public class HashProcessingStatistics {

    /** Repository ID this statistics belongs to */
    private String repositoryId;

    /** Total files processed (hash computed) */
    private long filesProcessed;

    /** Total files skipped (hash already exists, unchanged) */
    private long filesSkipped;

    /** Total files detected as changed */
    private long filesChanged;

    /** Total SHA-256 hash calculations performed */
    private long hashCalculations;

    /** Estimated time saved by skipping unchanged files (in milliseconds) */
    private long timeSavedMs;

    /** Processing duration for the operation */
    private Duration processingDuration;

    /** When the operation started */
    private LocalDateTime startTime;

    /** When the operation completed */
    private LocalDateTime endTime;

    /** Status of the operation: SUCCESS, FAILED, IN_PROGRESS */
    private String status;

    /** Error message if the operation failed */
    private String errorMessage;

    public HashProcessingStatistics() {
        this.filesProcessed = 0;
        this.filesSkipped = 0;
        this.filesChanged = 0;
        this.hashCalculations = 0;
        this.timeSavedMs = 0;
        this.processingDuration = Duration.ZERO;
        this.status = "SUCCESS";
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public long getFilesProcessed() {
        return filesProcessed;
    }

    public void setFilesProcessed(long filesProcessed) {
        this.filesProcessed = filesProcessed;
    }

    public long getFilesSkipped() {
        return filesSkipped;
    }

    public void setFilesSkipped(long filesSkipped) {
        this.filesSkipped = filesSkipped;
    }

    public long getFilesChanged() {
        return filesChanged;
    }

    public void setFilesChanged(long filesChanged) {
        this.filesChanged = filesChanged;
    }

    public long getHashCalculations() {
        return hashCalculations;
    }

    public void setHashCalculations(long hashCalculations) {
        this.hashCalculations = hashCalculations;
    }

    public long getTimeSavedMs() {
        return timeSavedMs;
    }

    public void setTimeSavedMs(long timeSavedMs) {
        this.timeSavedMs = timeSavedMs;
    }

    public Duration getProcessingDuration() {
        return processingDuration;
    }

    public void setProcessingDuration(Duration processingDuration) {
        this.processingDuration = processingDuration;
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
     * Calculate total files scanned (processed + skipped).
     */
    public long getTotalFilesScanned() {
        return filesProcessed + filesSkipped;
    }

    /**
     * Format processing duration as human-readable string.
     */
    public String getFormattedProcessingDuration() {
        if (processingDuration == null) {
            return "N/A";
        }
        long hours = processingDuration.toHours();
        long minutes = processingDuration.toMinutes() % 60;
        long seconds = processingDuration.getSeconds() % 60;
        long millis = processingDuration.toMillis() % 1000;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else if (seconds > 0) {
            return String.format("%ds %dms", seconds, millis);
        } else {
            return String.format("%dms", millis);
        }
    }

    /**
     * Get the estimated time saved as a human-readable string.
     */
    public String getFormattedTimeSaved() {
        long seconds = timeSavedMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%ds", remainingSeconds);
        }
    }

    public void incrementFilesProcessed() {
        this.filesProcessed++;
    }

    public void incrementFilesSkipped() {
        this.filesSkipped++;
    }

    public void incrementFilesChanged() {
        this.filesChanged++;
    }

    public void incrementHashCalculations() {
        this.hashCalculations++;
    }

    public void addTimeSavedMs(long timeMs) {
        this.timeSavedMs += timeMs;
    }
}