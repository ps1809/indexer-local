package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.RepositorySummary;
import com.projectiq.indexerlocal.service.RepositoryStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for repository statistics and summary.
 */
@RestController
@RequestMapping("/api/v1/repositories")
@Tag(name = "Repository Statistics", description = "APIs for retrieving repository statistics and summary")
public class RepositoryStatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryStatisticsController.class);

    private final RepositoryStatisticsService repositoryStatisticsService;

    public RepositoryStatisticsController(RepositoryStatisticsService repositoryStatisticsService) {
        this.repositoryStatisticsService = repositoryStatisticsService;
    }

    /**
     * Get repository summary with all aggregated statistics.
     */
    @GetMapping("/{repositoryId}/summary")
    @Operation(summary = "Get repository summary", description = "Retrieve a consolidated overview of a repository's indexed metadata")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Repository summary retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Repository not found"),
        @ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<RepositorySummary> getRepositorySummary(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request for repository summary: repositoryId={}", repositoryId);

        try {
            RepositorySummary summary = repositoryStatisticsService.getRepositorySummary(repositoryId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, status={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        }
    }

    /**
     * Get consolidated repository statistics.
     */
    @GetMapping("/{repositoryId}/statistics")
    @Operation(summary = "Get repository statistics", description = "Retrieve consolidated repository statistics in JSON format")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Repository statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Repository not found"),
        @ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<Map<String, Object>> getRepositoryStatistics(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request for repository statistics: repositoryId={}", repositoryId);

        try {
            Map<String, Object> statistics = repositoryStatisticsService.getRepositoryStatistics(repositoryId);
            return ResponseEntity.ok(statistics);
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, status={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        }
    }
}