package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents health statistics for a repository index.
 * Provides metrics about the indexed state, consistency, and overall health.
 */
public class RepositoryHealthStatistics {

    private String repositoryId;
    private String repositoryName;
    private long indexedRepositories;
    private long indexedModules;
    private long indexedFiles;
    private long indexedSymbols;
    private long springComponents;
    private long restEndpoints;
    private long relationships;
    private long dependencies;
    private LocalDateTime lastIndexingTime;
    private double healthScore;
    private String healthStatus;

    public RepositoryHealthStatistics() {
        this.healthScore = 0.0;
        this.healthStatus = "UNKNOWN";
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public long getIndexedRepositories() {
        return indexedRepositories;
    }

    public void setIndexedRepositories(long indexedRepositories) {
        this.indexedRepositories = indexedRepositories;
    }

    public long getIndexedModules() {
        return indexedModules;
    }

    public void setIndexedModules(long indexedModules) {
        this.indexedModules = indexedModules;
    }

    public long getIndexedFiles() {
        return indexedFiles;
    }

    public void setIndexedFiles(long indexedFiles) {
        this.indexedFiles = indexedFiles;
    }

    public long getIndexedSymbols() {
        return indexedSymbols;
    }

    public void setIndexedSymbols(long indexedSymbols) {
        this.indexedSymbols = indexedSymbols;
    }

    public long getSpringComponents() {
        return springComponents;
    }

    public void setSpringComponents(long springComponents) {
        this.springComponents = springComponents;
    }

    public long getRestEndpoints() {
        return restEndpoints;
    }

    public void setRestEndpoints(long restEndpoints) {
        this.restEndpoints = restEndpoints;
    }

    public long getRelationships() {
        return relationships;
    }

    public void setRelationships(long relationships) {
        this.relationships = relationships;
    }

    public long getDependencies() {
        return dependencies;
    }

    public void setDependencies(long dependencies) {
        this.dependencies = dependencies;
    }

    public LocalDateTime getLastIndexingTime() {
        return lastIndexingTime;
    }

    public void setLastIndexingTime(LocalDateTime lastIndexingTime) {
        this.lastIndexingTime = lastIndexingTime;
    }

    public double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(double healthScore) {
        this.healthScore = healthScore;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    /**
     * Convert to a map representation for API responses.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("repositoryId", repositoryId);
        map.put("repositoryName", repositoryName);
        map.put("indexedRepositories", indexedRepositories);
        map.put("indexedModules", indexedModules);
        map.put("indexedFiles", indexedFiles);
        map.put("indexedSymbols", indexedSymbols);
        map.put("springComponents", springComponents);
        map.put("restEndpoints", restEndpoints);
        map.put("relationships", relationships);
        map.put("dependencies", dependencies);
        map.put("lastIndexingTime", lastIndexingTime != null ? lastIndexingTime.toString() : null);
        map.put("healthScore", healthScore);
        map.put("healthStatus", healthStatus);
        return map;
    }
}