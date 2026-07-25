package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.event.FileWatchStats;
import com.projectiq.indexerlocal.model.event.WatchEvent;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * Interface for the File Watch Service that monitors indexed repositories
 * for file system changes using Java NIO WatchService.
 */
public interface FileWatchService {

    /**
     * Registers a repository for file watching.
     *
     * @param repositoryId the unique repository identifier
     * @param repositoryPath the root path to monitor
     * @throws IllegalArgumentException if repositoryId or repositoryPath is invalid
     */
    void registerRepository(String repositoryId, Path repositoryPath);

    /**
     * Unregisters a repository and stops watching it.
     *
     * @param repositoryId the unique repository identifier
     * @return true if the repository was unregistered, false if not found
     */
    boolean unregisterRepository(String repositoryId);

    /**
     * Starts watching all registered repositories.
     * Must be called after registration and before events can be captured.
     */
    void startWatching();

    /**
     * Stops watching all registered repositories.
     */
    void stopWatching();

    /**
     * Starts watching a specific registered repository.
     *
     * @param repositoryId the unique repository identifier
     * @return true if the watcher was started, false if not registered
     */
    boolean startRepositoryWatcher(String repositoryId);

    /**
     * Stops watching a specific registered repository.
     *
     * @param repositoryId the unique repository identifier
     * @return true if the watcher was stopped, false if not found
     */
    boolean stopRepositoryWatcher(String repositoryId);

    /**
     * Restarts the watcher for a specific repository.
     * Stops and then starts watching again.
     *
     * @param repositoryId the unique repository identifier
     * @return true if restarted successfully
     */
    boolean restartRepositoryWatcher(String repositoryId);

    /**
     * Captures a watch event from the queue (non-blocking).
     *
     * @return the next WatchEvent, or null if queue is empty
     */
    WatchEvent captureEvent();

    /**
     * Captures a watch event from the queue (blocking with timeout).
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit
     * @return the next WatchEvent, or null if timeout expires
     * @throws InterruptedException if the thread is interrupted
     */
    WatchEvent captureEvent(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException;

    /**
     * Returns the current watch statistics.
     */
    FileWatchStats getStatistics();

    /**
     * Returns true if the watch service is currently running.
     */
    boolean isRunning();

    /**
     * Returns the set of registered repository IDs.
     */
    Set<String> getRegisteredRepositories();

    /**
     * Returns the set of watched directory paths for a repository.
     *
     * @param repositoryId the repository ID
     * @return set of watched directory paths
     */
    Set<String> getWatchedDirectories(String repositoryId);

    /**
     * Checks if a file path should be ignored based on current filtering rules.
     *
     * @param path the path to check
     * @return true if the path should be ignored
     */
    boolean shouldIgnorePath(Path path);
}