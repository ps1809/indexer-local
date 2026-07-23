package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.RestApiEndpoint;
import com.projectiq.indexerlocal.model.RestApiStatistics;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.service.RestApiService;
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
 * REST controller for REST API Analysis.
 * 
 * Provides endpoints to discover, analyze, and retrieve REST APIs exposed by a repository.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/rest-apis")
@Tag(name = "REST APIs", description = "APIs for discovering and managing REST API inventory in repositories")
public class RestApiControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(RestApiControllerV1.class);

    private final RestApiService restApiService;

    public RestApiControllerV1(RestApiService restApiService) {
        this.restApiService = restApiService;
    }

    /**
     * Analyze REST APIs in a repository.
     */
    @PostMapping
    @Operation(
        summary = "Analyze REST APIs",
        description = "Discover and analyze all REST APIs exposed by the repository. " +
                      "Identifies REST controllers, endpoints, HTTP methods, request/response types, " +
                      "path variables, request parameters, headers, consumes/produces media types, and security annotations."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "REST API analysis completed successfully",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Repository not found"
    )
    public ResponseEntity<ApiResponse<List<RestApiEndpoint>>> analyzeRestApis(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {
        
        logger.info("Starting REST API analysis for repository: {}", repositoryId);

        try {
            List<RestApiEndpoint> endpoints = restApiService.analyzeRestApis(repositoryId);
            
            logger.info("REST API analysis complete. Found {} endpoints for repository: {}", 
                    endpoints.size(), repositoryId);

            return ResponseEntity.ok(ApiResponse.success(
                    "REST API analysis completed successfully",
                    endpoints));
        } catch (IllegalStateException e) {
            ApiResponse<List<RestApiEndpoint>> errorResp = new ApiResponse<>(409, e.getMessage(), null);
            return ResponseEntity.status(409).body(errorResp);
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Analysis failed: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<List<RestApiEndpoint>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get REST API inventory for a repository.
     */
    @GetMapping
    @Operation(
        summary = "Get REST APIs",
        description = "Retrieve all discovered REST API endpoints in a repository."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "REST API inventory retrieved successfully",
        content = @Content(schema = @Schema(implementation = PaginatedResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Repository not found"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<RestApiEndpoint>>> getRestApis(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "50") int size) {

        logger.info("Retrieving REST API inventory for repository: {}", repositoryId);

        try {
            List<RestApiEndpoint> allEndpoints = restApiService.getRestApis(repositoryId);
            
            // Apply pagination
            long totalElements = allEndpoints.size();
            long totalPages = (totalElements + size - 1) / size;
            int start = Math.min(page * size, (int) totalElements);
            int end = Math.min((page + 1) * size, (int) totalElements);
            List<RestApiEndpoint> pagedEndpoints = allEndpoints.subList(start, end);

            PaginatedResponse<RestApiEndpoint> response = PaginatedResponse.of(
                    pagedEndpoints, page, size, totalPages, totalElements);

            return ResponseEntity.ok(ApiResponse.success(
                    "REST API inventory retrieved successfully",
                    response));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve REST APIs: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<PaginatedResponse<RestApiEndpoint>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get REST API statistics for a repository.
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get REST API statistics",
        description = "Retrieve aggregated statistics about REST APIs in a repository, " +
                      "including controller counts, endpoint counts by HTTP method, and security metrics."
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
    public ResponseEntity<ApiResponse<RestApiStatistics>> getRestApiStatistics(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {

        logger.info("Retrieving REST API statistics for repository: {}", repositoryId);

        try {
            RestApiStatistics statistics = restApiService.getRestApiStatistics(repositoryId);

            return ResponseEntity.ok(ApiResponse.success(
                    "REST API statistics retrieved successfully",
                    statistics));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve statistics: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<RestApiStatistics>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get REST APIs filtered by HTTP method.
     */
    @GetMapping("/method/{httpMethod}")
    @Operation(
        summary = "Get REST APIs by HTTP method",
        description = "Retrieve REST API endpoints filtered by HTTP method (GET, POST, PUT, DELETE, PATCH)."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Filtered APIs retrieved successfully"
    )
    public ResponseEntity<ApiResponse<List<RestApiEndpoint>>> getApisByHttpMethod(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId,
            @Parameter(description = "HTTP method to filter by", required = true)
            @PathVariable String httpMethod) {

        logger.info("Retrieving REST APIs with method '{}' for repository: {}", httpMethod, repositoryId);

        try {
            List<RestApiEndpoint> endpoints = restApiService.getRestApis(repositoryId);
            
            // Filter in-memory
            List<RestApiEndpoint> filtered = endpoints.stream()
                    .filter(e -> e.getHttpMethod() != null && 
                                e.getHttpMethod().equalsIgnoreCase(httpMethod))
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Found %d %s endpoints", filtered.size(), httpMethod.toUpperCase()),
                    filtered));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve APIs: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<List<RestApiEndpoint>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get REST APIs filtered by controller name.
     */
    @GetMapping("/controller/{controllerName}")
    @Operation(
        summary = "Get REST APIs by controller",
        description = "Retrieve REST API endpoints filtered by controller class name."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Filtered APIs retrieved successfully"
    )
    public ResponseEntity<ApiResponse<List<RestApiEndpoint>>> getApisByController(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId,
            @Parameter(description = "Controller name to filter by", required = true)
            @PathVariable String controllerName) {

        logger.info("Retrieving REST APIs from controller '{}' for repository: {}", controllerName, repositoryId);

        try {
            List<RestApiEndpoint> endpoints = restApiService.getRestApis(repositoryId);
            
            // Filter in-memory
            List<RestApiEndpoint> filtered = endpoints.stream()
                    .filter(e -> e.getControllerName() != null && 
                                e.getControllerName().equalsIgnoreCase(controllerName))
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Found %d endpoints from controller '%s'", filtered.size(), controllerName),
                    filtered));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve APIs: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<List<RestApiEndpoint>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get secure REST APIs.
     */
    @GetMapping("/secure")
    @Operation(
        summary = "Get secure REST APIs",
        description = "Retrieve REST API endpoints that have security annotations (@PreAuthorize, @PostAuthorize, @RolesAllowed, @Secured)."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Secure APIs retrieved successfully"
    )
    public ResponseEntity<ApiResponse<List<RestApiEndpoint>>> getSecureApis(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {

        logger.info("Retrieving secure REST APIs for repository: {}", repositoryId);

        try {
            List<RestApiEndpoint> endpoints = restApiService.getRestApis(repositoryId);
            
            // Filter in-memory
            List<RestApiEndpoint> filtered = endpoints.stream()
                    .filter(RestApiEndpoint::isSecure)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Found %d secure endpoints", filtered.size()),
                    filtered));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve secure APIs: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<List<RestApiEndpoint>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }

    /**
     * Get public REST APIs.
     */
    @GetMapping("/public")
    @Operation(
        summary = "Get public REST APIs",
        description = "Retrieve REST API endpoints that do not have security annotations."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Public APIs retrieved successfully"
    )
    public ResponseEntity<ApiResponse<List<RestApiEndpoint>>> getPublicApis(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable String repositoryId) {

        logger.info("Retrieving public REST APIs for repository: {}", repositoryId);

        try {
            List<RestApiEndpoint> endpoints = restApiService.getRestApis(repositoryId);
            
            // Filter in-memory
            List<RestApiEndpoint> filtered = endpoints.stream()
                    .filter(e -> !e.isSecure())
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Found %d public endpoints", filtered.size()),
                    filtered));
        } catch (Exception e) {
            ApiResponse<Object> errResp = ApiResponse.internalError("Failed to retrieve public APIs: " + e.getMessage());
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<List<RestApiEndpoint>>> response = 
                (ResponseEntity) ResponseEntity.internalServerError().body(errResp);
            return response;
        }
    }
}