package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.config.WorkspaceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for managing the workspace directory.
 */
@Service
public class WorkspaceService {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceProperties workspaceProperties;
    private Path workspaceRootPath;

    public WorkspaceService(WorkspaceProperties workspaceProperties) {
        this.workspaceProperties = workspaceProperties;
    }

    /**
     * Initialize the workspace on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeWorkspace() {
        if (workspaceProperties.isAutoCreate()) {
            createWorkspace();
        }

        if (workspaceProperties.isVerifyWritable()) {
            verifyWritable();
        }
    }

    /**
     * Create the workspace directory if it does not exist.
     */
    public void createWorkspace() {
        workspaceRootPath = Paths.get(workspaceProperties.getRootDir()).toAbsolutePath().normalize();
        
        try {
            if (Files.exists(workspaceRootPath)) {
                logger.info("Workspace already exists at: {}", workspaceRootPath);
                return;
            }

            Files.createDirectories(workspaceRootPath);
            logger.info("Workspace created at: {}", workspaceRootPath);
        } catch (IOException e) {
            logger.error("Failed to create workspace directory: {}", workspaceRootPath, e);
            throw new RuntimeException("Failed to create workspace directory: " + workspaceRootPath, e);
        }
    }

    /**
     * Verify the workspace is writable.
     */
    public void verifyWritable() {
        if (workspaceRootPath == null) {
            workspaceRootPath = Paths.get(workspaceProperties.getRootDir()).toAbsolutePath().normalize();
        }

        if (!Files.exists(workspaceRootPath)) {
            logger.error("Workspace does not exist: {}", workspaceRootPath);
            throw new RuntimeException("Workspace does not exist: " + workspaceRootPath);
        }

        if (!Files.isWritable(workspaceRootPath)) {
            logger.error("Workspace is not writable: {}", workspaceRootPath);
            throw new RuntimeException("Workspace is not writable: " + workspaceRootPath);
        }

        logger.info("Workspace verified as writable: {}", workspaceRootPath);
    }

    /**
     * Get the workspace root path.
     */
    public Path getWorkspaceRootPath() {
        if (workspaceRootPath == null) {
            workspaceRootPath = Paths.get(workspaceProperties.getRootDir()).toAbsolutePath().normalize();
        }
        return workspaceRootPath;
    }

    /**
     * Create a subdirectory in the workspace for a repository.
     */
    public Path createRepositoryWorkspace(String repositoryId, String repositoryName) {
        Path repoPath = workspaceRootPath.resolve(repositoryId).normalize();
        
        try {
            if (!Files.exists(repoPath)) {
                Files.createDirectories(repoPath);
                logger.info("Repository workspace created at: {}", repoPath);
            } else {
                logger.info("Repository workspace already exists at: {}", repoPath);
            }
        } catch (IOException e) {
            logger.error("Failed to create repository workspace at: {}", repoPath, e);
            throw new RuntimeException("Failed to create repository workspace: " + repoPath, e);
        }
        
        return repoPath;
    }

    /**
     * Get the workspace path for a repository.
     */
    public String getRepositoryWorkspacePath(String repositoryId) {
        if (workspaceRootPath == null) {
            workspaceRootPath = Paths.get(workspaceProperties.getRootDir()).toAbsolutePath().normalize();
        }
        return workspaceRootPath.resolve(repositoryId).toString();
    }
}