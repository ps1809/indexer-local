package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * v1 REST controller for repository-scoped search operations.
 * All search endpoints are scoped to a specific repository and query only persisted indexed metadata.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/search")
@Tag(name = "Search API (v1)", description = "Repository-scoped search API for querying indexed metadata")
public class SearchControllerV1 {

    private static final Logger log = LoggerFactory.getLogger(SearchControllerV1.class);

    private final SearchService searchService;

    public SearchControllerV1(SearchService searchService) {
        this.searchService = searchService;
    }

    // ==================== General Repository Search ====================

    /**
     * General repository search - searches across all indexed entity types.
     * GET /api/v1/repositories/{repositoryId}/search
     */
    @GetMapping
    @Operation(summary = "General repository search", description = "Searches across all indexed entities (classes, methods, fields, annotations, components, endpoints) for a given repository using the provided query text")
    public ResponseEntity<ApiResponse<PaginatedResponse<Map<String, Object>>>> search(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId,
            @Parameter(description = "Search query text") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Entity type filter (CLASS, INTERFACE, ENUM, RECORD, METHOD, FIELD, ANNOTATION, COMPONENT, ENDPOINT, DEPENDENCY, CONFIGURATION, DATABASE_ARTIFACT)") @RequestParam(required = false) String type,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Annotation filter") @RequestParam(required = false) String annotation,
            @Parameter(description = "HTTP method filter (GET, POST, PUT, DELETE, PATCH)") @RequestParam(required = false) String httpMethod,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field (name, package, type)") @RequestParam(value = "sortBy", required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(value = "sortDir", required = false, defaultValue = "ASC") String sortDir) {
        long startTime = System.currentTimeMillis();
        log.info("Search request: repository={}, query={}, type={}, page={}", repositoryId, q, type, page);

        List<Map<String, Object>> results;
        long totalElements;

