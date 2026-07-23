package com.projectiq.indexerlocal.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Logging configuration for the indexer application.
 * 
 * <p>Log levels are configured via {@code application.properties} and profile-specific
 * property files ({@code application-{profile}.properties}). This class provides
 * initialization logging to confirm logging setup on startup.</p>
 * 
 * <h3>Log Levels by Profile</h3>
 * <ul>
 *   <li><b>local</b>: DEBUG for all components</li>
 *   <li><b>dev</b>: INFO root, DEBUG application</li>
 *   <li><b>prod</b>: WARN root, INFO application</li>
 * </ul>
 * 
 * <h3>Log Pattern</h3>
 * {@code %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n}
 */
@Configuration
public class LoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(LoggingConfig.class);

    /**
     * Log confirmation that logging configuration is initialized.
     * This helps verify the logging setup during application startup.
     */
    @PostConstruct
    public void init() {
        log.debug("LoggingConfig initialized - logging infrastructure ready");
    }
}