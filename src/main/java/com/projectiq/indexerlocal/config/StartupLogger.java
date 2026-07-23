package com.projectiq.indexerlocal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${spring.application.name:indexer-local}")
    private String applicationName;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerPath;

    /**
     * Log application startup information.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        log.info("====================================================");
        log.info("  {} started successfully", applicationName);
        log.info("  Application is ready to accept requests");
        log.info("  Server port: {}", serverPort);
        log.info("  Swagger UI: http://localhost:{}{}", serverPort, swaggerPath);
        log.info("  API Docs: http://localhost:{}/api-docs", serverPort);
        log.info("====================================================");
    }

    /**
     * Log application shutdown events.
     */
    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown() {
        log.info("====================================================");
        log.info("  {} is shutting down", applicationName);
        log.info("====================================================");
    }
}
