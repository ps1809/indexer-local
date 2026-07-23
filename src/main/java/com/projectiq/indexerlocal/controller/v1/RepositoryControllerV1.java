package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.response.RepositoryResponse;
import com.projectiq.indexerlocal.service.RepositoryService;
import com.projectiq.indexerlocal.service.RepositoryRefreshService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projectiq.indexerlocal.model.RepositoryStatus;

import java.util.List;
import java.util.Map;

/**
 * REST controller for repository management APIs.
 */
@RestController
@RequestMapping("/api/v1/repositories")
@Tag(name = "Repository Management", description = "APIs for managing registered repositories")
public class RepositoryControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryControllerV1.class);

    private final RepositoryService repositoryService;
    private final RepositoryRefreshService repositoryRefreshService;

    public RepositoryControllerV1(RepositoryService repositoryService, RepositoryRefreshService repositoryRefreshService) {
        this.repositoryService = repositoryService;
        this.repositoryRefreshService = repositoryRefreshService;
    }

    /**
     * Register a new repository.
     */
    @PostMapping
    @Operation(summary = "Register a repository", description = "Register a new repository for indexing")
    public ResponseEntity<RepositoryResponse> registerRepository(@RequestBody com.projectiq.indexerlocal.model.api.request.RepositoryRegisterRequest request) {
        logger.info("Received repository registration request: path={}", request.getPath());
        
        com.projectiq.indexerlocal.model.Repository repository = repositoryService.registerRepository(request.getPath());
        
        RepositoryResponse response = toResponse(repository);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all registered repositories with optional pagination, sorting, and filtering.
     */
    @GetMapping
    @Operation(summary = "List repositories", description = "List all registered repositories with optional filtering and pagination")
    public ResponseEntity<Map<String, Object>> listRepositories(
            @RequestParam(required = false) RepositoryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        logger.info("Listing all repositories with pagination: page={}, size={}, sortBy={}, sortOrder={}, status={}", 
            page, size, sortBy, sortOrder, status);
        
        Map<String, Object> result = repositoryService.listRepositoriesWithPagination(page, size, sortBy, sortOrder, status);
        
        // Convert repository objects to response DTOs
        List<RepositoryResponse> responses = ((List<com.projectiq.indexerlocal.model.Repository>) result.get("repositories")).stream()
            .map(this::toResponse)
            .toList();
        
        result.put("repositories", responses);
        return ResponseEntity.ok(result);
    }

    /**
     * Get a repository by internal ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get repository details", description = "Retrieve details of a specific repository by its internal ID")
    public ResponseEntity<RepositoryResponse> getRepository(@Parameter(description = "Repository internal ID") @PathVariable("id") Long id) {
        logger.info("Getting repository by id: {}", id);
        
        com.projectiq.indexerlocal.model.Repository repository = repositoryService.getRepository(id);
        RepositoryResponse response = toResponse(repository);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get a repository by its unique repository ID.
     */
    @GetMapping("/by-id/{repositoryId}")
    @Operation(summary = "Get repository by repositoryId", description = "Retrieve details of a specific repository by its unique repository ID")
    public ResponseEntity<RepositoryResponse> getRepositoryByRepositoryId(
            @Parameter(description = "Repository unique ID") @PathVariable("repositoryId") String repositoryId) {
        logger.info("Getting repository by repositoryId: {}", repositoryId);
        
        com.projectiq.indexerlocal.model.Repository repository = repositoryService.getRepositoryByRepositoryId(repositoryId);
        RepositoryResponse response = toResponse(repository);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update repository metadata.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update repository", description = "Update repository metadata such as name and workspace path")
    public ResponseEntity<RepositoryResponse> updateRepository(
            @Parameter(description = "Repository internal ID") @PathVariable("id") Long id,
            @RequestBody Map<String, String> request) {
        logger.info("Updating repository: id={}", id);
        
        String name = request.get("name");
        String description = request.get("description");
        String workspacePath = request.get("workspacePath");
        
        com.projectiq.indexerlocal.model.Repository repository = repositoryService.updateRepository(id, name, description, workspacePath);
        RepositoryResponse response = toResponse(repository);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a repository and all its indexed data by unique repository ID.
     */
    @DeleteMapping("/by-id/{repositoryId}")
    @Operation(summary = "Delete repository with all data", description = "Remove a repository registration, its workspace, and all indexed data")
    public ResponseEntity<Void> deleteRepositoryWithAllData(
            @Parameter(description = "Repository unique ID") @PathVariable("repositoryId") String repositoryId) {
        logger.info("Deleting repository with all data by repositoryId: {}", repositoryId);
        
        repositoryService.deleteRepositoryWithAllData(repositoryId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Get repository management statistics.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get repository statistics", description = "Retrieve repository management statistics including counts by status")
    public ResponseEntity<Map<String, Object>> getRepositoryStatistics() {
        logger.info("Getting repository management statistics");
        
        Map<String, Object> stats = repositoryService.getRepositoryStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Refresh repository metadata.
     */
    @PostMapping("/{repositoryId}/refresh")
    @Operation(summary = "Refresh repository", description = "Refresh repository metadata, verify workspace, and update timestamps")
    @ApiResponse(responseCode = "200", description = "Refresh completed successfully")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<Map<String, Object>> refreshRepository(
            @Parameter(description = "Repository ID") @PathVariable Long repositoryId) {
        logger.info("Received repository refresh request: repositoryId={}", repositoryId);
        
        try {
            // Convert Long to String for service layer which uses String repository IDs
            com.projectiq.indexerlocal.model.Repository repo = repositoryService.getRepository(repositoryId);
            Map<String, Object> result = repositoryRefreshService.refreshRepository(repo.getRepositoryId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to refresh repository {}: {}", repositoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Trigger full re-index of a repository.
     */
    @PostMapping("/{repositoryId}/reindex")
    @Operation(summary = "Full re-index", description = "Execute complete repository re-index from scratch")
    @ApiResponse(responseCode = "200", description = "Re-index completed successfully")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<Map<String, Object>> reindexRepository(
            @Parameter(description = "Repository ID") @PathVariable Long repositoryId) {
        logger.info("Received full re-index request: repositoryId={}", repositoryId);
        
        try {
            // Convert Long to String for service layer which uses String repository IDs
            com.projectiq.indexerlocal.model.Repository repo = repositoryService.getRepository(repositoryId);
            Map<String, Object> result = repositoryRefreshService.fullReindex(repo.getRepositoryId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to re-index repository {}: {}", repositoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Trigger incremental re-index of a repository.
     */
    @PostMapping("/{repositoryId}/reindex/incremental")
    @Operation(summary = "Incremental re-index", description = "Execute incremental re-index updating only changed files")
    @ApiResponse(responseCode = "200", description = "Incremental re-index completed successfully")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<Map<String, Object>> incrementalReindexRepository(
            @Parameter(description = "Repository ID") @PathVariable Long repositoryId) {
        logger.info("Received incremental re-index request: repositoryId={}", repositoryId);
        
        try {
            // Convert Long to String for service layer which uses String repository IDs
            com.projectiq.indexerlocal.model.Repository repo = repositoryService.getRepository(repositoryId);
            Map<String, Object> result = repositoryRefreshService.incrementalReindex(repo.getRepositoryId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to incrementally re-index repository {}: {}", repositoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current repository status.
     */
    @GetMapping("/{repositoryId}/status")
    @Operation(summary = "Get repository status", description = "Retrieve the current status of a registered repository")
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<Map<String, Object>> getRepositoryStatus(
            @Parameter(description = "Repository ID") @PathVariable Long repositoryId) {
        logger.info("Getting repository status: repositoryId={}", repositoryId);
        
        try {
            // Convert Long to String for service layer which uses String repository IDs
            com.projectiq.indexerlocal.model.Repository repo = repositoryService.getRepository(repositoryId);
            Map<String, Object> result = repositoryRefreshService.getRepositoryStatus(repo.getRepositoryId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to get status for repository {}: {}", repositoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Copy repository data to response DTO.
     */
    private RepositoryResponse toResponse(com.projectiq.indexerlocal.model.Repository repository) {
        RepositoryResponse response = new RepositoryResponse();
        response.setId(repository.getId());
        response.setRepositoryId(repository.getRepositoryId());
        response.setRepositoryName(repository.getRepositoryName());
        response.setOriginalPath(repository.getOriginalPath());
        response.setWorkspacePath(repository.getWorkspacePath());
        response.setRegistrationTimestamp(repository.getRegistrationTimestamp());
        response.setLastUpdatedTimestamp(repository.getLastUpdatedTimestamp());
        response.setLastRefreshTimestamp(repository.getLastRefreshTimestamp());
        response.setLastIndexingTimestamp(repository.getLastIndexingTimestamp());
        response.setStatus(repository.getStatus());
        response.setBuildSystem(repository.getBuildSystem());
        response.setTechnologyStack(repository.getTechnologyStack());
        return response;
    }
}
