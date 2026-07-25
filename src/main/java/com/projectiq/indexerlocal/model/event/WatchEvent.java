package com.projectiq.indexerlocal.model.event;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a file system watch event captured from the NIO WatchService.
 * Carries all necessary metadata for incremental indexing integration.
 */
public class WatchEvent {

    private final String repositoryId;
    private final Path absoluteFilePath;
    private final String relativePath;
    private final EventType eventType;
    private final Instant timestamp;
    private final String directoryPath;

    public WatchEvent(String repositoryId,
                      Path absoluteFilePath,
                      String relativePath,
                      EventType eventType,
                      Instant timestamp,
                      String directoryPath) {
        this.repositoryId = Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        this.absoluteFilePath = absoluteFilePath;
        this.relativePath = relativePath;
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.directoryPath = directoryPath;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public Path getAbsoluteFilePath() {
        return absoluteFilePath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    /**
     * Returns true if this is a file creation event.
     */
    public boolean isCreated() {
        return eventType == EventType.CREATED;
    }

    /**
     * Returns true if this is a file modification event.
     */
    public boolean isModified() {
        return eventType == EventType.MODIFIED;
    }

    /**
     * Returns true if this is a file deletion event.
     */
    public boolean isDeleted() {
        return eventType == EventType.DELETED;
    }

    /**
     * Returns true if this is a file rename event.
     */
    public boolean isRenamed() {
        return eventType == EventType.RENAMED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchEvent that = (WatchEvent) o;
        return repositoryId.equals(that.repositoryId) &&
                Objects.equals(absoluteFilePath, that.absoluteFilePath) &&
                eventType == that.eventType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositoryId, absoluteFilePath, eventType);
    }

    @Override
    public String toString() {
        return "WatchEvent{" +
                "repositoryId='" + repositoryId + '\'' +
                ", filePath='" + relativePath + '\'' +
                ", eventType=" + eventType +
                ", timestamp=" + timestamp +
                '}';
    }
}