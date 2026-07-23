package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.response.RepositoryResponse;
import com.projectiq.indexerlocal.service.RepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for repository management APIs.
 */
@RestController
@RequestMapping("/api/v1/repositories")
@Tag(name = "Repository Management", description = "APIs for managing registered repositories")
public class RepositoryControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryControllerV1.class);

    private final RepositoryService repositoryService;

    public RepositoryControllerV1(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Register a new repository.
     */
    @PostMapping
    @Operation(summary = "Register a repository", description = "Register a new repository for indexing")
    public ResponseEntity<RepositoryResponse> registerRepository(@RequestBody com.projectiq.indexerlocal.model.api.request.RepositoryRegisterRequest request) {
        logger.info("Received repository registration request: path={}", request.getPath());
        
        RepositoryResponse response = new RepositoryResponse();
        com.projectiq.indexerlocal.model.Repository repository = repositoryService.registerRepository(request.getPath());
        
        copyToResponse(repository, response);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all registered repositories.
     */
    @GetMapping
    @Operation(summary = "List repositories", description = "List all registered repositories")
    public ResponseEntity<List<RepositoryResponse>> listRepositories() {
        logger.info("Listing all repositories");
        
        List<com.projectiq.indexerlocal.model.Repository> repositories = repositoryService.listRepositories();
        List<RepositoryResponse> responses = repositories.stream()
            .map(this::copyToResponse)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a repository by ID.
     */
    @GetMapping("/{repositoryId}")
    @Operation(summary = "Get repository details", description = "Retrieve details of a specific repository by its internal ID")
    public ResponseEntity<RepositoryResponse> getRepository(@PathVariable Long repositoryId) {
        logger.info("Getting repository by id: {}", repositoryId);
        
        RepositoryResponse response = new RepositoryResponse();
        com.projectiq.indexerlocal.model.Repository repository = repositoryService.getRepository(repositoryId);
        
        copyToResponse(repository, response);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a repository.
     */
    @DeleteMapping("/{repositoryId}")
    @Operation(summary = "Delete repository", description = "Remove a repository registration and its workspace")
    public ResponseEntity<Void> deleteRepository(@PathVariable Long repositoryId) {
        logger.info("Deleting repository by id: {}", repositoryId);
        
        repositoryService.deleteRepository(repositoryId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Copy repository data to response DTO.
     */
    private RepositoryResponse copyToResponse(com.projectiq.indexerlocal.model.Repository repository) {
        RepositoryResponse response = new RepositoryResponse();
        response.setId(repository.getId());
        response.setRepositoryId(repository.getRepositoryId());
        response.setRepositoryName(repository.getRepositoryName());
        response.setOriginalPath(repository.getOriginalPath());
        response.setWorkspacePath(repository.getWorkspacePath());
        response.setRegistrationTimestamp(repository.getRegistrationTimestamp());
        response.setLastUpdatedTimestamp(repository.getLastUpdatedTimestamp());
        response.setStatus(repository.getStatus());
        response.setBuildSystem(repository.getBuildSystem());
        response.setTechnologyStack(repository.getTechnologyStack());
        return response;
    }

    private void copyToResponse(com.projectiq.indexerlocal.model.Repository repository, RepositoryResponse response) {
        response.setId(repository.getId());
        response.setRepositoryId(repository.getRepositoryId());
        response.setRepositoryName(repository.getRepositoryName());
        response.setOriginalPath(repository.getOriginalPath());
        response.setWorkspacePath(repository.getWorkspacePath());
        response.setRegistrationTimestamp(repository.getRegistrationTimestamp());
        response.setLastUpdatedTimestamp(repository.getLastUpdatedTimestamp());
        response.setStatus(repository.getStatus());
        response.setBuildSystem(repository.getBuildSystem());
        response.setTechnologyStack(repository.getTechnologyStack());
    }
}