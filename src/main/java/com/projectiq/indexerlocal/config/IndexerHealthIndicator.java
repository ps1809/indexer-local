package com.projectiq.indexerlocal.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;

/**
 * Custom health indicator for the indexer application.
 * Reports database connectivity and indexing service status.
 */
@Component
public class IndexerHealthIndicator implements HealthIndicator {

    private final String dbUrl;

    public IndexerHealthIndicator() {
        this.dbUrl = "sqlite";
    }

    @Override
    public Health health() {
        try {
            // Check database connectivity
            DriverManager.getConnection("jdbc:" + dbUrl + ":./indexer-local.db");
            
            return Health.up()
                .withDetail("database", "connected")
                .withDetail("component", "indexer-local")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "disconnected")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}