package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.config.WorkspaceProperties;
import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.model.RepositoryStatus;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

/**
 * Service for managing repository lifecycle.
 */
@Service
public class RepositoryService {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryService.class);

    private final RepositoryRepository repositoryRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceProperties workspaceProperties;
    private final com.projectiq.indexerlocal.repository.IndexRepository indexRepository;

    public RepositoryService(RepositoryRepository repositoryRepository,
                            WorkspaceService workspaceService,
                            WorkspaceProperties workspaceProperties,
                            com.projectiq.indexerlocal.repository.IndexRepository indexRepository) {
        this.repositoryRepository = repositoryRepository;
        this.workspaceService = workspaceService;
        this.workspaceProperties = workspaceProperties;
        this.indexRepository = indexRepository;
    }

    /**
     * Register a new repository.
     */
    public Repository registerRepository(String path) {
        // Validate the path
        validatePath(path);

        // Check for duplicate by original path
        com.projectiq.indexerlocal.model.Repository existing = repositoryRepository.findByOriginalPath(path);
        if (existing != null) {
            throw new IllegalArgumentException("Repository with path '" + path + "' is already registered");
        }

        // Generate unique repository ID
        String repositoryId = generateRepositoryId();

        // Get repository name from path
        String repositoryName = extractRepositoryName(path);

        // Create workspace directory for this repository
        Path workspacePath = workspaceService.createRepositoryWorkspace(repositoryId, repositoryName);

        // Create repository entity
        com.projectiq.indexerlocal.model.Repository repository = new com.projectiq.indexerlocal.model.Repository();
        repository.setRepositoryId(repositoryId);
        repository.setRepositoryName(repositoryName);
        repository.setOriginalPath(path);
        repository.setWorkspacePath(workspacePath.toString());
        repository.setRegistrationTimestamp(LocalDateTime.now());
        repository.setLastUpdatedTimestamp(LocalDateTime.now());
        repository.setStatus(RepositoryStatus.REGISTERED);
        repository.setBuildSystem("Unknown");
        repository.setTechnologyStack("Unknown");

        // Save to database
        repositoryRepository.save(repository);

        logger.info("Repository registered: id={}, name={}, path={}", repositoryId, repositoryName, path);

        return repository;
    }

    /**
     * List all registered repositories.
     */
    public List<com.projectiq.indexerlocal.model.Repository> listRepositories() {
        return repositoryRepository.findAll();
    }

    /**
     * List repositories with pagination, sorting, and filtering.
     */
    public Map<String, Object> listRepositoriesWithPagination(int page, int size, String sortBy, String sortOrder, RepositoryStatus status) {
        // Validate and default parameters
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        if (sortBy == null || sortBy.isEmpty()) sortBy = "id";
        if (sortOrder == null || sortOrder.isEmpty()) sortOrder = "asc";

        List<com.projectiq.indexerlocal.model.Repository> repositories = repositoryRepository.findAllWithPagination(page, size, sortBy, sortOrder, status);

        // Get total count for pagination info
        Long totalCount;
        if (status != null) {
            totalCount = repositoryRepository.countByStatus(status);
        } else {
            totalCount = repositoryRepository.countAll();
        }

        int totalPages = (int) Math.ceil((double) totalCount / size);

        Map<String, Object> result = new HashMap<>();
        result.put("repositories", repositories);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", totalPages);
        result.put("totalCount", totalCount);
        result.put("hasNext", page < totalPages - 1);
        result.put("hasPrevious", page > 0);

        return result;
    }

    /**
     * Update repository metadata.
     */
    public com.projectiq.indexerlocal.model.Repository updateRepository(Long id, String name, String description, String workspacePath) {
        com.projectiq.indexerlocal.model.Repository repository = repositoryRepository.findById(id);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with id '" + id + "' not found");
        }

        // Update fields if provided
        if (name != null && !name.isEmpty()) {
            repository.setRepositoryName(name);
        }

        if (workspacePath != null && !workspacePath.isEmpty()) {
            // Validate workspace path if provided
            Path pathObj = Paths.get(workspacePath).toAbsolutePath().normalize();
            if (!Files.exists(pathObj)) {
                throw new IllegalArgumentException("Workspace directory does not exist: " + workspacePath);
            }
            repository.setWorkspacePath(workspacePath);
        }

        // Update timestamp
        repository.setLastUpdatedTimestamp(LocalDateTime.now());
        repositoryRepository.update(repository);

        logger.info("Repository updated: id={}, name={}", id, repository.getRepositoryName());
        return repository;
    }

    /**
     * Delete a repository and all its indexed data.
     */
    public void deleteRepositoryWithAllData(String repositoryId) {
        long startTime = System.currentTimeMillis();
        
        com.projectiq.indexerlocal.model.Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with repositoryId '" + repositoryId + "' not found");
        }

        // Check if indexing is in progress
        if (repository.getStatus() == RepositoryStatus.INDEXING || repository.getStatus() == RepositoryStatus.REFRESHING) {
            throw new IllegalStateException("Cannot delete repository while indexing is in progress: " + repositoryId);
        }

        logger.info("Starting deletion of repository: id={}, name={}", repositoryId, repository.getRepositoryName());

        // Delete all indexed data from SQLite tables via IndexRepository
        if (indexRepository != null) {
            try {
                indexRepository.deleteAllJavaIndexData(repositoryId);
                logger.info("Deleted Java index data for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete Java index data for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllRestApiIndexData(repositoryId);
                logger.info("Deleted REST API index data for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete REST API index data for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllSpringComponentData(repositoryId);
                logger.info("Deleted Spring Component index data for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete Spring Component index data for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllDependencyData(repositoryId);
                logger.info("Deleted dependency metadata for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete dependency metadata for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllConfigurationData(repositoryId);
                logger.info("Deleted configuration metadata for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete configuration metadata for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllDatabaseData(repositoryId);
                logger.info("Deleted database metadata for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete database metadata for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllStatistics(repositoryId);
                logger.info("Deleted statistics for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete statistics for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllIndexingHistory(repositoryId);
                logger.info("Deleted indexing history for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete indexing history for repository {}: {}", repositoryId, e.getMessage());
            }
        }

        // Delete workspace and repository record via RepositoryRepository
        repositoryRepository.deleteAllDataByRepositoryId(repositoryId);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Repository and all indexed data fully deleted: id={}, name={}, duration={}ms", repositoryId, repository.getRepositoryName(), duration);
    }

    /**
     * Get repository management statistics.
     */
    public Map<String, Object> getRepositoryStatistics() {
        // Count repositories by status from RepositoryRepository
        Long totalRepositories = repositoryRepository.countAll();
        Long readyRepositories = repositoryRepository.countByStatus(RepositoryStatus.READY);
        Long failedRepositories = repositoryRepository.countByStatus(RepositoryStatus.FAILED);
        Long indexingRepositories = repositoryRepository.countByStatus(RepositoryStatus.INDEXING);
        Long registeredRepositories = repositoryRepository.countByStatus(RepositoryStatus.REGISTERED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRepositories", totalRepositories != null ? totalRepositories : 0);
        stats.put("readyRepositories", readyRepositories != null ? readyRepositories : 0);
        stats.put("failedRepositories", failedRepositories != null ? failedRepositories : 0);
        stats.put("indexingRepositories", indexingRepositories != null ? indexingRepositories : 0);
        stats.put("registeredRepositories", registeredRepositories != null ? registeredRepositories : 0);

        return stats;
    }

    /**
     * Get a repository by ID.
     */
    public com.projectiq.indexerlocal.model.Repository getRepository(Long id) {
        com.projectiq.indexerlocal.model.Repository repository = repositoryRepository.findById(id);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with id '" + id + "' not found");
        }
        return repository;
    }

    /**
     * Get a repository by its unique repository ID.
     */
    public com.projectiq.indexerlocal.model.Repository getRepositoryByRepositoryId(String repositoryId) {
        com.projectiq.indexerlocal.model.Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with repositoryId '" + repositoryId + "' not found");
        }
        return repository;
    }

    /**
     * Delete a repository.
     */
    public void deleteRepository(Long id) {
        com.projectiq.indexerlocal.model.Repository repository = repositoryRepository.findById(id);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with id '" + id + "' not found");
        }

        // Delete workspace directory if it exists
        try {
            Path workspacePath = Paths.get(repository.getWorkspacePath());
            if (Files.exists(workspacePath)) {
                deleteDirectory(workspacePath);
                logger.info("Repository workspace deleted: {}", workspacePath);
            }
        } catch (Exception e) {
            logger.warn("Failed to delete repository workspace: {}", repository.getWorkspacePath(), e);
        }

        // Delete from database
        repositoryRepository.deleteById(id);

        logger.info("Repository removed: id={}, name={}", id, repository.getRepositoryName());
    }

    /**
     * Update repository status.
     */
    public void updateStatus(Long id, RepositoryStatus status) {
        com.projectiq.indexerlocal.model.Repository repository = repositoryRepository.findById(id);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with id '" + id + "' not found");
        }

        repository.setStatus(status);
        repository.setLastUpdatedTimestamp(LocalDateTime.now());
        repositoryRepository.update(repository);

        logger.info("Repository status updated: id={}, status={}", id, status);
    }

    /**
     * Validate the provided path.
     */
    private void validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path must not be null or empty");
        }

        Path pathObj = Paths.get(path).toAbsolutePath().normalize();

        if (!Files.exists(pathObj)) {
            throw new IllegalArgumentException("Directory does not exist: " + path);
        }

        if (!Files.isDirectory(pathObj)) {
            throw new IllegalArgumentException("Path is not a directory: " + path);
        }
    }

    /**
     * Generate a unique repository ID.
     */
    private String generateRepositoryId() {
        return "repo_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * Extract the repository name from the path.
     */
    private String extractRepositoryName(String path) {
        Path pathObj = Paths.get(path);
        String name = pathObj.getFileName() != null ? pathObj.getFileName().toString() : path;
        
        // If the name is empty or just ".", use the absolute path as fallback
        if (name.isEmpty() || ".".equals(name)) {
            name = pathObj.getParent() != null ? pathObj.getParent().getFileName().toString() : "unknown";
        }
        
        return name;
    }

    /**
     * Recursively delete a directory.
     */
    private void deleteDirectory(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            Files.list(path).forEach(child -> {
                try {
                    if (Files.isDirectory(child)) {
                        deleteDirectory(child);
                    } else {
                        Files.delete(child);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to delete: {}", child, e);
                }
            });
        }
        Files.delete(path);
    }
}