package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipEntry;
import com.projectiq.indexerlocal.service.RelationshipSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the Relationship Search Engine.
 * Provides endpoints for deterministic traversal of indexed repository relationships.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Relationship Search", description = "Relationship Search Engine for traversing indexed repository relationships")
public class RelationshipSearchControllerV1 {

    private final RelationshipSearchService relationshipSearchService;

    public RelationshipSearchControllerV1(RelationshipSearchService relationshipSearchService) {
        this.relationshipSearchService = relationshipSearchService;
    }

    @GetMapping("/implementations")
    @Operation(summary = "Find implementations", description = "Find all classes that implement the given interface")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findImplementations(
            @RequestParam(required = false) String repositoryId,
            @RequestParam String interfaceName,
            @RequestParam(defaultValue = "false") boolean recursive,
            @RequestParam(defaultValue = "10") int maxDepth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findImplementations(
                repositoryId, interfaceName, recursive, maxDepth, page, size);
        long total = relationshipSearchService.countImplementations(
                repositoryId, interfaceName, recursive, maxDepth);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }

    @GetMapping("/inheritors")
    @Operation(summary = "Find inheritors", description = "Find all classes that extend (inherit from) the given class")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findInheritors(
            @RequestParam(required = false) String repositoryId,
            @RequestParam String className,
            @RequestParam(defaultValue = "false") boolean recursive,
            @RequestParam(defaultValue = "10") int maxDepth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findInheritors(
                repositoryId, className, recursive, maxDepth, page, size);
        long total = relationshipSearchService.countInheritors(
                repositoryId, className, recursive, maxDepth);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }

    @GetMapping("/references")
    @Operation(summary = "Find references", description = "Find all references to the given symbol across the indexed codebase")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findReferences(
            @RequestParam(required = false) String repositoryId,
            @RequestParam String symbolName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findReferences(
                repositoryId, symbolName, page, size);
        long total = relationshipSearchService.countReferences(repositoryId, symbolName);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }

    @GetMapping("/usages")
    @Operation(summary = "Find usages", description = "Find all usages of the given symbol (references + inheritance + implementations)")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findUsages(
            @RequestParam(required = false) String repositoryId,
            @RequestParam String symbolName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findUsages(
                repositoryId, symbolName, page, size);
        long total = relationshipSearchService.countUsages(repositoryId, symbolName);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }

    @GetMapping("/dependencies")
    @Operation(summary = "Find dependencies", description = "Find all dependencies of the given symbol (what it depends on)")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findDependencies(
            @RequestParam(required = false) String repositoryId,
            @RequestParam String symbolName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findDependencies(
                repositoryId, symbolName, page, size);
        long total = relationshipSearchService.countDependencies(repositoryId, symbolName);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }

    @GetMapping("/dependents")
    @Operation(summary = "Find dependents", description = "Find all dependents of the given symbol (what depends on it)")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findDependents(
            @RequestParam(required = false) String repositoryId,
            @RequestParam String symbolName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findDependents(
                repositoryId, symbolName, page, size);
        long total = relationshipSearchService.countDependents(repositoryId, symbolName);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }

    @GetMapping("/callers")
    @Operation(summary = "Find callers", description = "Find all callers of the given method")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findCallers(
            @RequestParam(required = false) String repositoryId,
            @RequestParam String methodName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findCallers(
                repositoryId, methodName, page, size);
        long total = relationshipSearchService.countCallers(repositoryId, methodName);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }

    @GetMapping("/callees")
    @Operation(summary = "Find callees", description = "Find all callees (methods called by) the given method")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findCallees(
            @RequestParam(required = false) String repositoryId,
            @RequestParam String methodName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findCallees(
                repositoryId, methodName, page, size);
        long total = relationshipSearchService.countCallees(repositoryId, methodName);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }

    @GetMapping("/package-relationships")
    @Operation(summary = "Find package relationships", description = "Find all relationships between packages in the repository")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findPackageRelationships(
            @RequestParam(required = false) String repositoryId,
            @RequestParam(required = false) String packageName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findPackageRelationships(
                repositoryId, packageName, page, size);
        long total = relationshipSearchService.countPackageRelationships(repositoryId, packageName);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }

    @GetMapping("/module-relationships")
    @Operation(summary = "Find module relationships", description = "Find all relationships between modules in the repository")
    public ResponseEntity<PaginatedResponse<RelationshipEntry>> findModuleRelationships(
            @RequestParam(required = false) String repositoryId,
            @RequestParam(required = false) String moduleName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RelationshipEntry> content = relationshipSearchService.findModuleRelationships(
                repositoryId, moduleName, page, size);
        long total = relationshipSearchService.countModuleRelationships(repositoryId, moduleName);
        long totalPages = size > 0 ? (long) Math.ceil((double) total / size) : 0;

        return ResponseEntity.ok(new PaginatedResponse<>(content, page, size, totalPages, total));
    }
}