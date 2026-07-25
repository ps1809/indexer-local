package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.HealthReport;
import com.projectiq.indexerlocal.model.RepositoryHealthStatistics;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.service.IndexMaintenanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for index maintenance operations.
 * Provides endpoints for manual maintenance execution, health reporting,
 * and statistics retrieval.
 */
@RestController
@RequestMapping("/api/v1/maintenance")
public class IndexMaintenanceController {

    private static final Logger log = LoggerFactory.getLogger(IndexMaintenanceController.class);

    private final IndexMaintenanceService indexMaintenanceService;

    public IndexMaintenanceController(IndexMaintenanceService indexMaintenanceService) {
        this.indexMaintenanceService = indexMaintenanceService;
    }

    /**
     * Execute full maintenance on all repositories.
     * Performs cleanup, consistency verification, and health reporting.
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<?>> executeFullMaintenance() {
        log.info("[API] POST /api/v1/maintenance/execute - Execute full maintenance");
        try {
            HealthReport report = indexMaintenanceService.executeFullMaintenance();
            return ResponseEntity.ok(ApiResponse.success("Full maintenance executed successfully", report.toMap()));
        } catch (Exception e) {
            log.error("[API] Full maintenance failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Full maintenance failed: " + e.getMessage()));
        }
    }

    /**
     * Execute maintenance on a specific repository.
     */
    @PostMapping("/execute/{repositoryId}")
    public ResponseEntity<ApiResponse<?>> executeRepositoryMaintenance(
            @PathVariable String repositoryId) {
        log.info("[API] POST /api/v1/maintenance/execute/{} - Execute repository maintenance", repositoryId);
        try {
            HealthReport report = indexMaintenanceService.executeRepositoryMaintenance(repositoryId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Maintenance executed for repository: " + repositoryId, report.toMap()));
        } catch (Exception e) {
            log.error("[API] Repository maintenance failed for {}: {}", repositoryId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Maintenance failed: " + e.getMessage()));
        }
    }

    /**
     * Execute cleanup operations on a specific repository.
     */
    @PostMapping("/cleanup/{repositoryId}")
    public ResponseEntity<ApiResponse<?>> executeCleanup(
            @PathVariable String repositoryId) {
        log.info("[API] POST /api/v1/maintenance/cleanup/{} - Execute cleanup", repositoryId);
        try {
            HealthReport.CleanupSummary summary = indexMaintenanceService.executeCleanup(repositoryId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Cleanup completed for repository: " + repositoryId, summary.toMap()));
        } catch (Exception e) {
            log.error("[API] Cleanup failed for {}: {}", repositoryId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Cleanup failed: " + e.getMessage()));
        }
    }

    /**
     * Verify index consistency for a specific repository.
     */
    @PostMapping("/verify/{repositoryId}")
    public ResponseEntity<ApiResponse<?>> verifyIndex(
            @PathVariable String repositoryId) {
        log.info("[API] POST /api/v1/maintenance/verify/{} - Verify index", repositoryId);
        try {
            HealthReport report = indexMaintenanceService.verifyIndex(repositoryId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Index verification completed for repository: " + repositoryId, report.toMap()));
        } catch (Exception e) {
            log.error("[API] Index verification failed for {}: {}", repositoryId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Index verification failed: " + e.getMessage()));
        }
    }

    /**
     * Generate health report for a specific repository.
     */
    @GetMapping("/health/{repositoryId}")
    public ResponseEntity<ApiResponse<?>> getHealthReport(
            @PathVariable String repositoryId) {
        log.info("[API] GET /api/v1/maintenance/health/{} - Get health report", repositoryId);
        try {
            HealthReport report = indexMaintenanceService.generateHealthReport(repositoryId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Health report generated for repository: " + repositoryId, report.toMap()));
        } catch (Exception e) {
            log.error("[API] Health report generation failed for {}: {}", repositoryId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Health report generation failed: " + e.getMessage()));
        }
    }

    /**
     * Generate overall health report for all repositories.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> getOverallHealthReport() {
        log.info("[API] GET /api/v1/maintenance/health - Get overall health report");
        try {
            HealthReport report = indexMaintenanceService.generateOverallHealthReport();
            return ResponseEntity.ok(ApiResponse.success("Overall health report generated", report.toMap()));
        } catch (Exception e) {
            log.error("[API] Overall health report generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Health report generation failed: " + e.getMessage()));
        }
    }

    /**
     * Get health statistics for a specific repository.
     */
    @GetMapping("/statistics/{repositoryId}")
    public ResponseEntity<ApiResponse<?>> getRepositoryStatistics(
            @PathVariable String repositoryId) {
        log.info("[API] GET /api/v1/maintenance/statistics/{} - Get repository statistics", repositoryId);
        try {
            RepositoryHealthStatistics stats = indexMaintenanceService.getRepositoryHealthStatistics(repositoryId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Statistics retrieved for repository: " + repositoryId, stats.toMap()));
        } catch (Exception e) {
            log.error("[API] Statistics retrieval failed for {}: {}", repositoryId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Statistics retrieval failed: " + e.getMessage()));
        }
    }

    /**
     * Get overall health statistics across all repositories.
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<?>> getOverallStatistics() {
        log.info("[API] GET /api/v1/maintenance/statistics - Get overall statistics");
        try {
            RepositoryHealthStatistics stats = indexMaintenanceService.getOverallHealthStatistics();
            return ResponseEntity.ok(ApiResponse.success("Overall statistics retrieved", stats.toMap()));
        } catch (Exception e) {
            log.error("[API] Overall statistics retrieval failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Statistics retrieval failed: " + e.getMessage()));
        }
    }
}