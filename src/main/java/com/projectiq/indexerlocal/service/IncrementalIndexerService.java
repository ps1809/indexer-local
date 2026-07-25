package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.event.WatchEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service that polls the FileWatchService event queue and processes
 * filesystem events incrementally. This is the main event-driven
 * incremental parser that connects the Watch Service to the
 * Incremental Indexing pipeline.
 *
 * Processing Pipeline:
 *   File Event → Incremental Parser → Existing Extractors → Database Update → Relationship Refresh
 */
@Service
public class IncrementalIndexerService {

    private static final Logger log = LoggerFactory.getLogger(IncrementalIndexerService.class);

    private final FileWatchService fileWatchService;
    private final IncrementalIndexerEventProcessor eventProcessor;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final AtomicLong totalEventsSkipped = new AtomicLong(0);

    private static final long POLL_INTERVAL_MS = 500; // Poll every 500ms
    private static final long POLL_TIMEOUT_MS = 100;  // Wait up to 100ms per poll

    public IncrementalIndexerService(FileWatchService fileWatchService,
                                     IncrementalIndexerEventProcessor eventProcessor) {
        this.fileWatchService = fileWatchService;
        this.eventProcessor = eventProcessor;
    }

    /**
     * Start the incremental indexer service.
     * Begins polling the FileWatchService event queue.
     */
    @PostConstruct
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("[INCREMENTAL-INDEXER] Starting Incremental Indexer Service...");
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "incremental-indexer");
                t.setDaemon(true);
                return t;
            });

            scheduler.scheduleWithFixedDelay(
                    this::pollAndProcessEvents,
                    0,
                    POLL_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );

            log.info("[INCREMENTAL-INDEXER] Incremental Indexer Service started. Polling every {}ms", POLL_INTERVAL_MS);
        }
    }

    /**
     * Stop the incremental indexer service.
     */
    @PreDestroy
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("[INCREMENTAL-INDEXER] Stopping Incremental Indexer Service...");
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
            log.info("[INCREMENTAL-INDEXER] Incremental Indexer Service stopped. " +
                    "Total processed: {}, failed: {}, skipped: {}",
                    totalEventsProcessed.get(), totalEventsFailed.get(), totalEventsSkipped.get());
        }
    }

    /**
     * Poll the event queue and process all available events.
     */
    private void pollAndProcessEvents() {
        if (!running.get()) {
            return;
        }

        int processedInBatch = 0;
        int failedInBatch = 0;
        int skippedInBatch = 0;

        try {
            // Process all available events in the queue
            while (true) {
                WatchEvent event = fileWatchService.captureEvent(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (event == null) {
                    break; // No more events in the queue
                }

                boolean success = eventProcessor.processEvent(event);
                if (success) {
                    processedInBatch++;
                    totalEventsProcessed.incrementAndGet();
                } else {
                    failedInBatch++;
                    totalEventsFailed.incrementAndGet();
                }
            }

            if (processedInBatch > 0 || failedInBatch > 0) {
                log.debug("[INCREMENTAL-INDEXER] Batch processed: {} succeeded, {} failed, {} skipped",
                        processedInBatch, failedInBatch, skippedInBatch);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[INCREMENTAL-INDEXER] Polling interrupted");
        } catch (Exception e) {
            log.error("[INCREMENTAL-INDEXER] Error during event polling: {}", e.getMessage(), e);
        }
    }

    /**
     * Returns whether the service is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the total number of events processed successfully.
     */
    public long getTotalEventsProcessed() {
        return totalEventsProcessed.get();
    }

    /**
     * Returns the total number of events that failed processing.
     */
    public long getTotalEventsFailed() {
        return totalEventsFailed.get();
    }

    /**
     * Returns the total number of events skipped.
     */
    public long getTotalEventsSkipped() {
        return totalEventsSkipped.get();
    }
}