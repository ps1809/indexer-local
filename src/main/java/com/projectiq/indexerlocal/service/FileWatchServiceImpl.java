package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.config.WatchServiceProperties;
import com.projectiq.indexerlocal.model.event.FileWatchStats;
import com.projectiq.indexerlocal.model.event.EventType;
import com.projectiq.indexerlocal.model.event.WatchEvent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Implementation of FileWatchService that uses Java NIO WatchService
 * to monitor indexed repositories for file system changes.
 */
@Service
public class FileWatchServiceImpl implements FileWatchService {

    private static final Logger logger = LoggerFactory.getLogger(FileWatchServiceImpl.class);

    private final WatchService watchService;
    private final WatchServiceProperties properties;
    private final FileWatchProperties fileWatchProperties;
    private final WatchEventQueue eventQueue;

    // Repository registration: repositoryId -> repositoryPath
    private final Map<String, Path> registeredRepositories = new ConcurrentHashMap<>();
    // Repository watcher threads: repositoryId -> watcher thread
    private final Map<String, ScheduledFuture<?>> watcherThreads = new ConcurrentHashMap<>();
    // Repository watched directories: repositoryId -> set of directory paths being watched
    private final Map<String, Set<Path>> watchedDirectories = new ConcurrentHashMap<>();

    // Locks for thread-safe operations
    private final ReentrantReadWriteLock repoLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock watchDirLock = new ReentrantReadWriteLock();

    // Statistics counters
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong eventsIgnored = new AtomicLong(0);
    private final AtomicLong lastEventTimestamp = new AtomicLong(0);

    // Service lifecycle
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Scheduled executor for watcher threads
    private ScheduledExecutorService scheduler;

