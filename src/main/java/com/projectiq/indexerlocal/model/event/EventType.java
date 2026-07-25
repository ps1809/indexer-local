package com.projectiq.indexerlocal.model.event;

/**
 * Represents the type of file system event detected by the Watch Service.
 */
public enum EventType {
    CREATED,
    MODIFIED,
    DELETED,
    RENAMED
}