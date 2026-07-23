package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.Dependency;
import com.projectiq.indexerlocal.model.DependencyType;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for persisting dependency information.
 * Uses ConcurrentHashMap for thread-safe operations.
 */
@Repository
public class DependencyRepository {

    private final Map<String, Dependency> dependencies = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> repositoryStats = new ConcurrentHashMap<>();

    /**
     * Save a dependency.
     */
    public void save(Dependency dependency) {
        String key = dependency.getRepositoryId() + ":" + dependency.getUniqueKey();
        dependencies.put(key, dependency);
    }

    /**
     * Save multiple dependencies.
     */
    public void saveAll(List<Dependency> dependenciesList) {
        for (Dependency dep : dependenciesList) {
            save(dep);
        }
    }

    /**
     * Find all dependencies for a repository.
     */
    public List<Dependency> findByRepositoryId(String repositoryId) {
        return dependencies.values().stream()
                .filter(dep -> repositoryId.equals(dep.getRepositoryId()))
                .toList();
    }

    /**
     * Find dependencies by repository ID and scope/type.
     */
    public List<Dependency> findByRepositoryIdAndType(String repositoryId, DependencyType type) {
        return dependencies.values().stream()
                .filter(dep -> repositoryId.equals(dep.getRepositoryId()))
                .filter(dep -> type.equals(dep.getType()))
                .toList();
    }

    /**
     * Find dependencies by repository ID and scope (string).
     */
    public List<Dependency> findByRepositoryIdAndScope(String repositoryId, String scope) {
        try {
            DependencyType type = DependencyType.valueOf(scope.toUpperCase());
            return findByRepositoryIdAndType(repositoryId, type);
        } catch (IllegalArgumentException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Delete all dependencies for a repository.
     */
    public void deleteByRepositoryId(String repositoryId) {
        dependencies.entrySet().removeIf(entry ->
                repositoryId.equals(entry.getKey().split(":")[0]));
    }

    /**
     * Check if repository has any dependencies.
     */
    public boolean hasDependencies(String repositoryId) {
        return dependencies.values().stream()
                .anyMatch(dep -> repositoryId.equals(dep.getRepositoryId()));
    }

    /**
     * Get total count of dependencies for a repository.
     */
    public int getCountByRepositoryId(String repositoryId) {
        return (int) dependencies.values().stream()
                .filter(dep -> repositoryId.equals(dep.getRepositoryId()))
                .count();
    }

    /**
     * Store dependency statistics for a repository.
     */
    public void saveStatistics(String repositoryId, Map<String, Object> statistics) {
        repositoryStats.put(repositoryId, statistics);
    }

    /**
     * Get dependency statistics for a repository.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatistics(String repositoryId) {
        return repositoryStats.getOrDefault(repositoryId, new ConcurrentHashMap<>());
    }

    /**
     * Clear all data. Used for testing.
     */
    public void clearAll() {
        dependencies.clear();
        repositoryStats.clear();
    }
}