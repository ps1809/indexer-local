package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.SpringComponent;
import com.projectiq.indexerlocal.model.SpringComponentStatistics;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.service.SpringComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Spring Component Analysis.
 * 
 * Provides endpoints to analyze, discover, and retrieve Spring Framework components
 * within a registered repository. This includes stereotypes, configuration classes,
 * beans, scheduling, messaging, caching, transactions, and security annotations.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/spring-components")
@Tag(name = "Spring Components", description = "APIs for discovering and managing Spring Framework components in repositories")
public class SpringControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(SpringControllerV1.class);

    private final SpringComponentService springComponentService;

    public SpringControllerV1(SpringComponentService springComponentService) {
        this.springComponentService = springComponentService;
    }

    /**
     * Analyze Spring components in a repository.
     */
    @PostMapping
    @Operation(
        summary = "Analyze Spring components",
        description = "Discover and classify all Spring Framework components within a repository. " +
                      "Requires that project structure analysis and technology stack detection are already completed."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Spring component analysis completed successfully",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Repository not found"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Prerequisites not met (project structure or technology stack analysis incomplete)"
    )
    public ResponseEntity<ApiResponse<List<SpringComponent>>> analyzeSpringComponents(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        logger.info("Analyzing Spring components for repository: {}", repositoryId);

        try {
            List<SpringComponent> components = springComponentService.analyzeSpringComponents(repositoryId);
            
            logger.info("Analysis complete. Found {} Spring components for repository: {}", 
                    components.size(), repositoryId);

            return ResponseEntity.ok(ApiResponse.success(
                    "Spring component analysis completed successfully",
                    components));
        } catch (IllegalStateException e) {
            ApiResponse<List<SpringComponent>> errorResp = new ApiResponse<>(409, e.getMessage(), null);
            return ResponseEntity.status(409).body(errorResp);
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Analysis failed: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<List<SpringComponent>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get Spring component inventory for a repository.
     */
    @GetMapping
    @Operation(
        summary = "Get Spring components",
        description = "Retrieve all discovered Spring Framework components in a repository."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Spring component inventory retrieved successfully",
        content = @Content(schema = @Schema(implementation = PaginatedResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Repository not found"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringComponent>>> getSpringComponents(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "50") int size) {

        logger.info("Retrieving Spring components for repository: {}", repositoryId);

        try {
            List<SpringComponent> allComponents = springComponentService.getSpringComponents(repositoryId);
            
            // Apply pagination
            long totalElements = allComponents.size();
            long totalPages = (totalElements + size - 1) / size;
            int start = Math.min(page * size, (int) totalElements);
            int end = Math.min((page + 1) * size, (int) totalElements);
            List<SpringComponent> pagedComponents = allComponents.subList(start, end);

            PaginatedResponse<SpringComponent> response = PaginatedResponse.of(
                    pagedComponents, page, size, totalPages, totalElements);

            return ResponseEntity.ok(ApiResponse.success(
                    "Spring component inventory retrieved successfully",
                    response));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve Spring components: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<PaginatedResponse<SpringComponent>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get Spring component statistics for a repository.
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get Spring component statistics",
        description = "Retrieve aggregated statistics about Spring components in a repository, " +
                      "including component type counts and annotation usage."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Statistics retrieved successfully",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Repository not found"
    )
    public ResponseEntity<ApiResponse<SpringComponentStatistics>> getSpringComponentStatistics(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {

        logger.info("Retrieving Spring component statistics for repository: {}", repositoryId);

        try {
            SpringComponentStatistics statistics = springComponentService.getSpringComponentStatistics(repositoryId);

            return ResponseEntity.ok(ApiResponse.success(
                    "Spring component statistics retrieved successfully",
                    statistics));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve statistics: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<SpringComponentStatistics>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get Spring components filtered by stereotype type.
     */
    @GetMapping("/type/{componentType}")
    @Operation(
        summary = "Get Spring components by type",
        description = "Retrieve Spring components filtered by stereotype type (SERVICE, REPOSITORY, CONTROLLER, REST_CONTROLLER, COMPONENT, CONFIGURATION)."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Filtered components retrieved successfully"
    )
    public ResponseEntity<ApiResponse<List<SpringComponent>>> getComponentsByType(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId,
            @Parameter(description = "Component type to filter by", required = true)
            @PathVariable String componentType) {

        logger.info("Retrieving Spring components of type '{}' for repository: {}", componentType, repositoryId);

        try {
            List<SpringComponent> components = springComponentService.getSpringComponents(repositoryId);
            
            // Filter in-memory (can be optimized to use database query if needed)
            List<SpringComponent> filtered = components.stream()
                    .filter(c -> c.getComponentType() != null && 
                               c.getComponentType().equalsIgnoreCase(componentType))
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Found %d %s components", filtered.size(), componentType),
                    filtered));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve components: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<List<SpringComponent>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get Spring components filtered by annotation presence.
     */
    @GetMapping("/annotation/{annotation}")
    @Operation(
        summary = "Get Spring components by annotation",
        description = "Retrieve Spring components that have a specific Spring annotation (e.g., AUTOWIRED, TRANSACTIONAL, SCHEDULED, CACHEABLE)."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Annotated components retrieved successfully"
    )
    public ResponseEntity<ApiResponse<List<SpringComponent>>> getComponentsByAnnotation(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId,
            @Parameter(description = "Annotation name to filter by", required = true)
            @PathVariable String annotation) {

        logger.info("Retrieving Spring components with annotation '{}' for repository: {}", annotation, repositoryId);

        try {
            List<SpringComponent> components = springComponentService.getSpringComponents(repositoryId);
            
            // Filter in-memory based on annotation flags
            List<SpringComponent> filtered = filterByAnnotation(components, annotation);

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Found %d components with annotation '%s'", filtered.size(), annotation),
                    filtered));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve components: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<List<SpringComponent>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    // ==================== Private Methods ====================

    private List<SpringComponent> filterByAnnotation(List<SpringComponent> components, String annotation) {
        return components.stream()
                .filter(c -> {
                    switch (annotation.toUpperCase()) {
                        case "AUTOWIRED": return c.hasAutowired();
                        case "INJECT": return c.hasInject();
                        case "RESOURCE": return c.hasResource();
                        case "TRANSACTIONAL": return c.hasTransactional();
                        case "SCHEDULED": return c.hasScheduled();
                        case "ASYNC": return c.hasAsync();
                        case "CACHEABLE": return c.hasCacheable();
                        case "PRE_AUTHORIZE": return c.hasPreAuthorize();
                        case "POST_AUTHORIZE": return c.hasPostAuthorize();
                        case "ROLES_ALLOWED": return c.hasRolesAllowed();
                        case "SECURED": return c.hasSecured();
                        default: return false;
                    }
                })
                .toList();
    }
}