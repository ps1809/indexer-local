package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.DatabaseArtifact;
import com.projectiq.indexerlocal.model.DatabaseType;
import com.projectiq.indexerlocal.model.SqlFileClassification;
import com.projectiq.indexerlocal.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for database analysis operations.
 * Provides endpoints to analyze, discover, and retrieve database artifacts
 * from repositories without connecting to any database.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/database")
@Tag(name = "Database Analyzer", description = "APIs for discovering and analyzing database artifacts in repositories")
public class DatabaseController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseController.class);

    private final DatabaseService databaseService;

    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * POST /api/v1/repositories/{repositoryId}/database - Analyze database artifacts.
     */
    @PostMapping
    @Operation(
        summary = "Analyze database artifacts",
        description = "Discovers, analyzes, classifies, and persists all database-related artifacts in a repository. " +
                      "This includes detected databases, datasource configurations, SQL files, migration frameworks, " +
                      "and ORM frameworks. Does NOT connect to any database."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Database analysis completed successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DatabaseArtifact.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Repository not found"
    )
    public ResponseEntity<List<DatabaseArtifact>> analyzeDatabases(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        logger.info("POST /api/v1/repositories/{repositoryId}/database - Starting database analysis");
        List<DatabaseArtifact> artifacts = databaseService.analyzeDatabases(repositoryId);
        logger.info("Database analysis completed. Found {} artifacts for repository: {}", artifacts.size(), repositoryId);
        return ResponseEntity.ok(artifacts);
    }

    /**
     * GET /api/v1/repositories/{repositoryId}/database - Retrieve detected database information.
     */
    @GetMapping
    @Operation(
        summary = "Retrieve detected databases",
        description = "Get all database artifacts detected in the repository. " +
                      "Includes datasource configurations, SQL files, migration configs, and ORM configs."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Database artifacts retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DatabaseArtifact.class))
    )
    public ResponseEntity<List<DatabaseArtifact>> getDatabases(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        List<DatabaseArtifact> artifacts = databaseService.getDatabases(repositoryId);
        return ResponseEntity.ok(artifacts);
    }

    /**
     * GET /api/v1/repositories/{repositoryId}/database/sql - Retrieve SQL inventory.
     */
    @GetMapping("/sql")
    @Operation(
        summary = "Retrieve SQL file inventory",
        description = "Get all SQL files detected in the repository with their classifications " +
                      "(DDL, DML, MIGRATION, SEED_DATA, etc.)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "SQL file inventory retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DatabaseArtifact.class))
    )
    public ResponseEntity<List<DatabaseArtifact>> getSqlFiles(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        List<DatabaseArtifact> sqlFiles = databaseService.getSqlFiles(repositoryId);
        return ResponseEntity.ok(sqlFiles);
    }

    /**
     * GET /api/v1/repositories/{repositoryId}/database/sql?classification=DDL - Filter SQL files by classification.
     */
    @GetMapping("/sql/classification")
    @Operation(
        summary = "Retrieve SQL files by classification",
        description = "Get SQL files filtered by their classification type (DDL, DML, MIGRATION, SEED_DATA, etc.)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "SQL files retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<List<DatabaseArtifact>> getSqlFilesByClassification(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId,
            @Parameter(description = "SQL file classification type")
            @RequestParam SqlFileClassification classification) {
        
        List<DatabaseArtifact> sqlFiles = databaseService.getSqlFilesByClassification(repositoryId, classification);
        return ResponseEntity.ok(sqlFiles);
    }

    /**
     * GET /api/v1/repositories/{repositoryId}/database/statistics - Retrieve database statistics.
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Retrieve database statistics",
        description = "Get comprehensive statistics about detected databases including: total databases, " +
                      "datasource configurations, SQL files by category, migration script count, ORM framework count, " +
                      "and migration framework count."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Database statistics retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))
    )
    public ResponseEntity<Map<String, Object>> getStatistics(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        Map<String, Object> statistics = databaseService.getStatistics(repositoryId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * GET /api/v1/repositories/{repositoryId}/database/types - Retrieve detected database types.
     */
    @GetMapping("/types")
    @Operation(
        summary = "Retrieve detected database types",
        description = "Get the list of detected database types (ORACLE, MYSQL, PostgreSQL, etc.)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Database types retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Set<String>> getDetectedDatabaseTypes(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        Set<DatabaseType> types = databaseService.getDetectedDatabases(repositoryId);
        Set<String> typeNames = types.stream()
                .map(DatabaseType::name)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(typeNames);
    }

    /**
     * GET /api/v1/repositories/{repositoryId}/database/datasource - Retrieve datasource configurations.
     */
    @GetMapping("/datasource")
    @Operation(
        summary = "Retrieve datasource configurations",
        description = "Get all datasource configurations detected in the repository."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Datasource configurations retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<List<DatabaseArtifact>> getDatasourceConfigs(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        List<DatabaseArtifact> configs = databaseService.getDatasourceConfigs(repositoryId);
        return ResponseEntity.ok(configs);
    }

    /**
     * GET /api/v1/repositories/{repositoryId}/database/orm - Retrieve ORM framework information.
     */
    @GetMapping("/orm")
    @Operation(
        summary = "Retrieve ORM framework information",
        description = "Get all detected ORM frameworks (SPRING_DATA_JPA, HIBERNATE, MYBATIS, etc.)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "ORM frameworks retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Set<String>> getDetectedOrmFrameworks(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        Set<String> frameworks = databaseService.getDetectedOrmFrameworks(repositoryId);
        return ResponseEntity.ok(frameworks);
    }

    /**
     * GET /api/v1/repositories/{repositoryId}/database/migration - Retrieve migration framework information.
     */
    @GetMapping("/migration")
    @Operation(
        summary = "Retrieve migration framework information",
        description = "Get all detected migration frameworks (FLYWAY, LIQUIBASE)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Migration frameworks retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Set<String>> getDetectedMigrationFrameworks(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        Set<String> frameworks = databaseService.getDetectedMigrationFrameworks(repositoryId);
        return ResponseEntity.ok(frameworks);
    }
}