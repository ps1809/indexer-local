package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.TechnologyStack;
import com.projectiq.indexerlocal.service.TechnologyStackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for technology stack detection and retrieval.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/technology-stack")
@Tag(name = "Technology Stack", description = "APIs for detecting and retrieving technology stack information")
public class TechnologyStackController {

    private static final Logger logger = LoggerFactory.getLogger(TechnologyStackController.class);

    private final TechnologyStackService technologyStackService;

    public TechnologyStackController(TechnologyStackService technologyStackService) {
        this.technologyStackService = technologyStackService;
    }

    /**
     * Detect the technology stack for a repository.
     */
    @PostMapping
    @Operation(summary = "Detect technology stack", description = "Detect the technologies, frameworks, languages, libraries, and platforms used by a registered repository")
    public ResponseEntity<?> detectTechnologyStack(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        logger.info("Received request to detect technology stack for repository: {}", repositoryId);

        try {
            TechnologyStack techStack = technologyStackService.detectTechnologyStack(repositoryId);

            Map<String, Object> response = new HashMap<>();
            response.put("repositoryId", repositoryId);
            response.put("detectedAt", techStack.getDetectedAt() != null ? techStack.getDetectedAt().toString() : null);
            response.put("languages", techStack.getLanguages());
            response.put("frameworks", techStack.getFrameworks());
            response.put("buildTools", techStack.getBuildTools());
            response.put("databases", techStack.getDatabases());
            response.put("testingFrameworks", techStack.getTestingFrameworks());
            response.put("frontendTechnologies", techStack.getFrontendTechnologies());

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.warn("Technology stack detection failed for repository {}: {}", repositoryId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Precondition failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IllegalArgumentException e) {
            logger.warn("Technology stack detection failed for repository {}: {}", repositoryId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Unexpected error during technology stack detection for repository: {}", repositoryId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", "An unexpected error occurred");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get the detected technology stack for a repository.
     */
    @GetMapping
    @Operation(summary = "Get technology stack", description = "Retrieve the detected technology stack for a registered repository")
    public ResponseEntity<?> getTechnologyStack(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        logger.info("Received request to get technology stack for repository: {}", repositoryId);

        try {
            TechnologyStack techStack = technologyStackService.getTechnologyStack(repositoryId);

            Map<String, Object> response = new HashMap<>();
            response.put("repositoryId", repositoryId);
            response.put("detectedAt", techStack.getDetectedAt() != null ? techStack.getDetectedAt().toString() : null);
            response.put("languages", techStack.getLanguages());
            response.put("frameworks", techStack.getFrameworks());
            response.put("buildTools", techStack.getBuildTools());
            response.put("databases", techStack.getDatabases());
            response.put("testingFrameworks", techStack.getTestingFrameworks());
            response.put("frontendTechnologies", techStack.getFrontendTechnologies());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Technology stack retrieval failed for repository {}: {}", repositoryId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Unexpected error during technology stack retrieval for repository: {}", repositoryId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", "An unexpected error occurred");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}