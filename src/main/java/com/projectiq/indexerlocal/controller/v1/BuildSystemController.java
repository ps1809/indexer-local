package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.BuildMetadata;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.service.BuildSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for build system analysis.
 * Provides endpoints to analyze and retrieve build system information.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}")
@Tag(name = "Build System Analyzer", description = "APIs for analyzing and retrieving project build system information")
public class BuildSystemController {

    private final BuildSystemService buildSystemService;

    public BuildSystemController(BuildSystemService buildSystemService) {
        this.buildSystemService = buildSystemService;
    }

    /**
     * Analyze the build system of a repository.
     */
    @PostMapping("/build")
    @Operation(summary = "Analyze build system", description = "Analyzes the build system of the specified repository.")
    public ResponseEntity<ApiResponse<?>> analyzeBuildSystem(
            @Parameter(description = "Repository ID to analyze") @PathVariable String repositoryId) {
        try {
            BuildMetadata metadata = buildSystemService.analyzeBuild(repositoryId);
            return ResponseEntity.ok(ApiResponse.successWithMessage("Build system analysis completed"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("validation", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.internalError(e.getMessage()));
        }
    }

    /**
     * Get build system information for a repository.
     */
    @GetMapping("/build")
    @Operation(summary = "Get build system info", description = "Retrieves the build system information for the specified repository.")
    public ResponseEntity<ApiResponse<?>> getBuildSystem(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        try {
            BuildMetadata metadata = buildSystemService.getBuild(repositoryId);
            if (metadata == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.successWithMessage("Build system information retrieved"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.internalError(e.getMessage()));
        }
    }

    /**
     * Get modules for a repository.
     */
    @GetMapping("/modules")
    @Operation(summary = "Get modules", description = "Retrieves all discovered modules for the specified repository.")
    public ResponseEntity<ApiResponse<?>> getModules(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        try {
            List<BuildMetadata.ModuleInfo> modules = buildSystemService.getModules(repositoryId);
            return ResponseEntity.ok(ApiResponse.successWithMessage("Modules retrieved"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.internalError(e.getMessage()));
        }
    }

    /**
     * Detect build system type.
     */
    @GetMapping("/build/detect")
    @Operation(summary = "Detect build system", description = "Detects the build system type without extracting full metadata.")
    public ResponseEntity<ApiResponse<?>> detectBuildSystem(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        try {
            String buildSystemType = buildSystemService.detectBuildSystemType(repositoryId);
            return ResponseEntity.ok(ApiResponse.successWithMessage("Build system detected"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.internalError(e.getMessage()));
        }
    }

    /**
     * Check if Maven Wrapper is present.
     */
    @GetMapping("/build/maven-wrapper")
    @Operation(summary = "Check Maven Wrapper", description = "Checks if Maven Wrapper (mvnw) is present.")
    public ResponseEntity<ApiResponse<?>> checkMavenWrapper(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        try {
            boolean present = buildSystemService.isMavenWrapperPresent(repositoryId);
            return ResponseEntity.ok(ApiResponse.successWithMessage("Maven wrapper check completed"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.internalError(e.getMessage()));
        }
    }

    /**
     * Check if Gradle Wrapper is present.
     */
    @GetMapping("/build/gradle-wrapper")
    @Operation(summary = "Check Gradle Wrapper", description = "Checks if Gradle Wrapper (gradlew) is present.")
    public ResponseEntity<ApiResponse<?>> checkGradleWrapper(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        try {
            boolean present = buildSystemService.isGradleWrapperPresent(repositoryId);
            return ResponseEntity.ok(ApiResponse.successWithMessage("Gradle wrapper check completed"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.internalError(e.getMessage()));
        }
    }
}