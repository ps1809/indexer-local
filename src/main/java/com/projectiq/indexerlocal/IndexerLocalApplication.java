package com.projectiq.indexerlocal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the ProjectIQ Indexer Local application.
 * A Spring Boot application that provides code indexing capabilities for Java projects.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Java source file indexing</li>
 *   <li>Class, method, and field metadata extraction</li>
 *   <li>Spring component detection</li>
 *   <li>Annotation tracking</li>
 *   <li>REST API for querying indexed data</li>
 * </ul>
 * 
 * <p>Profiles:</p>
 * <ul>
 *   <li><b>local</b> - Default profile for local development (port 8080)</li>
 *   <li><b>dev</b> - Development profile (port 8081, context path /indexer-local)</li>
 *   <li><b>prod</b> - Production profile with minimal logging and actuator exposure</li>
 * </ul>
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>REST API: {@code /api/**}</li>
 *   <li>Swagger UI: {@code /swagger-ui.html}</li>
 *   <li>Actuator Health: {@code /actuator/health}</li>
 *   <li>Actuator Info: {@code /actuator/info}</li>
 * </ul>
 */
@SpringBootApplication
public class IndexerLocalApplication {

    /**
     * Main method to start the Spring Boot application.
     * 
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(IndexerLocalApplication.class, args);
    }
}