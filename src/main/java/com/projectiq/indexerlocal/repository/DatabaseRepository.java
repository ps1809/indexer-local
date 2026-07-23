package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.DatabaseArtifact;
import com.projectiq.indexerlocal.model.DatabaseType;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for persisting database artifact information.
 * Uses ConcurrentHashMap for thread-safe operations.
 */
@Repository
public class DatabaseRepository {

    private final Map<String, DatabaseArtifact> artifacts = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> repositoryStatistics = new ConcurrentHashMap<>();

    /**
     * Save a database artifact.
     */
    public void save(DatabaseArtifact artifact) {
        String key = artifact.getRepositoryId() + ":" + artifact.getFilePath().toString();
        artifacts.put(key, artifact);
    }

    /**
     * Save multiple database artifacts.
     */
    public void saveAll(List<DatabaseArtifact> artifactsList) {
        for (DatabaseArtifact artifact : artifactsList) {
            save(artifact);
        }
    }

    /**
     * Find all database artifacts for a repository.
     */
    public List<DatabaseArtifact> findByRepositoryId(String repositoryId) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .toList();
    }

    /**
     * Find database artifacts by repository ID and artifact type.
     */
    public List<DatabaseArtifact> findByRepositoryIdAndType(String repositoryId, DatabaseArtifact.ArtifactType type) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .filter(a -> type == a.getArtifactType())
                .toList();
    }

    /**
     * Find database artifacts by repository ID and database type.
     */
    public List<DatabaseArtifact> findByRepositoryIdAndDatabaseType(String repositoryId, DatabaseType databaseType) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .filter(a -> databaseType == a.getDatabaseType())
                .toList();
    }

    /**
     * Find SQL files for a repository.
     */
    public List<DatabaseArtifact> findSqlFiles(String repositoryId) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .filter(a -> a.getArtifactType() == DatabaseArtifact.ArtifactType.SQL_FILE
                        || a.getArtifactType() == DatabaseArtifact.ArtifactType.DDL_SCRIPT
                        || a.getArtifactType() == DatabaseArtifact.ArtifactType.DML_SCRIPT
                        || a.getArtifactType() == DatabaseArtifact.ArtifactType.MIGRATION_SCRIPT)
                .toList();
    }

    /**
     * Find SQL files by classification for a repository.
     */
    public List<DatabaseArtifact> findSqlFilesByClassification(String repositoryId, com.projectiq.indexerlocal.model.SqlFileClassification classification) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .filter(a -> classification == a.getSqlClassification())
                .toList();
    }

    /**
     * Find datasource configurations for a repository.
     */
    public List<DatabaseArtifact> findDatasourceConfigs(String repositoryId) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .filter(a -> a.getArtifactType() == DatabaseArtifact.ArtifactType.DATASOURCE_CONFIG)
                .toList();
    }

    /**
     * Find migration configurations for a repository.
     */
    public List<DatabaseArtifact> findMigrationConfigs(String repositoryId) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .filter(a -> a.getArtifactType() == DatabaseArtifact.ArtifactType.MIGRATION_CONFIG)
                .toList();
    }

    /**
     * Find ORM configurations for a repository.
     */
    public List<DatabaseArtifact> findOrmConfigs(String repositoryId) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .filter(a -> a.getArtifactType() == DatabaseArtifact.ArtifactType.ORM_CONFIG)
                .toList();
    }

    /**
     * Find detected databases for a repository.
     */
    public Set<DatabaseType> findDatabases(String repositoryId) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .map(DatabaseArtifact::getDatabaseType)
                .filter(d -> d != null && d != DatabaseType.UNKNOWN)
                .collect(Collectors.toSet());
    }

    /**
     * Find detected ORM frameworks for a repository.
     */
    public Set<String> findOrmFrameworks(String repositoryId) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .map(DatabaseArtifact::getOrmFramework)
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Find detected migration frameworks for a repository.
     */
    public Set<String> findMigrationFrameworks(String repositoryId) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .map(DatabaseArtifact::getMigrationFramework)
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Delete all artifacts for a repository.
     */
    public void deleteByRepositoryId(String repositoryId) {
        artifacts.entrySet().removeIf(entry ->
                repositoryId.equals(entry.getKey().split(":")[0]));
    }

    /**
     * Check if repository has any database artifacts.
     */
    public boolean hasDatabases(String repositoryId) {
        return artifacts.values().stream()
                .anyMatch(a -> repositoryId.equals(a.getRepositoryId()));
    }

    /**
     * Store database statistics for a repository.
     */
    public void saveStatistics(String repositoryId, Map<String, Object> statistics) {
        repositoryStatistics.put(repositoryId, statistics);
    }

    /**
     * Get database statistics for a repository.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatistics(String repositoryId) {
        return repositoryStatistics.getOrDefault(repositoryId, new ConcurrentHashMap<>());
    }

    /**
     * Check if standard datasource configuration files exist.
     */
    public boolean hasDatasourceConfig(String repositoryId) {
        return artifacts.values().stream()
                .filter(a -> repositoryId.equals(a.getRepositoryId()))
                .anyMatch(a -> a.getArtifactType() == DatabaseArtifact.ArtifactType.DATASOURCE_CONFIG);
    }

    /**
     * Clear all data. Used for testing.
     */
    public void clearAll() {
        artifacts.clear();
        repositoryStatistics.clear();
    }
}