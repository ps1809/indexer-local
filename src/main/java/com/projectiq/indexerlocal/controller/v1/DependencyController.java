package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.Dependency;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.service.DependencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for dependency analysis operations.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/dependencies")
@Tag(name = "Dependency Analyzer", description = "APIs for analyzing and retrieving project dependencies")
public class DependencyController {

    private final DependencyService dependencyService;

    public DependencyController(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    /**
     * Analyze project dependencies.
     */
    @PostMapping
    @Operation(summary = "Analyze project dependencies", description = "Analyze the build files of a repository and extract all dependency information including group ID, artifact ID, version, scope, type, and classifier.")
    public ResponseEntity<ApiResponse<?>> analyzeDependencies(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        try {
            List<Dependency> dependencies = dependencyService.analyzeDependencies(repositoryId);

            // Convert to map for JSON response
            List<Map<String, Object>> result = dependencies.stream().map(dep -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("groupId", dep.getGroupId());
                map.put("artifactId", dep.getArtifactId());
                map.put("version", dep.getVersion());
                map.put("type", dep.getType() != null ? dep.getType().name() : null);
                map.put("configuration", dep.getConfiguration());
                map.put("optional", dep.isOptional());
                map.put("classifier", dep.getClassifier());
                map.put("typeClassifier", dep.getTypeClassifier());
                map.put("internal", dep.isInternal());
                map.put("repositoryId", dep.getRepositoryId());
                return map;
            }).toList();

            return ResponseEntity.ok(ApiResponse.success("Dependency analysis completed successfully", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("build-analysis", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.internalError("Internal error during dependency analysis: " + e.getMessage()));
        }
    }

    /**
     * Get dependency inventory.
     */
    @GetMapping
    @Operation(summary = "Get dependency inventory", description = "Retrieve all dependencies for a repository.")
    public ResponseEntity<ApiResponse<?>> getDependencies(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        try {
            List<Dependency> dependencies = dependencyService.getDependencies(repositoryId);

            List<Map<String, Object>> result = dependencies.stream().map(dep -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("groupId", dep.getGroupId());
                map.put("artifactId", dep.getArtifactId());
                map.put("version", dep.getVersion());
                map.put("type", dep.getType() != null ? dep.getType().name() : null);
                map.put("configuration", dep.getConfiguration());
                map.put("optional", dep.isOptional());
                map.put("classifier", dep.getClassifier());
                map.put("typeClassifier", dep.getTypeClassifier());
                map.put("internal", dep.isInternal());
                map.put("repositoryId", dep.getRepositoryId());
                return map;
            }).toList();

            return ResponseEntity.ok(ApiResponse.success("Dependencies retrieved successfully", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("dependencies", e.getMessage()));
        }
    }

    /**
     * Get dependency statistics.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get dependency statistics", description = "Retrieve dependency statistics including total count, by scope, duplicates, missing versions, and snapshot dependencies.")
    public ResponseEntity<ApiResponse<?>> getStatistics(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        try {
            Map<String, Object> statistics = dependencyService.getStatistics(repositoryId);
            return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", statistics));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("statistics", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("statistics", e.getMessage()));
        }
    }
}