    public FileWatchServiceImpl(@Qualifier("watchServiceProperties") WatchServiceProperties properties) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.properties = properties;
        this.fileWatchProperties = new FileWatchProperties(
                properties.getIgnoredDirectories(),
                properties.getIgnoreFilePatterns(),
                properties.getMaxQueueSize(),
                properties.getDebounceDelay(),
                properties.isEnabled()
        );
        this.eventQueue = new WatchEventQueue(properties.getMaxQueueSize());
    }

    /**
     * Starts the watch service.
     */
    @Override
    public void startWatching() {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting File Watch Service...");
            scheduler = Executors.newScheduledThreadPool(4);

            // Start watching all registered repositories
            repoLock.readLock().lock();
            try {
                for (String repoId : registeredRepositories.keySet()) {
                    startRepositoryWatcher(repoId);
                }
            } finally {
                repoLock.readLock().unlock();
            }

            logger.info("File Watch Service started successfully.");
        }
    }

    /**
     * Stops the watch service.
     */
    @Override
    public void stopWatching() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping File Watch Service...");

            // Stop all watcher threads
            for (Map.Entry<String, ScheduledFuture<?>> entry : watcherThreads.entrySet()) {
                entry.getValue().cancel(true);
                logger.info("Stopped watching repository: {}", entry.getKey());
            }
            watcherThreads.clear();

            // Reset watched directories
            watchDirLock.writeLock().lock();
            try {
                for (Set<Path> dirs : watchedDirectories.values()) {
                    dirs.clear();
                }
                watchedDirectories.clear();
            } finally {
                watchDirLock.writeLock().unlock();
            }

            // Shutdown scheduler
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            logger.info("File Watch Service stopped.");
        }
    }

    /**
     * Registers a repository for file watching.
     */
    @Override
    public void registerRepository(String repositoryId, Path repositoryPath) {
        if (repositoryId == null || repositoryId.isEmpty()) {
            throw new IllegalArgumentException("repositoryId must not be null or empty");
        }
        if (repositoryPath == null) {
            throw new IllegalArgumentException("repositoryPath must not be null");
        }

        // Normalize the path
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();

        // Validate the path exists and is a directory
        if (!Files.exists(normalizedPath)) {
            throw new IllegalArgumentException("Repository path does not exist: " + normalizedPath);
        }
        if (!Files.isDirectory(normalizedPath)) {
            throw new IllegalArgumentException("Repository path is not a directory: " + normalizedPath);
        }

        // Check read permissions
        if (!Files.isReadable(normalizedPath)) {
            throw new IllegalArgumentException("Repository path is not readable: " + normalizedPath);
        }

        repoLock.writeLock().lock();
        try {
            registeredRepositories.put(repositoryId, normalizedPath);
            logger.info("Registered repository: {} at {}", repositoryId, normalizedPath);
        } finally {
            repoLock.writeLock().unlock();
        }
    }

    /**
     * Unregisters a repository and stops watching it.
     */
    @Override
    public boolean unregisterRepository(String repositoryId) {
        // Stop watcher first
        boolean stopped = stopRepositoryWatcher(repositoryId);

        repoLock.writeLock().lock();
        Path removedPath = registeredRepositories.remove(repositoryId);
        repoLock.writeLock().unlock();

        if (removedPath != null) {
            // Remove from watched directories
            watchDirLock.writeLock().lock();
            try {
                watchedDirectories.remove(repositoryId);
            } finally {
                watchDirLock.writeLock().unlock();
            }

            logger.info("Unregistered repository: {} (path: {})", repositoryId, removedPath);
            return true;
        }
        return false;
    }

    /**
     * Starts watching a specific registered repository.
     */
    @Override
    public boolean startRepositoryWatcher(String repositoryId) {
        if (!running.get()) {
            logger.warn("Cannot start watcher for repository {}: service is not running", repositoryId);
            return false;
        }

        // Check if already watching
        if (watcherThreads.containsKey(repositoryId)) {
            logger.warn("Watcher already running for repository: {}", repositoryId);
            return false;
        }

        repoLock.readLock().lock();
        Path repositoryPath;
        try {
            repositoryPath = registeredRepositories.get(repositoryId);
        } finally {
            repoLock.readLock().unlock();
        }

        if (repositoryPath == null) {
            logger.warn("Repository not registered: {}", repositoryId);
            return false;
        }

        // Recursively register all directories
        Set<Path> directories = collectDirectories(repositoryPath);
        if (directories.isEmpty()) {
            logger.warn("No directories found to watch for repository: {}", repositoryId);
            return false;
        }

        // Register directories with WatchService
        for (Path dir : directories) {
            try {
                dir.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException e) {
                logger.warn("Could not register directory {} for watching: {}", dir, e.getMessage());
            } catch (InvalidPathException e) {
                logger.warn("Invalid path {} for repository {}: {}", dir, repositoryId, e.getMessage());
            } catch (ClosedWatchServiceException e) {
                logger.error("Watch service is closed for repository: {}", repositoryId);
                return false;
            } catch (SecurityException e) {
                logger.error("Access denied to directory {} for repository {}: {}", dir, repositoryId, e.getMessage());
            }
        }

        // Store watched directories
        watchDirLock.writeLock().lock();
        try {
            watchedDirectories.put(repositoryId, Collections.unmodifiableSet(directories));
        } finally {
            watchDirLock.writeLock().unlock();
        }

        logger.info("Watching {} directories for repository: {}", directories.size(), repositoryId);

        // Start the watcher thread
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                () -> processWatchEvents(repositoryId, repositoryPath),
                0,
                100,
                TimeUnit.MILLISECONDS
        );
        watcherThreads.put(repositoryId, future);

        logger.info("Started watching repository: {} at {}", repositoryId, repositoryPath);
        return true;
    }

    /**
     * Stops watching a specific registered repository.
     */
    @Override
    public boolean stopRepositoryWatcher(String repositoryId) {
        ScheduledFuture<?> future = watcherThreads.remove(repositoryId);
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
            logger.info("Stopped watcher for repository: {}", repositoryId);
            return true;
        }
        return false;
    }

    /**
     * Restarts the watcher for a specific repository.
     */
    @Override
    public boolean restartRepositoryWatcher(String repositoryId) {
        boolean stopped = stopRepositoryWatcher(repositoryId);
        if (stopped) {
            // Clear watched directories to force re-registration
            watchDirLock.writeLock().lock();
            try {
                watchedDirectories.remove(repositoryId);
            } finally {
                watchDirLock.writeLock().unlock();
            }
            return startRepositoryWatcher(repositoryId);
        }
        return false;
    }

    /**
     * Processes watch events for a repository.
     */
    private void processWatchEvents(String repositoryId, Path repositoryPath) {
        try {
            WatchKey key;
            try {
                key = watchService.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (key == null) {
                return;
            }

            Path watchedDir = (Path) key.watchable();

            // Check if this directory is still being watched
            watchDirLock.readLock().lock();
            boolean isWatched;
            try {
                Set<Path> dirs = watchedDirectories.get(repositoryId);
                isWatched = dirs != null && dirs.contains(watchedDir);
            } finally {
                watchDirLock.readLock().unlock();
            }

            if (!isWatched) {
                key.reset();
                return;
            }

            for (java.nio.file.WatchEvent<?> event : key.pollEvents()) {
                java.nio.file.WatchEvent.Kind<?> kind = event.kind();

                // Skip OVERFLOW events
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                java.nio.file.WatchEvent<Path> pathEvent = (java.nio.file.WatchEvent<Path>) event;
                Path fileName = pathEvent.context();
                Path fullPath = watchedDir.resolve(fileName);

                // Ignore hidden directories and files at root level of watched path
                if (fullPath.getNameCount() > repositoryPath.getNameCount()) {
                    // Check each path component for ignored directories
                    boolean shouldIgnore = false;
                    for (int i = repositoryPath.getNameCount(); i < fullPath.getNameCount(); i++) {
                        String component = fullPath.getName(i).toString();
                        if (fileWatchProperties.isIgnoredDirectory(component)) {
                            shouldIgnore = true;
                            break;
                        }
                    }

                    // Check file name patterns for files
                    if (!shouldIgnore && !Files.isDirectory(fullPath)) {
                        shouldIgnore = fileWatchProperties.shouldIgnorePath(fullPath);
                    }

                    if (!shouldIgnore) {
                        EventType eventType = mapWatchEventKind(kind);
                        WatchEvent watchEvent = new WatchEvent(
                                repositoryId,
                                fullPath,
                                repositoryPath.relativize(fullPath).toString(),
                                eventType,
                                Instant.now(),
                                watchedDir.toString()
                        );

                        // Non-blocking offer to queue
                        if (eventQueue.offer(watchEvent)) {
                            eventsProcessed.incrementAndGet();
                            lastEventTimestamp.set(System.currentTimeMillis());
                            logger.debug("Captured {} event: {}", eventType, fullPath);
                        } else {
                            eventsIgnored.incrementAndGet();
                            logger.warn("Event queue full, dropping event: {}", fullPath);
                        }

                        // Auto-register newly created directories
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                            autoRegisterDirectory(repositoryId, fullPath);
                        }
                    } else {
                        eventsIgnored.incrementAndGet();
                    }
                }
            }

            // Reset the key
            boolean valid = key.reset();
            if (!valid) {
                logger.warn("Watch key for {} is no longer valid, may have been deleted: {}", watchedDir, repositoryId);
            }

        } catch (Exception e) {
            logger.error("Error processing watch events for repository {}: {}", repositoryId, e.getMessage(), e);
        }
    }

    /**
     * Auto-registers a newly created directory with the WatchService.
     */
    private void autoRegisterDirectory(String repositoryId, Path newDir) {
        if (!fileWatchProperties.isInIgnoredDirectory(newDir)) {
            try {
                newDir.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                watchDirLock.writeLock().lock();
                try {
                    Set<Path> dirs = watchedDirectories.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet());
                    dirs.add(newDir);
                } finally {
                    watchDirLock.writeLock().unlock();
                }

                logger.debug("Auto-registered new directory: {} for repository: {}", newDir, repositoryId);
            } catch (IOException e) {
                logger.warn("Could not auto-register directory {} for {}: {}", newDir, repositoryId, e.getMessage());
            } catch (ClosedWatchServiceException e) {
                logger.error("Watch service is closed when trying to register: {}", newDir);
            } catch (SecurityException e) {
                logger.error("Access denied when registering directory: {} for {}", newDir, repositoryId);
            }
        }
    }

    /**
     * Maps a WatchEvent kind to our EventType enum.
     */
    private EventType mapWatchEventKind(java.nio.file.WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            return EventType.CREATED;
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            return EventType.MODIFIED;
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            return EventType.DELETED;
        }
        return EventType.MODIFIED; // Default for unknown kinds
    }

    /**
     * Collects all directories recursively under the given path.
     */
    private Set<Path> collectDirectories(Path rootPath) {
        Set<Path> directories = ConcurrentHashMap.newKeySet();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Check if directory should be ignored
                    if (fileWatchProperties.isInIgnoredDirectory(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    directories.add(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.warn("Cannot access file/directory: {}", file);
                    return FileVisitResult.SKIP_SIBLINGS;
                }
            });
        } catch (IOException e) {
            logger.error("Error collecting directories from {}: {}", rootPath, e.getMessage());
        }

        return directories;
    }

    /**
     * Captures a watch event from the queue (non-blocking).
     */
    @Override
    public WatchEvent captureEvent() {
        if (eventQueue.isEmpty()) {
            return null;
        }
        try {
            return eventQueue.poll(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Captures a watch event from the queue (blocking with timeout).
     */
    @Override
    public WatchEvent captureEvent(long timeout, TimeUnit unit) throws InterruptedException {
        return eventQueue.poll(timeout, unit);
    }

    /**
     * Returns the current watch statistics.
     */
    @Override
    public FileWatchStats getStatistics() {
        repoLock.readLock().lock();
        int activeRepos;
        try {
            // Count repositories that have active watcher threads
            activeRepos = (int) watcherThreads.values().stream()
                    .filter(f -> !f.isCancelled() && !f.isDone())
                    .count();
        } finally {
            repoLock.readLock().unlock();
        }

        watchDirLock.readLock().lock();
        int totalDirs = 0;
        try {
            for (Set<Path> dirs : watchedDirectories.values()) {
                totalDirs += dirs.size();
            }
        } finally {
            watchDirLock.readLock().unlock();
        }

        return new FileWatchStats(
                activeRepos,
                totalDirs,
                eventsProcessed.get(),
                eventQueue.size(),
                lastEventTimestamp.get() > 0 ? Instant.ofEpochMilli(lastEventTimestamp.get()) : null
        );
    }

    /**
     * Returns true if the watch service is currently running.
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the set of registered repository IDs.
     */
    @Override
    public Set<String> getRegisteredRepositories() {
        repoLock.readLock().lock();
        try {
            return Collections.unmodifiableSet(registeredRepositories.keySet());
        } finally {
            repoLock.readLock().unlock();
        }
    }

    /**
     * Returns the set of watched directory paths for a repository.
     */
    @Override
    public Set<String> getWatchedDirectories(String repositoryId) {
        watchDirLock.readLock().lock();
        try {
            Set<Path> dirs = watchedDirectories.get(repositoryId);
            if (dirs != null) {
                return dirs.stream()
                        .map(Path::toString)
                        .collect(Collectors.toSet());
            }
            return Collections.emptySet();
        } finally {
            watchDirLock.readLock().unlock();
        }
    }

    /**
     * Checks if a file path should be ignored based on current filtering rules.
     */
    @Override
    public boolean shouldIgnorePath(Path path) {
        return fileWatchProperties.shouldIgnorePath(path);
    }

    /**
     * Cleanup on bean destruction.
     */
    @PreDestroy
    public void destroy() {
        stopWatching();
    }
}