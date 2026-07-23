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
import java.util.List;
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

    public RepositoryService(RepositoryRepository repositoryRepository,
                            WorkspaceService workspaceService,
                            WorkspaceProperties workspaceProperties) {
        this.repositoryRepository = repositoryRepository;
        this.workspaceService = workspaceService;
        this.workspaceProperties = workspaceProperties;
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