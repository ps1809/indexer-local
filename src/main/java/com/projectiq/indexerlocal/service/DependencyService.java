package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.analyzer.DependencyAnalyzer;
import com.projectiq.indexerlocal.model.BuildMetadata;
import com.projectiq.indexerlocal.model.Dependency;
import com.projectiq.indexerlocal.model.DependencyStatistics;
import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.repository.DependencyRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer for dependency analysis operations.
 * Coordinates between Repository, BuildSystemService, and DependencyAnalyzer.
 */
@Service
public class DependencyService {

    private static final Logger logger = LoggerFactory.getLogger(DependencyService.class);

    private final DependencyAnalyzer dependencyAnalyzer;
    private final DependencyRepository dependencyRepository;
    private final RepositoryRepository repositoryRepository;
    private final BuildSystemService buildSystemService;

    public DependencyService(DependencyAnalyzer dependencyAnalyzer,
                             DependencyRepository dependencyRepository,
                             RepositoryRepository repositoryRepository,
                             BuildSystemService buildSystemService) {
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.dependencyRepository = dependencyRepository;
        this.repositoryRepository = repositoryRepository;
        this.buildSystemService = buildSystemService;
    }

    /**
     * Analyze dependencies for a repository.
     * 
     * @param repositoryId the repository ID to analyze
     * @return list of discovered dependencies
     * @throws IllegalArgumentException if repository doesn't exist or build system analysis not completed
     */
    public List<Dependency> analyzeDependencies(String repositoryId) {
        logger.info("Starting dependency analysis for repository: {}", repositoryId);

        // Validate repository exists
        Repository repository = getExistingRepository(repositoryId);
        
        // Check that build system analysis was completed first
        BuildMetadata buildMetadata = getCompletedBuildMetadata(repositoryId);

        Path workspacePath = Path.of(repository.getWorkspacePath());
        
        logger.info("Analyzing dependencies using build system: {}", buildMetadata.getBuildSystemType());

        // Analyze dependencies
        List<Dependency> dependencies = dependencyAnalyzer.analyzeDependencies(
                repositoryId, workspacePath, buildMetadata);

        // Classify dependencies
        List<Dependency> classifiedDependencies = classifyDependencies(dependencies, buildMetadata);

        // Clear existing dependencies and save new ones
        dependencyRepository.deleteByRepositoryId(repositoryId);
        dependencyRepository.saveAll(classifiedDependencies);

        // Calculate and store statistics
        DependencyStatistics stats = dependencyAnalyzer.calculateStatistics(
                repositoryId, classifiedDependencies, buildMetadata);
        
        Map<String, Object> statsMap = new ConcurrentHashMap<>();
        statsMap.put("totalDependencies", stats.getTotalDependencies());
        statsMap.put("byScope", stats.getDependenciesByScope());
        statsMap.put("byType", stats.getDependenciesByType());
        statsMap.put("duplicateDependencies", stats.getDuplicateDependencies());
        statsMap.put("missingVersionsCount", stats.getMissingVersionsCount());
        statsMap.put("snapshotDependenciesCount", stats.getSnapshotDependenciesCount());
        statsMap.put("internalDependenciesCount", stats.getInternalDependenciesCount());
        statsMap.put("externalDependenciesCount", stats.getExternalDependenciesCount());
        statsMap.put("analyzedAt", stats.getAnalyzedAt().toString());
        
        dependencyRepository.saveStatistics(repositoryId, statsMap);

        logger.info("Dependency analysis completed for repository: {}. Found {} dependencies.",
                repositoryId, classifiedDependencies.size());

        return classifiedDependencies;
    }

    /**
     * Get dependency inventory for a repository.
     */
    public List<Dependency> getDependencies(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return dependencyRepository.findByRepositoryId(repositoryId);
    }

    /**
     * Get dependencies filtered by scope/type.
     */
    public List<Dependency> getDependenciesByScope(String repositoryId, String scope) {
        validateRepositoryExists(repositoryId);
        return dependencyRepository.findByRepositoryIdAndScope(repositoryId, scope);
    }

    /**
     * Get dependency statistics for a repository.
     */
    public Map<String, Object> getStatistics(String repositoryId) {
        validateRepositoryExists(repositoryId);
        
        Map<String, Object> stats = dependencyRepository.getStatistics(repositoryId);
        if (stats.isEmpty()) {
            // Run analysis first if no statistics exist
            if (dependencyRepository.hasDependencies(repositoryId)) {
                return stats;
            }
            throw new IllegalStateException("Dependency analysis not completed for repository: " + repositoryId);
        }
        return stats;
    }

    /**
     * Check if a repository has been analyzed for dependencies.
     */
    public boolean hasDependencies(String repositoryId) {
        return dependencyRepository.hasDependencies(repositoryId);
    }

    // ==================== Private Methods ====================

    private Repository getExistingRepository(String repositoryId) {
        Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
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

    private BuildMetadata getCompletedBuildMetadata(String repositoryId) {
        BuildMetadata buildMetadata = buildSystemService.getBuild(repositoryId);
        if (buildMetadata == null || buildMetadata.getBuildSystemType() == null) {
            throw new IllegalStateException(
                    "Build system analysis must be completed before dependency analysis for repository: " + repositoryId);
        }
        return buildMetadata;
    }

    private List<Dependency> classifyDependencies(List<Dependency> dependencies, BuildMetadata buildMetadata) {
        logger.info("Classifying {} dependencies for repository: {}", dependencies.size(), buildMetadata.getBuildSystemType());

        // Dependencies are already classified in the analyzer based on Maven scope
        // and Gradle configuration. This method can be extended for additional classification logic.
        
        String projectGroupId = buildMetadata.getGroupId();
        if (projectGroupId == null || projectGroupId.isEmpty()) {
            projectGroupId = buildMetadata.getGradleGroup();
        }

        for (Dependency dep : dependencies) {
            // Mark internal dependencies
            if (projectGroupId != null && !projectGroupId.isEmpty() 
                    && dep.getGroupId() != null && dep.getGroupId().startsWith(projectGroupId)) {
                dep.setInternal(true);
            } else {
                dep.setInternal(false);
            }
        }

        return dependencies;
    }
}