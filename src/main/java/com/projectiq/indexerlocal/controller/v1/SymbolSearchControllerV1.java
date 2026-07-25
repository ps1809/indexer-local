package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.symbol.SymbolEntry;
import com.projectiq.indexerlocal.model.symbol.SymbolSearchResult;
import com.projectiq.indexerlocal.service.SymbolSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Symbol Search Engine.
 * Provides deterministic, fast symbol lookup endpoints without filesystem scanning.
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Symbol Search API", description = "Fast, deterministic symbol search engine for indexed Java symbols")
public class SymbolSearchControllerV1 {

    private static final Logger log = LoggerFactory.getLogger(SymbolSearchControllerV1.class);

    private final SymbolSearchService symbolSearchService;

    public SymbolSearchControllerV1(SymbolSearchService symbolSearchService) {
        this.symbolSearchService = symbolSearchService;
    }

    // ==================== Class Search ====================

    @GetMapping("/class")
    @Operation(summary = "Find classes", description = "Search indexed Java classes by name with pagination and filters")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> findClass(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Visibility filter: PUBLIC, PRIVATE, PROTECTED") @RequestParam(required = false) String visibility,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type=CLASS, query={}, mode={}, page={}", q, mode, page);
        SymbolSearchResult result = symbolSearchService.findClass(repositoryId, q, mode, packageName, visibility, page, size);
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }

    // ==================== Interface Search ====================

    @GetMapping("/interface")
    @Operation(summary = "Find interfaces", description = "Search indexed Java interfaces by name")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> findInterface(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Visibility filter") @RequestParam(required = false) String visibility,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type=INTERFACE, query={}, mode={}, page={}", q, mode, page);
        SymbolSearchResult result = symbolSearchService.findInterface(repositoryId, q, mode, packageName, visibility, page, size);
        return ResponseEntity.ok(ApiResponse.success("Interfaces retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }

    // ==================== Enum Search ====================

    @GetMapping("/enum")
    @Operation(summary = "Find enums", description = "Search indexed Java enums by name")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> findEnum(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Visibility filter") @RequestParam(required = false) String visibility,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type=ENUM, query={}, mode={}, page={}", q, mode, page);
        SymbolSearchResult result = symbolSearchService.findEnum(repositoryId, q, mode, packageName, visibility, page, size);
        return ResponseEntity.ok(ApiResponse.success("Enums retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }

    // ==================== Record Search ====================

    @GetMapping("/record")
    @Operation(summary = "Find records", description = "Search indexed Java records by name")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> findRecord(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Visibility filter") @RequestParam(required = false) String visibility,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type=RECORD, query={}, mode={}, page={}", q, mode, page);
        SymbolSearchResult result = symbolSearchService.findRecord(repositoryId, q, mode, packageName, visibility, page, size);
        return ResponseEntity.ok(ApiResponse.success("Records retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }

    // ==================== Annotation Search ====================

    @GetMapping("/annotation")
    @Operation(summary = "Find annotations", description = "Search indexed Java annotations by name")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> findAnnotation(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type=ANNOTATION, query={}, mode={}, page={}", q, mode, page);
        SymbolSearchResult result = symbolSearchService.findAnnotation(repositoryId, q, mode, null, page, size);
        return ResponseEntity.ok(ApiResponse.success("Annotations retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }

    // ==================== Method Search ====================

    @GetMapping("/method")
    @Operation(summary = "Find methods", description = "Search indexed Java methods by name")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> findMethod(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Visibility filter") @RequestParam(required = false) String visibility,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type=METHOD, query={}, mode={}, page={}", q, mode, page);
        SymbolSearchResult result = symbolSearchService.findMethod(repositoryId, q, mode, packageName, visibility, page, size);
        return ResponseEntity.ok(ApiResponse.success("Methods retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }

    // ==================== Constructor Search ====================

    @GetMapping("/constructor")
    @Operation(summary = "Find constructors", description = "Search indexed Java constructors by name")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> findConstructor(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Visibility filter") @RequestParam(required = false) String visibility,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type=CONSTRUCTOR, query={}, mode={}, page={}", q, mode, page);
        SymbolSearchResult result = symbolSearchService.findConstructor(repositoryId, q, mode, packageName, visibility, page, size);
        return ResponseEntity.ok(ApiResponse.success("Constructors retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }

    // ==================== Field Search ====================

    @GetMapping("/field")
    @Operation(summary = "Find fields", description = "Search indexed Java fields by name")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> findField(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Visibility filter") @RequestParam(required = false) String visibility,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type=FIELD, query={}, mode={}, page={}", q, mode, page);
        SymbolSearchResult result = symbolSearchService.findField(repositoryId, q, mode, packageName, visibility, page, size);
        return ResponseEntity.ok(ApiResponse.success("Fields retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }

    // ==================== Package Search ====================

    @GetMapping("/package")
    @Operation(summary = "Find packages", description = "Search indexed Java packages by name")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> findPackage(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type=PACKAGE, query={}, mode={}, page={}", q, mode, page);
        SymbolSearchResult result = symbolSearchService.findPackage(repositoryId, q, mode, page, size);
        return ResponseEntity.ok(ApiResponse.success("Packages retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }

    // ==================== General Symbol Search ====================

    @GetMapping("/symbol")
    @Operation(summary = "Search all symbols", description = "General symbol search across all indexed symbol types")
    public ResponseEntity<ApiResponse<PaginatedResponse<SymbolEntry>>> searchSymbols(
            @Parameter(description = "Repository ID filter") @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Search mode: EXACT, PREFIX, PARTIAL, FQN") @RequestParam(required = false, defaultValue = "PARTIAL") String mode,
            @Parameter(description = "Symbol type filter: CLASS, INTERFACE, ENUM, RECORD, ANNOTATION, METHOD, CONSTRUCTOR, FIELD, PACKAGE") @RequestParam(required = false) String symbolType,
            @Parameter(description = "Package filter") @RequestParam(required = false) String packageName,
            @Parameter(description = "Visibility filter") @RequestParam(required = false) String visibility,
            @Parameter(description = "Module filter") @RequestParam(required = false) String module,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Symbol search: type={}, query={}, mode={}, page={}", symbolType, q, mode, page);
        SymbolSearchResult result = symbolSearchService.searchSymbols(repositoryId, q, mode, symbolType, packageName, visibility, module, page, size);
        return ResponseEntity.ok(ApiResponse.success("Symbols retrieved",
                PaginatedResponse.of(result.getContent(), page, size, result.getTotalPages(), result.getTotalElements())));
    }
}