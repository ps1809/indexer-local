package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents statistics about project configuration files.
 */
public class ConfigurationStatistics {

    private String repositoryId;
    private int totalConfigurationFiles;
    private Map<ConfigurationType, Integer> configurationFilesByType;
    private int environmentSpecificConfigurationCount;
    private int missingStandardSpringConfigCount;
    private LocalDateTime analyzedAt;

    public ConfigurationStatistics() {
        this.analyzedAt = LocalDateTime.now();
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public int getTotalConfigurationFiles() {
        return totalConfigurationFiles;
    }

    public void setTotalConfigurationFiles(int totalConfigurationFiles) {
        this.totalConfigurationFiles = totalConfigurationFiles;
    }

    public Map<ConfigurationType, Integer> getConfigurationFilesByType() {
        return configurationFilesByType;
    }

    public void setConfigurationFilesByType(Map<ConfigurationType, Integer> configurationFilesByType) {
        this.configurationFilesByType = configurationFilesByType;
    }

    public int getEnvironmentSpecificConfigurationCount() {
        return environmentSpecificConfigurationCount;
    }

    public void setEnvironmentSpecificConfigurationCount(int environmentSpecificConfigurationCount) {
        this.environmentSpecificConfigurationCount = environmentSpecificConfigurationCount;
    }

    public int getMissingStandardSpringConfigCount() {
        return missingStandardSpringConfigCount;
    }

    public void setMissingStandardSpringConfigCount(int missingStandardSpringConfigCount) {
        this.missingStandardSpringConfigCount = missingStandardSpringConfigCount;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    @Override
    public String toString() {
        return "ConfigurationStatistics{" +
                "totalConfigurationFiles=" + totalConfigurationFiles +
                ", environmentSpecificConfigCount=" + environmentSpecificConfigurationCount +
                ", missingSpringConfigCount=" + missingStandardSpringConfigCount +
                ", analyzedAt=" + analyzedAt +
                '}';
    }
}