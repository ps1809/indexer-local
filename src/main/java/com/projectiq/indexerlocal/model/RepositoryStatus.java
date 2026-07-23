package com.projectiq.indexerlocal.model;

/**
 * Represents the status of a repository in its lifecycle.
 */
public enum RepositoryStatus {
    /**
     * Repository has been registered but not yet processed.
     */
    REGISTERED,

    /**
     * Repository is ready for indexing.
     */
    READY,

    /**
     * Repository is currently being indexed.
     */
    INDEXING,

    /**
     * Repository has been successfully indexed.
     */
    INDEXED,

    /**
     * Repository has encountered an error.
     */
    FAILED,

    /**
     * Repository metadata is being refreshed.
     */
    REFRESHING
}
