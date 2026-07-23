package com.projectiq.indexerlocal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Logging configuration for the indexer application.
 * Provides centralized logging setup using SLF4J.
 */
@Configuration
public class LoggingConfig {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfig.class);

    @PostConstruct
    public void init() {
        logger.info("========================================");
        logger.info("ProjectIQ Indexer Local starting up");
        logger.info("SLF4J logging is configured");
        logger.info("========================================");
    }
}