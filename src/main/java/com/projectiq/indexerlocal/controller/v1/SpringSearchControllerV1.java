package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.springsearch.SpringSearchResult;
import com.projectiq.indexerlocal.service.SpringSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Spring Search Engine.
 * Provides fast, deterministic search over indexed Spring Framework artifacts
 * without filesystem scanning.
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Spring Search API", description = "Fast, deterministic Spring artifact search engine")
public class SpringSearchControllerV1 {

    private static final Logger log = LoggerFactory.getLogger(SpringSearchControllerV1.class);

    private final SpringSearchService springSearchService;

    public SpringSearchControllerV1(SpringSearchService springSearchService) {
        this.springSearchService = springSearchService;
    }

    // ==================== Controller Search ====================

    @GetMapping("/spring/controller")
    @Operation(summary = "Find Spring controllers", description = "Search indexed Spring @Controller and @RestController components")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringSearchResult>>> findControllers(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Spring search: type=CONTROLLER, repositoryId={}, package={}, module={}, page={}", repositoryId, packageName, module, page);
        List<SpringSearchResult> results = springSearchService.findControllers(repositoryId, packageName, module, page, size);
        long total = springSearchService.countControllers(repositoryId, packageName, module);
        long totalPages = (long) Math.ceil((double) total / size);
        return ResponseEntity.ok(ApiResponse.success("Controllers retrieved",
                PaginatedResponse.of(results, page, size, totalPages, total)));
    }

    // ==================== Service Search ====================

    @GetMapping("/spring/service")
    @Operation(summary = "Find Spring services", description = "Search indexed @Service components")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringSearchResult>>> findServices(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Spring search: type=SERVICE, repositoryId={}, package={}, module={}, page={}", repositoryId, packageName, module, page);
        List<SpringSearchResult> results = springSearchService.findServices(repositoryId, packageName, module, page, size);
        long total = springSearchService.countServices(repositoryId, packageName, module);
        long totalPages = (long) Math.ceil((double) total / size);
        return ResponseEntity.ok(ApiResponse.success("Services retrieved",
                PaginatedResponse.of(results, page, size, totalPages, total)));
    }

    // ==================== Repository Search ====================

    @GetMapping("/spring/repository")
    @Operation(summary = "Find Spring repositories", description = "Search indexed @Repository components")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringSearchResult>>> findRepositories(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Spring search: type=REPOSITORY, repositoryId={}, package={}, module={}, page={}", repositoryId, packageName, module, page);
        List<SpringSearchResult> results = springSearchService.findRepositories(repositoryId, packageName, module, page, size);
        long total = springSearchService.countRepositories(repositoryId, packageName, module);
        long totalPages = (long) Math.ceil((double) total / size);
        return ResponseEntity.ok(ApiResponse.success("Repositories retrieved",
                PaginatedResponse.of(results, page, size, totalPages, total)));
    }

    // ==================== Component Search ====================

    @GetMapping("/spring/component")
    @Operation(summary = "Find Spring components", description = "Search indexed @Component components")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringSearchResult>>> findComponents(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Spring search: type=COMPONENT, repositoryId={}, package={}, module={}, page={}", repositoryId, packageName, module, page);
        List<SpringSearchResult> results = springSearchService.findComponents(repositoryId, packageName, module, page, size);
        long total = springSearchService.countComponents(repositoryId, packageName, module);
        long totalPages = (long) Math.ceil((double) total / size);
        return ResponseEntity.ok(ApiResponse.success("Components retrieved",
                PaginatedResponse.of(results, page, size, totalPages, total)));
    }

    // ==================== Configuration Class Search ====================

