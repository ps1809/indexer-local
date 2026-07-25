package com.projectiq.indexerlocal.model.event;

import java.time.Instant;
import java.util.Set;

/**
 * Statistics for the File Watch Service.
 */
public record FileWatchStats(
        int activeRepositories,
        int watchedDirectories,
        long eventsProcessed,
        int queueSize,
        Instant lastEventTimestamp
) {
}