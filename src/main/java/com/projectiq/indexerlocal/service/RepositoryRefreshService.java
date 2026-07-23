package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.config.WorkspaceProperties;
import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.model.RepositoryRefreshHistory;
import com.projectiq.indexerlocal.model.RepositoryStatus;
import com.projectiq.indexerlocal.model.IncrementalIndexingStatistics;
import com.projectiq.indexerlocal.repository.IndexRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import com.projectiq.indexerlocal.repository.RepositoryRefreshHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for repository refresh and re-index operations.
 * Orchestrates existing services to refresh metadata, trigger full re-index,
 * or trigger incremental re-index.
 */
@Service
public class RepositoryRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryRefreshService.class);

    private final RepositoryRepository repositoryRepository;
    private final RepositoryRefreshHistoryRepository refreshHistoryRepository;
    private final IndexRepository indexRepository;
    private final WorkspaceProperties workspaceProperties;
    private final BuildSystemService buildSystemService;
    private final ProjectStructureService projectStructureService;
    private final TechnologyStackService technologyStackService;
    private final DependencyService dependencyService;
    private final ConfigurationService configurationService;
    private final DatabaseService databaseService;
    private final SpringComponentService springComponentService;
    private final RestApiService restApiService;
    private final JavaCodeIndexer javaCodeIndexer;
    private final IncrementalIndexingService incrementalIndexingService;
    private final RepositoryStatisticsService repositoryStatisticsService;

    public RepositoryRefreshService(RepositoryRepository repositoryRepository,
                                    RepositoryRefreshHistoryRepository refreshHistoryRepository,
                                    IndexRepository indexRepository,
                                    WorkspaceProperties workspaceProperties,
                                    BuildSystemService buildSystemService,
                                    ProjectStructureService projectStructureService,
                                    TechnologyStackService technologyStackService,
                                    DependencyService dependencyService,
                                    ConfigurationService configurationService,
                                    DatabaseService databaseService,
                                    SpringComponentService springComponentService,
                                    RestApiService restApiService,
                                    JavaCodeIndexer javaCodeIndexer,
                                    IncrementalIndexingService incrementalIndexingService,
                                    RepositoryStatisticsService repositoryStatisticsService) {
        this.repositoryRepository = repositoryRepository;
        this.refreshHistoryRepository = refreshHistoryRepository;
        this.indexRepository = indexRepository;
        this.workspaceProperties = workspaceProperties;
        this.buildSystemService = buildSystemService;
        this.projectStructureService = projectStructureService;
        this.technologyStackService = technologyStackService;
        this.dependencyService = dependencyService;
        this.configurationService = configurationService;
        this.databaseService = databaseService;
        this.springComponentService = springComponentService;
        this.restApiService = restApiService;
        this.javaCodeIndexer = javaCodeIndexer;
        this.incrementalIndexingService = incrementalIndexingService;
        this.repositoryStatisticsService = repositoryStatisticsService;
    }

    /**
     * Refresh repository metadata: verify path, update timestamps and status.
     */
    public Map<String, Object> refreshRepository(String repositoryId) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting refresh for repository: {}", repositoryId);

        // Record refresh history
        RepositoryRefreshHistory history = refreshHistoryRepository.create(repositoryId, "REFRESH");

        try {
            // Verify repository exists
            Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
            if (repository == null) {
                throw new IllegalArgumentException("Repository not found: " + repositoryId);
            }

            logger.info("Execution order: Project Structure Analyzer -> Build System Analyzer -> Technology Stack Detection -> Dependency Analyzer -> Configuration Analyzer -> Database Analyzer -> Spring Component Analyzer -> REST API Analyzer");

            // Verify workspace path exists
            Path workspacePath = Paths.get(repository.getWorkspacePath());
            if (!Files.exists(workspacePath)) {
                throw new IOException("Workspace path does not exist: " + repository.getWorkspacePath());
            }

            // Update repository status to REFRESHING
            repository.setStatus(RepositoryStatus.REFRESHING);
            repository.setLastUpdatedTimestamp(LocalDateTime.now());
            repositoryRepository.save(repository);

            // Re-run all analyzers in correct order
            logger.info("Executing analyzers for refresh...");

            // 1. Project Structure Analyzer
            projectStructureService.analyzeStructure(repositoryId);
            logger.info("Project Structure Analyzer completed");

            // 2. Build System Analyzer (method is analyzeBuild)
            buildSystemService.analyzeBuild(repositoryId);
            logger.info("Build System Analyzer completed");

            // 3. Technology Stack Detection (method is detectTechnologyStack)
            technologyStackService.detectTechnologyStack(repositoryId);
            logger.info("Technology Stack Detection completed");

            // 4. Dependency Analyzer
            dependencyService.analyzeDependencies(repositoryId);
            logger.info("Dependency Analyzer completed");

            // 5. Configuration Analyzer
            configurationService.analyzeConfigurations(repositoryId);
            logger.info("Configuration Analyzer completed");

            // 6. Database Analyzer
            databaseService.analyzeDatabases(repositoryId);
            logger.info("Database Analyzer completed");

            // 7. Spring Component Analyzer
            springComponentService.analyzeSpringComponents(repositoryId);
            logger.info("Spring Component Analyzer completed");

            // 8. REST API Analyzer
            restApiService.analyzeRestApis(repositoryId);
            logger.info("REST API Analyzer completed");

            // Update status to READY
            repository.setStatus(RepositoryStatus.READY);
            repository.setLastUpdatedTimestamp(LocalDateTime.now());
            repositoryRepository.save(repository);

            long duration = System.currentTimeMillis() - startTime;
            refreshHistoryRepository.updateCompletion(repositoryId, "REFRESH", "SUCCESS", duration, null);

            logger.info("Refresh completed for repository: {} in {} ms", repositoryId, duration);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Repository metadata refreshed successfully");
            result.put("repositoryId", repositoryId);
            result.put("durationMs", duration);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            refreshHistoryRepository.updateCompletion(repositoryId, "REFRESH", "FAILED", duration, e.getMessage());
            logger.error("Refresh failed for repository: {}", repositoryId, e);

            // Update status to FAILED
            try {
                Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
                if (repository != null) {
                    repository.setStatus(RepositoryStatus.FAILED);
                    repositoryRepository.save(repository);
                }
            } catch (Exception ex) {
                logger.error("Failed to update repository status", ex);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Refresh failed: " + e.getMessage());
            result.put("repositoryId", repositoryId);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Execute full re-index of the repository.
     */
    public Map<String, Object> fullReindex(String repositoryId) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting full re-index for repository: {}", repositoryId);

        // Record refresh history
        refreshHistoryRepository.create(repositoryId, "FULL_REINDEX");

        try {
            // Verify repository exists
            Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
            if (repository == null) {
                throw new IllegalArgumentException("Repository not found: " + repositoryId);
            }

            // Verify workspace path exists
            Path workspacePath = Paths.get(repository.getWorkspacePath());
            if (!Files.exists(workspacePath)) {
                throw new IOException("Workspace path does not exist: " + repository.getWorkspacePath());
            }

            // Update status to INDEXING
            repository.setStatus(RepositoryStatus.INDEXING);
            repository.setLastUpdatedTimestamp(LocalDateTime.now());
            repositoryRepository.save(repository);

            logger.info("Execution order for full re-index: Project Structure Analyzer -> Build System Analyzer -> Technology Stack Detection -> Dependency Analyzer -> Configuration Analyzer -> Database Analyzer -> Spring Component Analyzer -> REST API Analyzer -> Java Code Indexing Engine -> Repository Statistics & Summary");

            // 1. Delete existing index data
            logger.info("Deleting existing index data...");
            indexRepository.deleteAllByRepositoryId(repositoryId);
            logger.info("Existing index data deleted");

            // 2. Run all analyzers in correct order
            projectStructureService.analyzeStructure(repositoryId);
            logger.info("Project Structure Analyzer completed");

            buildSystemService.analyzeBuild(repositoryId);
            logger.info("Build System Analyzer completed");

            technologyStackService.detectTechnologyStack(repositoryId);
            logger.info("Technology Stack Detection completed");

            dependencyService.analyzeDependencies(repositoryId);
            logger.info("Dependency Analyzer completed");

            configurationService.analyzeConfigurations(repositoryId);
            logger.info("Configuration Analyzer completed");

            databaseService.analyzeDatabases(repositoryId);
            logger.info("Database Analyzer completed");

            springComponentService.analyzeSpringComponents(repositoryId);
            logger.info("Spring Component Analyzer completed");

            restApiService.analyzeRestApis(repositoryId);
            logger.info("REST API Analyzer completed");

            // 3. Java Code Indexing Engine (replace existing index)
            logger.info("Starting Java Code Indexing Engine...");
            JavaCodeIndexer.JavaIndexResult indexingResult = javaCodeIndexer.indexRepository(repositoryId, workspacePath.toString());
            logger.info("Java Code Indexing Engine completed: {}", indexingResult != null ? "success" : "failed");

            // Update status to INDEXED
            repository.setStatus(RepositoryStatus.INDEXED);
            repository.setLastUpdatedTimestamp(LocalDateTime.now());
            repositoryRepository.save(repository);

            // 4. Update repository statistics
            Map<String, Object> statistics = repositoryStatisticsService.getRepositoryStatistics(repositoryId);
            logger.info("Repository statistics updated");

            long duration = System.currentTimeMillis() - startTime;
            refreshHistoryRepository.updateCompletion(repositoryId, "FULL_REINDEX", "SUCCESS", duration, null);

            logger.info("Full re-index completed for repository: {} in {} ms", repositoryId, duration);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Full re-index completed successfully");
            result.put("repositoryId", repositoryId);
            result.put("durationMs", duration);
            result.put("statistics", statistics);
            if (indexingResult != null && indexingResult.getStatistics() != null) {
                result.put("totalFilesIndexed", indexingResult.getStatistics().getTotalJavaFiles());
                result.put("totalClassesFound", indexingResult.getStatistics().getTotalClasses());
                result.put("totalMethodsFound", indexingResult.getStatistics().getTotalMethods());
                result.put("totalFieldsFound", indexingResult.getStatistics().getTotalFields());
            }
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            refreshHistoryRepository.updateCompletion(repositoryId, "FULL_REINDEX", "FAILED", duration, e.getMessage());
            logger.error("Full re-index failed for repository: {}", repositoryId, e);

            // Update status to FAILED
            try {
                Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
                if (repository != null) {
                    repository.setStatus(RepositoryStatus.FAILED);
                    repositoryRepository.save(repository);
                }
            } catch (Exception ex) {
                logger.error("Failed to update repository status", ex);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Full re-index failed: " + e.getMessage());
            result.put("repositoryId", repositoryId);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Execute incremental re-index of the repository.
     */
    public Map<String, Object> incrementalReindex(String repositoryId) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting incremental re-index for repository: {}", repositoryId);

        // Record refresh history
        refreshHistoryRepository.create(repositoryId, "INCREMENTAL_REINDEX");

        try {
            // Verify repository exists
            Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
            if (repository == null) {
                throw new IllegalArgumentException("Repository not found: " + repositoryId);
            }

            // Verify workspace path exists
            Path workspacePath = Paths.get(repository.getWorkspacePath());
            if (!Files.exists(workspacePath)) {
                throw new IOException("Workspace path does not exist: " + repository.getWorkspacePath());
            }

            // Update status to INDEXING
            repository.setStatus(RepositoryStatus.INDEXING);
            repository.setLastUpdatedTimestamp(LocalDateTime.now());
            repositoryRepository.save(repository);

            logger.info("Execution order for incremental re-index: Project Structure Analyzer -> Build System Analyzer -> Technology Stack Detection -> Dependency Analyzer -> Configuration Analyzer -> Database Analyzer -> Spring Component Analyzer -> REST API Analyzer -> Java Code Indexing Engine (incremental)");

            // Run all analyzers with incremental approach
            projectStructureService.analyzeStructure(repositoryId);
            logger.info("Project Structure Analyzer completed (incremental)");

            buildSystemService.analyzeBuild(repositoryId);
            logger.info("Build System Analyzer completed (incremental)");

            technologyStackService.detectTechnologyStack(repositoryId);
            logger.info("Technology Stack Detection completed (incremental)");

            dependencyService.analyzeDependencies(repositoryId);
            logger.info("Dependency Analyzer completed (incremental)");

            configurationService.analyzeConfigurations(repositoryId);
            logger.info("Configuration Analyzer completed (incremental)");

            databaseService.analyzeDatabases(repositoryId);
            logger.info("Database Analyzer completed (incremental)");

            springComponentService.analyzeSpringComponents(repositoryId);
            logger.info("Spring Component Analyzer completed (incremental)");

            restApiService.analyzeRestApis(repositoryId);
            logger.info("REST API Analyzer completed (incremental)");

            // Incremental Java Code Indexing via existing service
            logger.info("Starting incremental indexing via IncrementalIndexingService...");

            Map<String, Object> incrementalResult = new HashMap<>();
            try {
                IncrementalIndexingStatistics incrStats = incrementalIndexingService.performIncrementalIndexing(repositoryId);
                incrementalResult.put("filesAdded", incrStats.getFilesAdded());
                incrementalResult.put("filesModified", incrStats.getFilesModified());
                incrementalResult.put("filesDeleted", incrStats.getFilesDeleted());
                incrementalResult.put("filesUnchanged", incrStats.getFilesUnchanged());
                incrementalResult.put("status", incrStats.getStatus());
                incrementalResult.put("processingTimeMs", incrStats.getProcessingTime() != null ? incrStats.getProcessingTime().toMillis() : 0L);
                logger.info("Incremental indexing completed: added={}, modified={}, deleted={}, unchanged={}",
                        incrStats.getFilesAdded(), incrStats.getFilesModified(),
                        incrStats.getFilesDeleted(), incrStats.getFilesUnchanged());
            } catch (Exception e) {
                // If incremental fails, fall back to full re-index
                logger.error("Incremental indexing failed, falling back to full re-index", e);
                JavaCodeIndexer.JavaIndexResult fallbackResult = javaCodeIndexer.indexRepository(repositoryId, workspacePath.toString());
                incrementalResult.put("filesAdded", 0);
                incrementalResult.put("filesModified", 0);
                incrementalResult.put("filesDeleted", 0);
                incrementalResult.put("filesUnchanged", 0);
                incrementalResult.put("status", "FULL_FALLBACK");
                incrementalResult.put("result", fallbackResult != null ? "success" : "failed");
            }

            // Update repository statistics
            Map<String, Object> statistics = repositoryStatisticsService.getRepositoryStatistics(repositoryId);

            // Update status to INDEXED
            repository.setStatus(RepositoryStatus.INDEXED);
            repository.setLastUpdatedTimestamp(LocalDateTime.now());
            repositoryRepository.save(repository);

            long duration = System.currentTimeMillis() - startTime;
            refreshHistoryRepository.updateCompletion(repositoryId, "INCREMENTAL_REINDEX", "SUCCESS", duration, null);

            logger.info("Incremental re-index completed for repository: {} in {} ms", repositoryId, duration);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Incremental re-index completed successfully");
            result.put("repositoryId", repositoryId);
            result.put("durationMs", duration);
            result.put("statistics", statistics);
            result.put("incrementalResult", incrementalResult);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            refreshHistoryRepository.updateCompletion(repositoryId, "INCREMENTAL_REINDEX", "FAILED", duration, e.getMessage());
            logger.error("Incremental re-index failed for repository: {}", repositoryId, e);

            // Update status to FAILED
            try {
                Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
                if (repository != null) {
                    repository.setStatus(RepositoryStatus.FAILED);
                    repositoryRepository.save(repository);
                }
            } catch (Exception ex) {
                logger.error("Failed to update repository status", ex);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Incremental re-index failed: " + e.getMessage());
            result.put("repositoryId", repositoryId);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Get current repository status.
     */
    public Map<String, Object> getRepositoryStatus(String repositoryId) {
        try {
            Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
            if (repository == null) {
                throw new IllegalArgumentException("Repository not found: " + repositoryId);
            }

            // Get latest refresh history
            RepositoryRefreshHistory latestHistory = refreshHistoryRepository.getLatestHistory(repositoryId);

            Map<String, Object> status = new HashMap<>();
            status.put("repositoryId", repository.getRepositoryId());
            status.put("repositoryName", repository.getRepositoryName());
            status.put("status", repository.getStatus().toString());
            status.put("registrationTimestamp", repository.getRegistrationTimestamp());
            status.put("lastUpdatedTimestamp", repository.getLastUpdatedTimestamp());
            status.put("buildSystem", repository.getBuildSystem());
            status.put("technologyStack", repository.getTechnologyStack());

            if (latestHistory != null) {
                status.put("lastRefreshType", latestHistory.getRefreshType());
                status.put("lastRefreshStatus", latestHistory.getStatus());
                status.put("lastRefreshTime", latestHistory.getStartTime());
                status.put("lastRefreshDurationMs", latestHistory.getExecutionDurationMs());
            }

            return status;

        } catch (Exception e) {
            logger.error("Failed to get repository status for: {}", repositoryId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Failed to get repository status: " + e.getMessage());
            result.put("repositoryId", repositoryId);
            return result;
        }
    }

    /**
     * Get recent refresh history for a repository.
     */
    public List<RepositoryRefreshHistory> getRefreshHistory(String repositoryId, int limit) {
        return refreshHistoryRepository.getRecentHistory(repositoryId, limit);
    }
}