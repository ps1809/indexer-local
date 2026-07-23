package com.projectiq.indexerlocal.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Centralized DataSource configuration for SQLite.
 * Uses HikariCP connection pooling optimized for SQLite.
 */
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.hikari.data-source-uri:jdbc:sqlite:./indexer-local.db}")
    private String dataSourceUri;

    /**
     * Configures HikariCP connection pool for SQLite.
     * HikariCP is the default connection pool in Spring Boot.
     */
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dataSourceUri);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        // HikariCP settings optimized for SQLite (single-writer database)
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(2);
        dataSource.setIdleTimeout(300000);
        dataSource.setMaxLifetime(1200000);
        dataSource.setConnectionTimeout(30000);
        
        // SQLite-specific JDBC properties
        dataSource.addDataSourceProperty("cacheSize", "5000");
        dataSource.addDataSourceProperty("foreignKeys", "true");
        
        return dataSource;
    }
}