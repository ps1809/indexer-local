package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents dependency analysis statistics for a repository.
 */
public class DependencyStatistics {

    private String repositoryId;
    private int totalDependencies;
    private Map<DependencyType, Integer> dependenciesByScope = new HashMap<>();
    private Map<String, Integer> dependenciesByType = new HashMap<>();
    private List<String> duplicateDependencies;
    private int missingVersionsCount;
    private int snapshotDependenciesCount;
    private int internalDependenciesCount;
    private int externalDependenciesCount;
    private LocalDateTime analyzedAt;

    public DependencyStatistics() {
        this.analyzedAt = LocalDateTime.now();
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public int getTotalDependencies() {
        return totalDependencies;
    }

    public void setTotalDependencies(int totalDependencies) {
        this.totalDependencies = totalDependencies;
    }

    public Map<DependencyType, Integer> getDependenciesByScope() {
        return dependenciesByScope;
    }

    public void setDependenciesByScope(Map<DependencyType, Integer> dependenciesByScope) {
        this.dependenciesByScope = dependenciesByScope;
    }

    public Map<String, Integer> getDependenciesByType() {
        return dependenciesByType;
    }

    public void setDependenciesByType(Map<String, Integer> dependenciesByType) {
        this.dependenciesByType = dependenciesByType;
    }

    public List<String> getDuplicateDependencies() {
        return duplicateDependencies;
    }

    public void setDuplicateDependencies(List<String> duplicateDependencies) {
        this.duplicateDependencies = duplicateDependencies;
    }

    public int getMissingVersionsCount() {
        return missingVersionsCount;
    }

    public void setMissingVersionsCount(int missingVersionsCount) {
        this.missingVersionsCount = missingVersionsCount;
    }

    public int getSnapshotDependenciesCount() {
        return snapshotDependenciesCount;
    }

    public void setSnapshotDependenciesCount(int snapshotDependenciesCount) {
        this.snapshotDependenciesCount = snapshotDependenciesCount;
    }

    public int getInternalDependenciesCount() {
        return internalDependenciesCount;
    }

    public void setInternalDependenciesCount(int internalDependenciesCount) {
        this.internalDependenciesCount = internalDependenciesCount;
    }

    public int getExternalDependenciesCount() {
        return externalDependenciesCount;
    }

    public void setExternalDependenciesCount(int externalDependenciesCount) {
        this.externalDependenciesCount = externalDependenciesCount;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}