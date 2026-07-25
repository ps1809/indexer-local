package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.buildsearch.BuildSearchResult;
import com.projectiq.indexerlocal.service.BuildSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Build Search Engine.
 * Provides endpoints for deterministic discovery of build system metadata
 * across indexed repositories without filesystem scanning.
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Build Search Engine", description = "APIs for searching build system metadata across indexed repositories")
public class BuildSearchControllerV1 {

    private static final Logger log = LoggerFactory.getLogger(BuildSearchControllerV1.class);

    private final BuildSearchService buildSearchService;

    public BuildSearchControllerV1(BuildSearchService buildSearchService) {
        this.buildSearchService = buildSearchService;
    }

    /**
     * General build search with multiple optional filters.
     */
    @GetMapping("/build")
    @Operation(summary = "Search build metadata", description = "General search across build metadata with optional filters.")
    public ResponseEntity<?> searchBuild(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Build system type (MAVEN, GRADLE)")
            @RequestParam(required = false) String buildSystem,
            @Parameter(description = "Module name filter")
            @RequestParam(required = false) String moduleName,
            @Parameter(description = "Group ID filter")
            @RequestParam(required = false) String groupId,
            @Parameter(description = "Artifact ID filter")
            @RequestParam(required = false) String artifactId,
            @Parameter(description = "Version filter")
            @RequestParam(required = false) String version,
            @Parameter(description = "Plugin name filter")
            @RequestParam(required = false) String plugin,
            @Parameter(description = "Dependency coordinate filter")
            @RequestParam(required = false) String dependency,
            @Parameter(description = "Profile name filter")
            @RequestParam(required = false) String profile,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.searchBuild(
                    repositoryId, buildSystem, moduleName, groupId, artifactId, version,
                    plugin, dependency, profile, page, size);
            return ResponseEntity.ok(ApiResponse.success("Build search completed", result));
        } catch (Exception e) {
            log.error("Build search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Build search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for Maven projects.
     */
    @GetMapping("/build/maven")
    @Operation(summary = "Find Maven projects", description = "Search for Maven projects across indexed repositories.")
    public ResponseEntity<?> findMavenProjects(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Group ID filter")
            @RequestParam(required = false) String groupId,
            @Parameter(description = "Artifact ID filter")
            @RequestParam(required = false) String artifactId,
            @Parameter(description = "Version filter")
            @RequestParam(required = false) String version,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findMavenProjects(
                    repositoryId, groupId, artifactId, version, page, size);
            return ResponseEntity.ok(ApiResponse.success("Maven projects found", result));
        } catch (Exception e) {
            log.error("Maven project search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Maven project search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for Gradle projects.
     */
    @GetMapping("/build/gradle")
    @Operation(summary = "Find Gradle projects", description = "Search for Gradle projects across indexed repositories.")
    public ResponseEntity<?> findGradleProjects(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Project name filter")
            @RequestParam(required = false) String projectName,
            @Parameter(description = "Group filter")
            @RequestParam(required = false) String group,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findGradleProjects(
                    repositoryId, projectName, group, page, size);
            return ResponseEntity.ok(ApiResponse.success("Gradle projects found", result));
        } catch (Exception e) {
            log.error("Gradle project search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Gradle project search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for modules.
     */
    @GetMapping("/module")
    @Operation(summary = "Find modules", description = "Search for build modules across indexed repositories.")
    public ResponseEntity<?> findModules(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Module name filter")
            @RequestParam(required = false) String moduleName,
            @Parameter(description = "Parent module filter")
            @RequestParam(required = false) String parentModule,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findModules(
                    repositoryId, moduleName, parentModule, page, size);
            return ResponseEntity.ok(ApiResponse.success("Modules found", result));
        } catch (Exception e) {
            log.error("Module search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Module search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for plugins.
     */
    @GetMapping("/plugin")
    @Operation(summary = "Find plugins", description = "Search for build plugins across indexed repositories.")
    public ResponseEntity<?> findPlugins(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Plugin name filter")
            @RequestParam(required = false) String pluginName,
            @Parameter(description = "Module name filter")
            @RequestParam(required = false) String moduleName,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findPlugins(
                    repositoryId, pluginName, moduleName, page, size);
            return ResponseEntity.ok(ApiResponse.success("Plugins found", result));
        } catch (Exception e) {
            log.error("Plugin search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Plugin search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for dependencies.
     */
    @GetMapping("/dependency")
    @Operation(summary = "Find dependencies", description = "Search for build dependencies across indexed repositories.")
    public ResponseEntity<?> findDependencies(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Group ID filter")
            @RequestParam(required = false) String groupId,
            @Parameter(description = "Artifact ID filter")
            @RequestParam(required = false) String artifactId,
            @Parameter(description = "Version filter")
            @RequestParam(required = false) String version,
            @Parameter(description = "Module name filter")
            @RequestParam(required = false) String moduleName,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findDependencies(
                    repositoryId, groupId, artifactId, version, moduleName, page, size);
            return ResponseEntity.ok(ApiResponse.success("Dependencies found", result));
        } catch (Exception e) {
            log.error("Dependency search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Dependency search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for build profiles.
     */
    @GetMapping("/profile")
    @Operation(summary = "Find build profiles", description = "Search for build profiles across indexed repositories.")
    public ResponseEntity<?> findBuildProfiles(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Profile name filter")
            @RequestParam(required = false) String profileName,
            @Parameter(description = "Module name filter")
            @RequestParam(required = false) String moduleName,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findBuildProfiles(
                    repositoryId, profileName, moduleName, page, size);
            return ResponseEntity.ok(ApiResponse.success("Build profiles found", result));
        } catch (Exception e) {
            log.error("Build profile search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Build profile search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for parent projects.
     */
    @GetMapping("/build/parent")
    @Operation(summary = "Find parent projects", description = "Search for parent project references across indexed repositories.")
    public ResponseEntity<?> findParentProjects(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Parent group ID filter")
            @RequestParam(required = false) String parentGroupId,
            @Parameter(description = "Parent artifact ID filter")
            @RequestParam(required = false) String parentArtifactId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findParentProjects(
                    repositoryId, parentGroupId, parentArtifactId, page, size);
            return ResponseEntity.ok(ApiResponse.success("Parent projects found", result));
        } catch (Exception e) {
            log.error("Parent project search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Parent project search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for child modules.
     */
    @GetMapping("/build/children")
    @Operation(summary = "Find child modules", description = "Search for child modules of a given parent project.")
    public ResponseEntity<?> findChildModules(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Parent module name filter")
            @RequestParam(required = false) String parentModule,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findChildModules(
                    repositoryId, parentModule, page, size);
            return ResponseEntity.ok(ApiResponse.success("Child modules found", result));
        } catch (Exception e) {
            log.error("Child module search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Child module search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for build configurations.
     */
    @GetMapping("/build/configuration")
    @Operation(summary = "Find build configurations", description = "Search for build configurations across indexed repositories.")
    public ResponseEntity<?> findBuildConfigurations(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Build system type filter")
            @RequestParam(required = false) String buildSystem,
            @Parameter(description = "Packaging type filter (jar, war, pom)")
            @RequestParam(required = false) String packaging,
            @Parameter(description = "Module name filter")
            @RequestParam(required = false) String moduleName,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findBuildConfigurations(
                    repositoryId, buildSystem, packaging, moduleName, page, size);
            return ResponseEntity.ok(ApiResponse.success("Build configurations found", result));
        } catch (Exception e) {
            log.error("Build configuration search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Build configuration search failed: " + e.getMessage()));
        }
    }

    /**
     * Search for build files.
     */
    @GetMapping("/build/files")
    @Operation(summary = "Find build files", description = "Search for build files across indexed repositories.")
    public ResponseEntity<?> findBuildFiles(
            @Parameter(description = "Repository ID filter")
            @RequestParam(required = false) String repositoryId,
            @Parameter(description = "Build system type filter")
            @RequestParam(required = false) String buildSystem,
            @Parameter(description = "Build file name filter")
            @RequestParam(required = false) String buildFileName,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<BuildSearchResult> result = buildSearchService.findBuildFiles(
                    repositoryId, buildSystem, buildFileName, page, size);
            return ResponseEntity.ok(ApiResponse.success("Build files found", result));
        } catch (Exception e) {
            log.error("Build file search failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Build file search failed: " + e.getMessage()));
        }
    }
}
