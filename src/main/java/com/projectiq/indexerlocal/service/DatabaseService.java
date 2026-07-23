package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.analyzer.DatabaseAnalyzer;
import com.projectiq.indexerlocal.model.DatabaseArtifact;
import com.projectiq.indexerlocal.model.DatabaseType;
import com.projectiq.indexerlocal.model.SqlFileClassification;
import com.projectiq.indexerlocal.repository.DatabaseRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;

/**
 * Service layer for database analysis operations.
 * Coordinates between Repository, and DatabaseAnalyzer.
 */
@Service
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private final DatabaseAnalyzer databaseAnalyzer;
    private final DatabaseRepository databaseRepository;
    private final RepositoryRepository repositoryRepository;

    public DatabaseService(DatabaseAnalyzer databaseAnalyzer,
                           DatabaseRepository databaseRepository,
                           RepositoryRepository repositoryRepository) {
        this.databaseAnalyzer = databaseAnalyzer;
        this.databaseRepository = databaseRepository;
        this.repositoryRepository = repositoryRepository;
    }

    /**
     * Analyze database artifacts for a repository.
     * 
     * @param repositoryId the repository ID to analyze
     * @return list of detected database artifacts
     * @throws IllegalArgumentException if repository doesn't exist or project structure analysis not completed
     */
    public List<DatabaseArtifact> analyzeDatabases(String repositoryId) {
        logger.info("Starting database analysis for repository: {}", repositoryId);

        // Validate repository exists
        var repository = getExistingRepository(repositoryId);

        Path workspacePath = Path.of(repository.getWorkspacePath());

        logger.info("Analyzing database artifacts in workspace: {}", workspacePath);

        // Analyze database artifacts
        List<DatabaseArtifact> artifacts = databaseAnalyzer.analyzeDatabases(repositoryId, workspacePath);

        // Clear existing artifacts and save new ones
        databaseRepository.deleteByRepositoryId(repositoryId);
        databaseRepository.saveAll(artifacts);

        // Calculate and store statistics
        Map<String, Object> stats = databaseAnalyzer.calculateStatistics(repositoryId, artifacts);
        databaseRepository.saveStatistics(repositoryId, stats);

        // Log detected databases
        Set<DatabaseType> detectedDatabases = artifacts.stream()
                .map(DatabaseArtifact::getDatabaseType)
                .filter(d -> d != null && d != DatabaseType.UNKNOWN)
                .collect(java.util.stream.Collectors.toSet());
        logger.info("Detected databases: {}", detectedDatabases);

        // Log migration framework detection
        Set<String> migrationFrameworks = artifacts.stream()
                .map(DatabaseArtifact::getMigrationFramework)
                .filter(f -> f != null && !f.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        if (!migrationFrameworks.isEmpty()) {
            logger.info("Migration frameworks detected: {}", migrationFrameworks);
        }

        logger.info("Database analysis completed for repository: {}. Found {} artifacts.", 
                repositoryId, artifacts.size());

        return artifacts;
    }

    /**
     * Retrieve all database artifacts for a repository.
     */
    public List<DatabaseArtifact> getDatabases(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return databaseRepository.findByRepositoryId(repositoryId);
    }

    /**
     * Retrieve detected database types for a repository.
     */
    public Set<DatabaseType> getDetectedDatabases(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return databaseRepository.findDatabases(repositoryId);
    }

    /**
     * Retrieve datasource configurations for a repository.
     */
    public List<DatabaseArtifact> getDatasourceConfigs(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return databaseRepository.findDatasourceConfigs(repositoryId);
    }

    /**
     * Retrieve SQL file inventory for a repository.
     */
    public List<DatabaseArtifact> getSqlFiles(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return databaseRepository.findSqlFiles(repositoryId);
    }

    /**
     * Retrieve SQL files by classification for a repository.
     */
    public List<DatabaseArtifact> getSqlFilesByClassification(String repositoryId, SqlFileClassification classification) {
        validateRepositoryExists(repositoryId);
        return databaseRepository.findSqlFilesByClassification(repositoryId, classification);
    }

    /**
     * Retrieve migration configurations for a repository.
     */
    public List<DatabaseArtifact> getMigrationConfigs(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return databaseRepository.findMigrationConfigs(repositoryId);
    }

    /**
     * Retrieve ORM configurations for a repository.
     */
    public List<DatabaseArtifact> getOrmConfigs(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return databaseRepository.findOrmConfigs(repositoryId);
    }

    /**
     * Detected ORM frameworks for a repository.
     */
    public Set<String> getDetectedOrmFrameworks(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return databaseRepository.findOrmFrameworks(repositoryId);
    }

    /**
     * Retrieve detected migration frameworks for a repository.
     */
    public Set<String> getDetectedMigrationFrameworks(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return databaseRepository.findMigrationFrameworks(repositoryId);
    }

    /**
     * Retrieve database statistics for a repository.
     */
    public Map<String, Object> getStatistics(String repositoryId) {
        validateRepositoryExists(repositoryId);
        
        Map<String, Object> stats = databaseRepository.getStatistics(repositoryId);
        if (stats.isEmpty()) {
            if (!databaseRepository.hasDatabases(repositoryId)) {
                throw new IllegalStateException("Database analysis not completed for repository: " + repositoryId);
            }
            throw new IllegalStateException("Statistics not available for repository: " + repositoryId);
        }
        return stats;
    }

    /**
     * Check if a repository has been analyzed for databases.
     */
    public boolean hasDatabases(String repositoryId) {
        return databaseRepository.hasDatabases(repositoryId);
    }

    // ==================== Private Methods ====================

    private com.projectiq.indexerlocal.model.Repository getExistingRepository(String repositoryId) {
        com.projectiq.indexerlocal.model.Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException(
                    "Repository not found: " + repositoryId);
        }
        return repository;
    }

    private void validateRepositoryExists(String repositoryId) {
        if (!repositoryRepository.existsByRepositoryId(repositoryId)) {
            throw new IllegalArgumentException(
                    "Invalid repository ID: " + repositoryId);
        }
    }
}