        if (type != null && !type.isEmpty()) {
            switch (type.toUpperCase()) {
                case "CLASS":
                case "INTERFACE":
                case "ENUM":
                case "RECORD":
                    var classResults = searchService.searchClasses(repositoryId, q, packageName, page, size, sortBy, sortDir);
                    results = classResults.getContent();
                    totalElements = classResults.getTotalElements();
                    break;
                case "METHOD":
                    var methodResults = searchService.searchMethods(repositoryId, q, packageName, page, size, sortBy, sortDir);
                    results = methodResults.getContent();
                    totalElements = methodResults.getTotalElements();
                    break;
                case "FIELD":
                    var fieldResults = searchService.searchFields(repositoryId, q, packageName, page, size, sortBy, sortDir);
                    results = fieldResults.getContent();
                    totalElements = fieldResults.getTotalElements();
                    break;
                case "ANNOTATION":
                    var annotationResults = searchService.searchAnnotations(repositoryId, q, page, size, sortBy, sortDir);
                    results = annotationResults.getContent();
                    totalElements = annotationResults.getTotalElements();
                    break;
                case "COMPONENT":
                    var componentResults = searchService.searchComponents(repositoryId, q, packageName, annotation, page, size, sortBy, sortDir);
                    results = componentResults.getContent();
                    totalElements = componentResults.getTotalElements();
                    break;
                case "ENDPOINT":
                    var endpointResults = searchService.searchRestEndpoints(repositoryId, q, httpMethod, page, size, sortBy, sortDir);
                    results = endpointResults.getContent();
                    totalElements = endpointResults.getTotalElements();
                    break;
                default:
                    var generalResults = searchService.generalSearch(repositoryId, q, packageName, annotation);
                    int fromIndex = page * size;
                    int toIndex = Math.min(fromIndex + size, generalResults.size());
                    List<Map<String, Object>> pagedContent = generalResults.subList(fromIndex, toIndex);
                    return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully",
                            PaginatedResponse.of(pagedContent, page, size, (int) Math.ceil((double) generalResults.size() / size), generalResults.size())));
            }
        } else {
            var generalResults = searchService.generalSearch(repositoryId, q, packageName, annotation);
            int totalPages = (int) Math.ceil((double) generalResults.size() / size);
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, generalResults.size());
            List<Map<String, Object>> pagedContent = generalResults.subList(fromIndex, toIndex);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Search completed: repository={}, results={}, duration={}ms", repositoryId, generalResults.size(), duration);
            return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully",
                    PaginatedResponse.of(pagedContent, page, size, totalPages, generalResults.size())));
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Search completed: repository={}, query={}, type={}, results={}, duration={}ms", repositoryId, q, type, totalElements, duration);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully",
                PaginatedResponse.of(results, page, size, totalPages, totalElements)));
    }

    // ==================== Class Search ====================

    /**
     * Search classes, interfaces, enums, and records.
     * GET /api/v1/repositories/{repositoryId}/search/classes
     */
    @GetMapping("/classes")
    @Operation(summary = "Search classes", description = "Searches indexed Java classes, interfaces, enums, and records for a repository")
    public ResponseEntity<ApiResponse<PaginatedResponse<Map<String, Object>>>> searchClasses(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId,
            @Parameter(description = "Search query text") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Entity type filter (CLASS, INTERFACE, ENUM, RECORD)") @RequestParam(required = false) String entityType,
            @Parameter(description = "Visibility filter (PUBLIC, PRIVATE, PROTECTED, PACKAGE_PRIVATE)") @RequestParam(required = false) String visibility,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field (name, package, type)") @RequestParam(value = "sortBy", required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(value = "sortDir", required = false, defaultValue = "ASC") String sortDir) {
        long startTime = System.currentTimeMillis();
        log.info("Class search: repository={}, query={}, entityType={}", repositoryId, q, entityType);

        List<Map<String, Object>> results;
        long totalElements;

        if (entityType != null && !entityType.isEmpty()) {
            var paginatedResults = searchService.searchClassesByType(repositoryId, q, entityType, visibility, packageName, page, size, sortBy, sortDir);
            results = paginatedResults.getContent();
            totalElements = paginatedResults.getTotalElements();
        } else {
            var paginatedResults = searchService.searchClasses(repositoryId, q, packageName, page, size, sortBy, sortDir);
            results = paginatedResults.getContent();
            totalElements = paginatedResults.getTotalElements();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Class search completed: repository={}, results={}, duration={}ms", repositoryId, totalElements, duration);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved successfully",
                PaginatedResponse.of(results, page, size, totalPages, totalElements)));
    }

    // ==================== Method Search ====================

    /**
     * Search methods.
     * GET /api/v1/repositories/{repositoryId}/search/methods
     */
    @GetMapping("/methods")
    @Operation(summary = "Search methods", description = "Searches indexed Java methods for a repository")
    public ResponseEntity<ApiResponse<PaginatedResponse<Map<String, Object>>>> searchMethods(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId,
            @Parameter(description = "Search query text") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Visibility filter (PUBLIC, PRIVATE, PROTECTED, PACKAGE_PRIVATE)") @RequestParam(required = false) String visibility,
            @Parameter(description = "Static filter") @RequestParam(required = false) Boolean isStatic,
            @Parameter(description = "Abstract filter") @RequestParam(required = false) Boolean isAbstract,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field (name, package, type)") @RequestParam(value = "sortBy", required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(value = "sortDir", required = false, defaultValue = "ASC") String sortDir) {
        long startTime = System.currentTimeMillis();
        log.info("Method search: repository={}, query={}", repositoryId, q);

        var paginatedResults = searchService.searchMethods(repositoryId, q, packageName, page, size, sortBy, sortDir);
        List<Map<String, Object>> results = paginatedResults.getContent();
        long totalElements = paginatedResults.getTotalElements();
        long duration = System.currentTimeMillis() - startTime;
        log.info("Method search completed: repository={}, results={}, duration={}ms", repositoryId, totalElements, duration);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return ResponseEntity.ok(ApiResponse.success("Methods retrieved successfully",
                PaginatedResponse.of(results, page, size, totalPages, totalElements)));
    }

    // ==================== Field Search ====================

    /**
     * Search fields.
     * GET /api/v1/repositories/{repositoryId}/search/fields
     */
    @GetMapping("/fields")
    @Operation(summary = "Search fields", description = "Searches indexed Java fields for a repository")
    public ResponseEntity<ApiResponse<PaginatedResponse<Map<String, Object>>>> searchFields(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId,
            @Parameter(description = "Search query text") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Visibility filter (PUBLIC, PRIVATE, PROTECTED, PACKAGE_PRIVATE)") @RequestParam(required = false) String visibility,
            @Parameter(description = "Static filter") @RequestParam(required = false) Boolean isStatic,
            @Parameter(description = "Final filter") @RequestParam(required = false) Boolean isFinal,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field (name, package, type)") @RequestParam(value = "sortBy", required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(value = "sortDir", required = false, defaultValue = "ASC") String sortDir) {
        long startTime = System.currentTimeMillis();
        log.info("Field search: repository={}, query={}", repositoryId, q);

        var paginatedResults = searchService.searchFields(repositoryId, q, packageName, page, size, sortBy, sortDir);
        List<Map<String, Object>> results = paginatedResults.getContent();
        long totalElements = paginatedResults.getTotalElements();
        long duration = System.currentTimeMillis() - startTime;
        log.info("Field search completed: repository={}, results={}, duration={}ms", repositoryId, totalElements, duration);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return ResponseEntity.ok(ApiResponse.success("Fields retrieved successfully",
                PaginatedResponse.of(results, page, size, totalPages, totalElements)));
    }

    // ==================== REST API Search ====================

    /**
     * Search REST endpoints.
     * GET /api/v1/repositories/{repositoryId}/search/rest-apis
     */
    @GetMapping("/rest-apis")
    @Operation(summary = "Search REST APIs", description = "Searches indexed REST API endpoints for a repository")
    public ResponseEntity<ApiResponse<PaginatedResponse<Map<String, Object>>>> searchRestEndpoints(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId,
            @Parameter(description = "Search query text") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "HTTP method filter (GET, POST, PUT, DELETE, PATCH)") @RequestParam(required = false) String httpMethod,
            @Parameter(description = "Path pattern filter") @RequestParam(required = false) String pathPattern,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field (name, package, type)") @RequestParam(value = "sortBy", required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(value = "sortDir", required = false, defaultValue = "ASC") String sortDir) {
        long startTime = System.currentTimeMillis();
        log.info("REST API search: repository={}, query={}, httpMethod={}", repositoryId, q, httpMethod);

        var paginatedResults = searchService.searchRestEndpoints(repositoryId, q, httpMethod, page, size, sortBy, sortDir);
        List<Map<String, Object>> results = paginatedResults.getContent();
        long totalElements = paginatedResults.getTotalElements();
        long duration = System.currentTimeMillis() - startTime;
        log.info("REST API search completed: repository={}, results={}, duration={}ms", repositoryId, totalElements, duration);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return ResponseEntity.ok(ApiResponse.success("REST endpoints retrieved successfully",
                PaginatedResponse.of(results, page, size, totalPages, totalElements)));
    }

    // ==================== Spring Component Search ====================

    /**
     * Search Spring components.
     * GET /api/v1/repositories/{repositoryId}/search/components
     */
    @GetMapping("/components")
    @Operation(summary = "Search Spring components", description = "Searches indexed Spring components (services, repositories, controllers, configurations, beans) for a repository")
    public ResponseEntity<ApiResponse<PaginatedResponse<Map<String, Object>>>> searchComponents(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId,
            @Parameter(description = "Search query text") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Component type filter (SERVICE, REPOSITORY, CONTROLLER, REST_CONTROLLER, CONFIGURATION, COMPONENT, BEAN)") @RequestParam(required = false) String componentType,
            @Parameter(description = "Annotation filter") @RequestParam(required = false) String annotation,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field (name, package, type)") @RequestParam(value = "sortBy", required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(value = "sortDir", required = false, defaultValue = "ASC") String sortDir) {
        long startTime = System.currentTimeMillis();
        log.info("Component search: repository={}, query={}, componentType={}", repositoryId, q, componentType);

        var paginatedResults = searchService.searchComponents(repositoryId, q, packageName, annotation, page, size, sortBy, sortDir);
        List<Map<String, Object>> results = paginatedResults.getContent();
        long totalElements = paginatedResults.getTotalElements();
        long duration = System.currentTimeMillis() - startTime;
        log.info("Component search completed: repository={}, results={}, duration={}ms", repositoryId, totalElements, duration);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return ResponseEntity.ok(ApiResponse.success("Spring components retrieved successfully",
                PaginatedResponse.of(results, page, size, totalPages, totalElements)));
    }

    // ==================== Dependency Search ====================

    /**
     * Search dependencies.
     * GET /api/v1/repositories/{repositoryId}/search/dependencies
     */
    @GetMapping("/dependencies")
    @Operation(summary = "Search dependencies", description = "Searches indexed project dependencies for a repository")
    public ResponseEntity<ApiResponse<PaginatedResponse<Map<String, Object>>>> searchDependencies(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId,
            @Parameter(description = "Search query text") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Dependency type filter (LIBRARY, FRAMEWORK, TOOL, PLUGIN)") @RequestParam(required = false) String dependencyType,
            @Parameter(description = "Scope filter (COMPILE, RUNTIME, TEST, PROVIDED)") @RequestParam(required = false) String scope,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field (name, type)") @RequestParam(value = "sortBy", required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(value = "sortDir", required = false, defaultValue = "ASC") String sortDir) {
        long startTime = System.currentTimeMillis();
        log.info("Dependency search: repository={}, query={}", repositoryId, q);

        var paginatedResults = searchService.searchDependencies(repositoryId, q, dependencyType, scope, page, size, sortBy, sortDir);
        List<Map<String, Object>> results = paginatedResults.getContent();
        long totalElements = paginatedResults.getTotalElements();
        long duration = System.currentTimeMillis() - startTime;
        log.info("Dependency search completed: repository={}, results={}, duration={}ms", repositoryId, totalElements, duration);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return ResponseEntity.ok(ApiResponse.success("Dependencies retrieved successfully",
                PaginatedResponse.of(results, page, size, totalPages, totalElements)));
    }

    // ==================== Search Statistics ====================

    /**
     * Get search statistics for a repository.
     * GET /api/v1/repositories/{repositoryId}/search/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "Get search statistics", description = "Returns count statistics for each indexed entity type in the repository")
    public ResponseEntity<Object> getSearchStatistics(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId) {
        Map<String, Long> stats = searchService.getSearchStatistics(repositoryId);
        return ResponseEntity.ok(ApiResponse.success("Search statistics retrieved successfully", stats));
    }
}