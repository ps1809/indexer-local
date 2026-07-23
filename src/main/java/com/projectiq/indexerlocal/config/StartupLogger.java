package com.projectiq.indexerlocal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

/**
 * Logs important application startup and shutdown events.
 */
@Configuration
public class StartupLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    /**
     * Log application startup information.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        log.info("====================================================");
        log.info("  ProjectIQ Indexer Local started successfully");
        log.info("  Application is ready to accept requests");
        log.info("====================================================");
    }

    /**
     * Log application shutdown events.
     */
    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown() {
        log.warn("====================================================");
        log.warn("  ProjectIQ Indexer Local is shutting down");
        log.warn("====================================================");
    }
}