    @GetMapping("/spring/configuration")
    @Operation(summary = "Find Spring configuration classes", description = "Search indexed @Configuration classes")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringSearchResult>>> findConfigurationClasses(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Spring search: type=CONFIGURATION, repositoryId={}, package={}, module={}, page={}", repositoryId, packageName, module, page);
        List<SpringSearchResult> results = springSearchService.findConfigurationClasses(repositoryId, packageName, module, page, size);
        long total = springSearchService.countConfigurationClasses(repositoryId, packageName, module);
        long totalPages = (long) Math.ceil((double) total / size);
        return ResponseEntity.ok(ApiResponse.success("Configuration classes retrieved",
                PaginatedResponse.of(results, page, size, totalPages, total)));
    }

    // ==================== Endpoint Search ====================

    @GetMapping("/spring/endpoint")
    @Operation(summary = "Find REST endpoints", description = "Search indexed REST API endpoints with filters for HTTP method, path, and controller")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringSearchResult>>> findEndpoints(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "HTTP method filter (GET, POST, PUT, DELETE, PATCH)") @RequestParam(required = false) String httpMethod,
            @Parameter(description = "Path filter") @RequestParam(required = false) String path,
            @Parameter(description = "Controller name filter") @RequestParam(required = false) String controllerName,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Spring search: type=ENDPOINT, repositoryId={}, method={}, path={}, page={}", repositoryId, httpMethod, path, page);
        List<SpringSearchResult> results = springSearchService.findEndpoints(repositoryId, httpMethod, path, controllerName, packageName, module, page, size);
        long total = springSearchService.countEndpoints(repositoryId, httpMethod, path, controllerName, packageName, module);
        long totalPages = (long) Math.ceil((double) total / size);
        return ResponseEntity.ok(ApiResponse.success("Endpoints retrieved",
                PaginatedResponse.of(results, page, size, totalPages, total)));
    }

    // ==================== Bean Search ====================

    @GetMapping("/spring/bean")
    @Operation(summary = "Find Spring beans", description = "Search indexed @Bean definitions")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringSearchResult>>> findBeans(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Bean name filter") @RequestParam(required = false) String beanName,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Spring search: type=BEAN, repositoryId={}, beanName={}, page={}", repositoryId, beanName, page);
        List<SpringSearchResult> results = springSearchService.findBeans(repositoryId, packageName, beanName, module, page, size);
        long total = springSearchService.countBeans(repositoryId, packageName, beanName, module);
        long totalPages = (long) Math.ceil((double) total / size);
        return ResponseEntity.ok(ApiResponse.success("Beans retrieved",
                PaginatedResponse.of(results, page, size, totalPages, total)));
    }

    // ==================== Scheduled Task Search ====================

    @GetMapping("/spring/scheduled")
    @Operation(summary = "Find scheduled tasks", description = "Search indexed @Scheduled methods")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringSearchResult>>> findScheduledTasks(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Spring search: type=SCHEDULED, repositoryId={}, page={}", repositoryId, page);
        List<SpringSearchResult> results = springSearchService.findScheduledTasks(repositoryId, packageName, module, page, size);
        long total = springSearchService.countScheduledTasks(repositoryId, packageName, module);
        long totalPages = (long) Math.ceil((double) total / size);
        return ResponseEntity.ok(ApiResponse.success("Scheduled tasks retrieved",
                PaginatedResponse.of(results, page, size, totalPages, total)));
    }

    // ==================== Event Listener Search ====================

    @GetMapping("/spring/event-listener")
    @Operation(summary = "Find event listeners", description = "Search indexed @EventListener components")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringSearchResult>>> findEventListeners(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Spring search: type=EVENT_LISTENER, repositoryId={}, page={}", repositoryId, page);
        List<SpringSearchResult> results = springSearchService.findEventListeners(repositoryId, packageName, module, page, size);
        long total = springSearchService.countEventListeners(repositoryId, packageName, module);
        long totalPages = (long) Math.ceil((double) total / size);
        return ResponseEntity.ok(ApiResponse.success("Event listeners retrieved",
                PaginatedResponse.of(results, page, size, totalPages, total)));
    }
}