package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.ConfigurationFile;
import com.projectiq.indexerlocal.model.ConfigurationType;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for persisting configuration file information.
 * Uses ConcurrentHashMap for thread-safe operations.
 */
@Repository
public class ConfigurationRepository {

    private final Map<String, ConfigurationFile> configurationFiles = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> repositoryStatistics = new ConcurrentHashMap<>();

    /**
     * Save a configuration file.
     */
    public void save(ConfigurationFile configurationFile) {
        String key = configurationFile.getRepositoryId() + ":" + configurationFile.getFileName();
        configurationFiles.put(key, configurationFile);
    }

    /**
     * Save multiple configuration files.
     */
    public void saveAll(List<ConfigurationFile> configurationFilesList) {
        for (ConfigurationFile config : configurationFilesList) {
            save(config);
        }
    }

    /**
     * Find all configuration files for a repository.
     */
    public List<ConfigurationFile> findByRepositoryId(String repositoryId) {
        return configurationFiles.values().stream()
                .filter(config -> repositoryId.equals(config.getRepositoryId()))
                .toList();
    }

    /**
     * Find configuration files by repository ID and type.
     */
    public List<ConfigurationFile> findByRepositoryIdAndType(String repositoryId, ConfigurationType type) {
        return configurationFiles.values().stream()
                .filter(config -> repositoryId.equals(config.getRepositoryId()))
                .filter(config -> type.equals(config.getConfigurationType()))
                .toList();
    }

    /**
     * Find configuration files by repository ID and file name pattern.
     */
    public List<ConfigurationFile> findByRepositoryIdAndFileNamePattern(String repositoryId, String fileNamePattern) {
        return configurationFiles.values().stream()
                .filter(config -> repositoryId.equals(config.getRepositoryId()))
                .filter(config -> config.getFileName().contains(fileNamePattern))
                .toList();
    }

    /**
     * Delete all configuration files for a repository.
     */
    public void deleteByRepositoryId(String repositoryId) {
        configurationFiles.entrySet().removeIf(entry ->
                repositoryId.equals(entry.getKey().split(":")[0]));
    }

    /**
     * Check if repository has any configuration files.
     */
    public boolean hasConfigurations(String repositoryId) {
        return configurationFiles.values().stream()
                .anyMatch(config -> repositoryId.equals(config.getRepositoryId()));
    }

    /**
     * Get total count of configuration files for a repository.
     */
    public int getCountByRepositoryId(String repositoryId) {
        return (int) configurationFiles.values().stream()
                .filter(config -> repositoryId.equals(config.getRepositoryId()))
                .count();
    }

    /**
     * Store configuration statistics for a repository.
     */
    public void saveStatistics(String repositoryId, Map<String, Object> statistics) {
        repositoryStatistics.put(repositoryId, statistics);
    }

    /**
     * Get configuration statistics for a repository.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatistics(String repositoryId) {
        return repositoryStatistics.getOrDefault(repositoryId, new ConcurrentHashMap<>());
    }

    /**
     * Check if standard Spring configuration files exist.
     */
    public boolean hasStandardSpringConfig(String repositoryId, String fileNamePattern) {
        return configurationFiles.values().stream()
                .filter(config -> repositoryId.equals(config.getRepositoryId()))
                .anyMatch(config -> config.getFileName().contains(fileNamePattern));
    }

    /**
     * Clear all data. Used for testing.
     */
    public void clearAll() {
        configurationFiles.clear();
        repositoryStatistics.clear();
    }
}