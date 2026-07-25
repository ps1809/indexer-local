package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Index Maintenance Engine responsible for continuously validating, cleaning,
 * repairing, and optimizing the repository index.
 * <p>
 * Ensures long-term consistency between the filesystem and the index database,
 * removes obsolete data, detects inconsistencies, and exposes repository health metrics.
 */
@Service
public class IndexMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(IndexMaintenanceService.class);

    private final JdbcTemplate jdbcTemplate;
    private final RepositoryRepository repositoryRepository;
    private final IndexRepository indexRepository;
    private final FileHashService fileHashService;
    private final DependencyRepository dependencyRepository;
    private final RepositoryStatisticsService repositoryStatisticsService;

    public IndexMaintenanceService(
            JdbcTemplate jdbcTemplate,
            RepositoryRepository repositoryRepository,
            IndexRepository indexRepository,
            FileHashService fileHashService,
            DependencyRepository dependencyRepository,
            RepositoryStatisticsService repositoryStatisticsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.repositoryRepository = repositoryRepository;
        this.indexRepository = indexRepository;
        this.fileHashService = fileHashService;
        this.dependencyRepository = dependencyRepository;
        this.repositoryStatisticsService = repositoryStatisticsService;
    }

    // ==================== Public API ====================

    /**
     * Execute full maintenance on all repositories.
     * Performs cleanup, consistency verification, and health reporting.
     *
     * @return health report for all repositories
     */
    @Transactional
    public HealthReport executeFullMaintenance() {
        long startTime = System.currentTimeMillis();
        log.info("[INDEX-MAINTENANCE] Starting full maintenance on all repositories");

        HealthReport report = new HealthReport();
        report.setRepositoryId("ALL");
        report.setRepositoryName("All Repositories");

        try {
            List<com.projectiq.indexerlocal.model.Repository> repositories = repositoryRepository.findAll();

            for (com.projectiq.indexerlocal.model.Repository repo : repositories) {
                try {
                    performRepositoryCleanup(repo.getRepositoryId(), report);
                } catch (Exception e) {
                    log.warn("[INDEX-MAINTENANCE] Cleanup failed for repository {}: {}",
                            repo.getRepositoryId(), e.getMessage());
                    report.addInconsistency("Cleanup failed for " + repo.getRepositoryId() + ": " + e.getMessage());
                }
            }

            // Generate overall health statistics
            RepositoryHealthStatistics stats = generateOverallHealthStatistics();
            report.setHealthStatistics(stats);

            // Generate recommendations
            generateRecommendations(report);

            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);
            log.info("[INDEX-MAINTENANCE] Full maintenance completed in {}ms", report.getProcessingDurationMs());

        } catch (Exception e) {
            log.error("[INDEX-MAINTENANCE] Full maintenance failed: {}", e.getMessage(), e);
            report.addInconsistency("Full maintenance failed: " + e.getMessage());
        }

        return report;
    }

    /**
     * Execute maintenance on a specific repository.
     *
     * @param repositoryId the repository identifier
     * @return health report for the repository
     */
    @Transactional
    public HealthReport executeRepositoryMaintenance(String repositoryId) {
        long startTime = System.currentTimeMillis();
        log.info("[INDEX-MAINTENANCE] Starting maintenance for repository: {}", repositoryId);

        HealthReport report = new HealthReport();
        report.setRepositoryId(repositoryId);

        com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo == null) {
            report.addInconsistency("Repository not found: " + repositoryId);
            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);
            return report;
        }

        report.setRepositoryName(repo.getRepositoryName());
        report.setRepositoryStatus(repo.getStatus());

        try {
            // Perform cleanup
            performRepositoryCleanup(repositoryId, report);

            // Verify consistency
            verifyRepositoryConsistency(repositoryId, report);

            // Generate health statistics
            RepositoryHealthStatistics stats = generateRepositoryHealthStatistics(repositoryId);
            report.setHealthStatistics(stats);

            // Generate recommendations
            generateRepositoryRecommendations(repositoryId, report);

            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);
            log.info("[INDEX-MAINTENANCE] Maintenance completed for repository {} in {}ms",
                    repositoryId, report.getProcessingDurationMs());

        } catch (Exception e) {
            log.error("[INDEX-MAINTENANCE] Maintenance failed for repository {}: {}",
                    repositoryId, e.getMessage(), e);
            report.addInconsistency("Maintenance failed: " + e.getMessage());
            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);
        }

        return report;
    }

    /**
     * Execute cleanup operations on a specific repository.
     *
     * @param repositoryId the repository identifier
     * @return cleanup summary
     */
    @Transactional
    public HealthReport.CleanupSummary executeCleanup(String repositoryId) {
        log.info("[INDEX-MAINTENANCE] Executing cleanup for repository: {}", repositoryId);
        HealthReport.CleanupSummary summary = new HealthReport.CleanupSummary();

        try {
            com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
            if (repo == null) {
                log.warn("[INDEX-MAINTENANCE] Repository not found for cleanup: {}", repositoryId);
                return summary;
            }

            // Remove deleted file records
            removeDeletedFileRecords(repositoryId, repo.getWorkspacePath(), summary);

            // Remove orphaned symbols (classes, methods, fields without parent file)
            removeOrphanedSymbols(repositoryId, summary);

            // Remove orphaned relationships
            removeOrphanedRelationships(repositoryId, summary);

            // Remove orphaned Spring components
            removeOrphanedSpringComponents(repositoryId, summary);

            // Remove orphaned REST endpoints
            removeOrphanedRestEndpoints(repositoryId, summary);

            // Remove stale dependency records
            removeStaleDependencies(repositoryId, summary);

            log.info("[INDEX-MAINTENANCE] Cleanup completed for repository {}. Total cleaned: {}",
                    repositoryId, summary.getTotalRecordsCleaned());

        } catch (Exception e) {
            log.error("[INDEX-MAINTENANCE] Cleanup failed for repository {}: {}",
                    repositoryId, e.getMessage(), e);
        }

        return summary;
    }

    /**
     * Execute full index verification on a specific repository.
     *
     * @param repositoryId the repository identifier
     * @return health report with verification results
     */
    @Transactional
    public HealthReport verifyIndex(String repositoryId) {
        long startTime = System.currentTimeMillis();
        log.info("[INDEX-MAINTENANCE] Verifying index for repository: {}", repositoryId);

        HealthReport report = new HealthReport();
        report.setRepositoryId(repositoryId);

        com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo == null) {
            report.addInconsistency("Repository not found: " + repositoryId);
            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);
            return report;
        }

        report.setRepositoryName(repo.getRepositoryName());
        report.setRepositoryStatus(repo.getStatus());

        try {
            verifyRepositoryConsistency(repositoryId, report);
            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("[INDEX-MAINTENANCE] Index verification failed for repository {}: {}",
                    repositoryId, e.getMessage(), e);
            report.addInconsistency("Verification failed: " + e.getMessage());
            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);
        }

        return report;
    }

    /**
     * Generate health report for a specific repository.
     *
     * @param repositoryId the repository identifier
     * @return health report
     */
    public HealthReport generateHealthReport(String repositoryId) {
        long startTime = System.currentTimeMillis();
        log.info("[INDEX-MAINTENANCE] Generating health report for repository: {}", repositoryId);

        HealthReport report = new HealthReport();
        report.setRepositoryId(repositoryId);

        com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo == null) {
            report.addInconsistency("Repository not found: " + repositoryId);
            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);
            return report;
        }

        report.setRepositoryName(repo.getRepositoryName());
        report.setRepositoryStatus(repo.getStatus());

        try {
            // Check for orphan records
            long orphanCount = countOrphanRecords(repositoryId);
            report.setOrphanRecords(orphanCount);

            // Check for missing records
            long missingCount = countMissingRecords(repositoryId, repo.getWorkspacePath());
            report.setMissingRecords(missingCount);

            // Verify consistency
            verifyRepositoryConsistency(repositoryId, report);

            // Generate health statistics
            RepositoryHealthStatistics stats = generateRepositoryHealthStatistics(repositoryId);
            report.setHealthStatistics(stats);

            // Generate recommendations
            generateRepositoryRecommendations(repositoryId, report);

            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("[INDEX-MAINTENANCE] Health report generation failed for repository {}: {}",
                    repositoryId, e.getMessage(), e);
            report.addInconsistency("Health report generation failed: " + e.getMessage());
        }

        return report;
    }

    /**
     * Generate health report for all repositories.
     *
     * @return health report
     */
    public HealthReport generateOverallHealthReport() {
        long startTime = System.currentTimeMillis();
        log.info("[INDEX-MAINTENANCE] Generating overall health report");

        HealthReport report = new HealthReport();
        report.setRepositoryId("ALL");
        report.setRepositoryName("All Repositories");

        try {
            List<com.projectiq.indexerlocal.model.Repository> repositories = repositoryRepository.findAll();
            long totalOrphans = 0;
            long totalMissing = 0;

            for (com.projectiq.indexerlocal.model.Repository repo : repositories) {
                try {
                    totalOrphans += countOrphanRecords(repo.getRepositoryId());
                    totalMissing += countMissingRecords(repo.getRepositoryId(), repo.getWorkspacePath());
                } catch (Exception e) {
                    log.warn("[INDEX-MAINTENANCE] Error checking repository {}: {}",
                            repo.getRepositoryId(), e.getMessage());
                }
            }

            report.setOrphanRecords(totalOrphans);
            report.setMissingRecords(totalMissing);

            RepositoryHealthStatistics stats = generateOverallHealthStatistics();
            report.setHealthStatistics(stats);

            generateRecommendations(report);
            report.setProcessingDurationMs(System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("[INDEX-MAINTENANCE] Overall health report generation failed: {}", e.getMessage(), e);
            report.addInconsistency("Health report generation failed: " + e.getMessage());
        }

        return report;
    }

    /**
     * Get repository health statistics for a specific repository.
     *
     * @param repositoryId the repository identifier
     * @return health statistics
     */
    public RepositoryHealthStatistics getRepositoryHealthStatistics(String repositoryId) {
        return generateRepositoryHealthStatistics(repositoryId);
    }

    /**
     * Get overall health statistics across all repositories.
     *
     * @return health statistics
     */
    public RepositoryHealthStatistics getOverallHealthStatistics() {
        return generateOverallHealthStatistics();
    }

    // ==================== Scheduled Maintenance ====================

    /**
     * Scheduled maintenance task that runs daily at 2:00 AM.
     * Performs full maintenance on all repositories.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledMaintenance() {
        log.info("[INDEX-MAINTENANCE] Starting scheduled daily maintenance");
        try {
            HealthReport report = executeFullMaintenance();
            log.info("[INDEX-MAINTENANCE] Scheduled maintenance completed. " +
                            "Orphans found: {}, Inconsistencies: {}, Cleaned: {}",
                    report.getOrphanRecords(),
                    report.getInconsistencies().size(),
                    report.getCleanupSummary() != null ? report.getCleanupSummary().getTotalRecordsCleaned() : 0);
        } catch (Exception e) {
            log.error("[INDEX-MAINTENANCE] Scheduled maintenance failed: {}", e.getMessage(), e);
        }
    }

    // ==================== Private Cleanup Methods ====================

    private void performRepositoryCleanup(String repositoryId, HealthReport report) {
        HealthReport.CleanupSummary summary = report.getCleanupSummary();
        if (summary == null) {
            summary = new HealthReport.CleanupSummary();
            report.setCleanupSummary(summary);
        }

        com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo == null) {
            return;
        }

        // Remove deleted file records
        removeDeletedFileRecords(repositoryId, repo.getWorkspacePath(), summary);

        // Remove orphaned symbols
        removeOrphanedSymbols(repositoryId, summary);

        // Remove orphaned relationships
        removeOrphanedRelationships(repositoryId, summary);

        // Remove orphaned Spring components
        removeOrphanedSpringComponents(repositoryId, summary);

        // Remove orphaned REST endpoints
        removeOrphanedRestEndpoints(repositoryId, summary);

        // Remove stale dependency records
        removeStaleDependencies(repositoryId, summary);
    }

    private void removeDeletedFileRecords(String repositoryId, String workspacePath, HealthReport.CleanupSummary summary) {
        if (workspacePath == null || workspacePath.isEmpty()) {
            return;
        }

        try {
            // Find file_index records where the file no longer exists on disk
            List<Map<String, Object>> fileRecords = jdbcTemplate.queryForList(
                    "SELECT id, file_path FROM file_index WHERE file_path LIKE ?",
                    "%" + repositoryId + "%");

            for (Map<String, Object> record : fileRecords) {
                String filePath = (String) record.get("file_path");
                if (filePath != null && !Files.exists(Paths.get(filePath))) {
                    // File no longer exists, remove from index
                    Long fileId = ((Number) record.get("id")).longValue();
                    indexRepository.deleteFileIndexByFilePath(filePath);
                    summary.incrementDeletedFileRecords();
                    log.debug("[INDEX-MAINTENANCE] Removed deleted file record: {}", filePath);
                }
            }
        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error removing deleted file records for repository {}: {}",
                    repositoryId, e.getMessage());
        }
    }

    private void removeOrphanedSymbols(String repositoryId, HealthReport.CleanupSummary summary) {
        try {
            // Remove class_info records that reference non-existent file_index records
            List<Map<String, Object>> orphanedClasses = jdbcTemplate.queryForList(
                    "SELECT ci.id FROM class_info ci " +
                    "LEFT JOIN file_index fi ON ci.file_index_id = fi.id " +
                    "WHERE fi.id IS NULL");

            for (Map<String, Object> row : orphanedClasses) {
                Long classId = ((Number) row.get("id")).longValue();
                // Delete methods and fields referencing this class
                jdbcTemplate.update("DELETE FROM method_info WHERE class_id = ?", classId);
                jdbcTemplate.update("DELETE FROM field_info WHERE class_id = ?", classId);
                jdbcTemplate.update("DELETE FROM annotation_info WHERE target_type = 'CLASS' AND target_id = ?", classId);
                jdbcTemplate.update("DELETE FROM class_info WHERE id = ?", classId);
                summary.incrementOrphanedSymbolsRemoved();
            }

            // Remove method_info records that reference non-existent class_info records
            List<Map<String, Object>> orphanedMethods = jdbcTemplate.queryForList(
                    "SELECT mi.id FROM method_info mi " +
                    "LEFT JOIN class_info ci ON mi.class_id = ci.id " +
                    "WHERE ci.id IS NULL");

            for (Map<String, Object> row : orphanedMethods) {
                Long methodId = ((Number) row.get("id")).longValue();
                jdbcTemplate.update("DELETE FROM method_info WHERE id = ?", methodId);
                summary.incrementOrphanedSymbolsRemoved();
            }

            // Remove field_info records that reference non-existent class_info records
            List<Map<String, Object>> orphanedFields = jdbcTemplate.queryForList(
                    "SELECT fi.id FROM field_info fi " +
                    "LEFT JOIN class_info ci ON fi.class_id = ci.id " +
                    "WHERE ci.id IS NULL");

            for (Map<String, Object> row : orphanedFields) {
                Long fieldId = ((Number) row.get("id")).longValue();
                jdbcTemplate.update("DELETE FROM field_info WHERE id = ?", fieldId);
                summary.incrementOrphanedSymbolsRemoved();
            }

            // Remove import_info records that reference non-existent file_index records
            List<Map<String, Object>> orphanedImports = jdbcTemplate.queryForList(
                    "SELECT ii.id FROM import_info ii " +
                    "LEFT JOIN file_index fi ON ii.file_index_id = fi.id " +
                    "WHERE fi.id IS NULL");

            for (Map<String, Object> row : orphanedImports) {
                Long importId = ((Number) row.get("id")).longValue();
                jdbcTemplate.update("DELETE FROM import_info WHERE id = ?", importId);
                summary.incrementOrphanedSymbolsRemoved();
            }

            // Remove annotation_info records that reference non-existent targets
            List<Map<String, Object>> orphanedAnnotations = jdbcTemplate.queryForList(
                    "SELECT ai.id FROM annotation_info ai " +
                    "WHERE ai.target_type = 'CLASS' AND ai.target_id NOT IN (SELECT id FROM class_info)");

            for (Map<String, Object> row : orphanedAnnotations) {
                Long annotationId = ((Number) row.get("id")).longValue();
                jdbcTemplate.update("DELETE FROM annotation_info WHERE id = ?", annotationId);
                summary.incrementOrphanedSymbolsRemoved();
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error removing orphaned symbols for repository {}: {}",
                    repositoryId, e.getMessage());
        }
    }

    private void removeOrphanedRelationships(String repositoryId, HealthReport.CleanupSummary summary) {
        try {
            // Remove spring_component records that reference non-existent class_info records
            List<Map<String, Object>> orphanedComponents = jdbcTemplate.queryForList(
                    "SELECT sc.id FROM spring_component sc " +
                    "LEFT JOIN class_info ci ON sc.class_id = ci.id " +
                    "WHERE sc.class_id > 0 AND ci.id IS NULL");

            for (Map<String, Object> row : orphanedComponents) {
                Long componentId = ((Number) row.get("id")).longValue();
                jdbcTemplate.update("DELETE FROM spring_component WHERE id = ?", componentId);
                summary.incrementOrphanedRelationshipsRemoved();
            }

            // Remove spring_component records that reference non-existent file_index records
            List<Map<String, Object>> orphanedByFile = jdbcTemplate.queryForList(
                    "SELECT sc.id FROM spring_component sc " +
                    "LEFT JOIN file_index fi ON sc.file_index_id = fi.id " +
                    "WHERE sc.file_index_id > 0 AND fi.id IS NULL");

            for (Map<String, Object> row : orphanedByFile) {
                Long componentId = ((Number) row.get("id")).longValue();
                jdbcTemplate.update("DELETE FROM spring_component WHERE id = ?", componentId);
                summary.incrementOrphanedRelationshipsRemoved();
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error removing orphaned relationships for repository {}: {}",
                    repositoryId, e.getMessage());
        }
    }

    private void removeOrphanedSpringComponents(String repositoryId, HealthReport.CleanupSummary summary) {
        try {
            // Remove spring_component records for repositories that no longer exist
            List<Map<String, Object>> orphanedSpring = jdbcTemplate.queryForList(
                    "SELECT DISTINCT sc.id FROM spring_component sc " +
                    "LEFT JOIN repository r ON sc.repository_id = r.repository_id " +
                    "WHERE r.repository_id IS NULL");

            for (Map<String, Object> row : orphanedSpring) {
                Long componentId = ((Number) row.get("id")).longValue();
                jdbcTemplate.update("DELETE FROM spring_component WHERE id = ?", componentId);
                summary.incrementOrphanedSpringComponentsRemoved();
            }

            // Remove spring_component records with REST_API suffix where the repository no longer exists
            List<Map<String, Object>> orphanedRestApi = jdbcTemplate.queryForList(
                    "SELECT DISTINCT sc.id FROM spring_component sc " +
                    "WHERE sc.repository_id LIKE '%:REST_API%' " +
                    "AND sc.repository_id NOT IN (SELECT repository_id FROM repository)");

            for (Map<String, Object> row : orphanedRestApi) {
                Long componentId = ((Number) row.get("id")).longValue();
                jdbcTemplate.update("DELETE FROM spring_component WHERE id = ?", componentId);
                summary.incrementOrphanedSpringComponentsRemoved();
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error removing orphaned Spring components for repository {}: {}",
                    repositoryId, e.getMessage());
        }
    }

    private void removeOrphanedRestEndpoints(String repositoryId, HealthReport.CleanupSummary summary) {
        try {
            // Remove REST endpoint spring_component records where the source file no longer exists
            List<Map<String, Object>> orphanedEndpoints = jdbcTemplate.queryForList(
                    "SELECT sc.id, sc.source_file FROM spring_component sc " +
                    "WHERE sc.repository_id = ? AND sc.component_type = 'REST_ENDPOINT'",
                    repositoryId + ":REST_API");

            for (Map<String, Object> row : orphanedEndpoints) {
                String sourceFile = (String) row.get("source_file");
                if (sourceFile != null && !sourceFile.isEmpty() && !sourceFile.startsWith("produces=")) {
                    // Check if the source file still exists
                    if (!Files.exists(Paths.get(sourceFile))) {
                        Long endpointId = ((Number) row.get("id")).longValue();
                        jdbcTemplate.update("DELETE FROM spring_component WHERE id = ?", endpointId);
                        summary.incrementOrphanedRestEndpointsRemoved();
                    }
                }
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error removing orphaned REST endpoints for repository {}: {}",
                    repositoryId, e.getMessage());
        }
    }

    private void removeStaleDependencies(String repositoryId, HealthReport.CleanupSummary summary) {
        try {
            // Check if the repository still exists
            com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
            if (repo == null) {
                // Repository no longer exists, remove all its dependencies
                List<Dependency> deps = dependencyRepository.findByRepositoryId(repositoryId);
                for (Dependency dep : deps) {
                    dependencyRepository.deleteByRepositoryId(repositoryId);
                    summary.incrementStaleDependenciesRemoved();
                }
            }
        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error removing stale dependencies for repository {}: {}",
                    repositoryId, e.getMessage());
        }
    }

    // ==================== Private Consistency Verification Methods ====================

    private void verifyRepositoryConsistency(String repositoryId, HealthReport report) {
        try {
            // Verify file metadata consistency
            verifyFileMetadataConsistency(repositoryId, report);

            // Verify symbol ownership
            verifySymbolOwnership(repositoryId, report);

            // Verify relationship integrity
            verifyRelationshipIntegrity(repositoryId, report);

            // Verify dependency integrity
            verifyDependencyIntegrity(repositoryId, report);

            // Verify hash consistency
            verifyHashConsistency(repositoryId, report);

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Consistency verification failed for repository {}: {}",
                    repositoryId, e.getMessage());
            report.addInconsistency("Consistency verification failed: " + e.getMessage());
        }
    }

    private void verifyFileMetadataConsistency(String repositoryId, HealthReport report) {
        try {
            com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
            if (repo == null || repo.getWorkspacePath() == null) {
                return;
            }

            // Check if indexed files still exist on disk
            List<Map<String, Object>> fileRecords = jdbcTemplate.queryForList(
                    "SELECT id, file_path FROM file_index WHERE file_path LIKE ?",
                    "%" + repositoryId + "%");

            for (Map<String, Object> record : fileRecords) {
                String filePath = (String) record.get("file_path");
                if (filePath != null && !Files.exists(Paths.get(filePath))) {
                    report.addInconsistency("Indexed file no longer exists on disk: " + filePath);
                }
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] File metadata consistency check failed: {}", e.getMessage());
        }
    }

    private void verifySymbolOwnership(String repositoryId, HealthReport report) {
        try {
            // Check for classes without a valid file_index reference
            Long orphanedClasses = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class_info ci " +
                    "LEFT JOIN file_index fi ON ci.file_index_id = fi.id " +
                    "WHERE fi.id IS NULL", Long.class);

            if (orphanedClasses != null && orphanedClasses > 0) {
                report.addInconsistency("Found " + orphanedClasses + " class(es) without a valid file reference");
            }

            // Check for methods without a valid class reference
            Long orphanedMethods = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM method_info mi " +
                    "LEFT JOIN class_info ci ON mi.class_id = ci.id " +
                    "WHERE ci.id IS NULL", Long.class);

            if (orphanedMethods != null && orphanedMethods > 0) {
                report.addInconsistency("Found " + orphanedMethods + " method(s) without a valid class reference");
            }

            // Check for fields without a valid class reference
            Long orphanedFields = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM field_info fi " +
                    "LEFT JOIN class_info ci ON fi.class_id = ci.id " +
                    "WHERE ci.id IS NULL", Long.class);

            if (orphanedFields != null && orphanedFields > 0) {
                report.addInconsistency("Found " + orphanedFields + " field(s) without a valid class reference");
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Symbol ownership verification failed: {}", e.getMessage());
        }
    }

    private void verifyRelationshipIntegrity(String repositoryId, HealthReport report) {
        try {
            // Check for Spring components without valid class reference
            Long orphanedSpringComponents = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_component sc " +
                    "LEFT JOIN class_info ci ON sc.class_id = ci.id " +
                    "WHERE sc.class_id > 0 AND ci.id IS NULL", Long.class);

            if (orphanedSpringComponents != null && orphanedSpringComponents > 0) {
                report.addInconsistency("Found " + orphanedSpringComponents + " Spring component(s) without a valid class reference");
            }

            // Check for Spring components without valid file reference
            Long orphanedByFile = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_component sc " +
                    "LEFT JOIN file_index fi ON sc.file_index_id = fi.id " +
                    "WHERE sc.file_index_id > 0 AND fi.id IS NULL", Long.class);

            if (orphanedByFile != null && orphanedByFile > 0) {
                report.addInconsistency("Found " + orphanedByFile + " Spring component(s) without a valid file reference");
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Relationship integrity verification failed: {}", e.getMessage());
        }
    }

    private void verifyDependencyIntegrity(String repositoryId, HealthReport report) {
        try {
            // Check if the repository has dependencies but no longer exists
            com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
            if (repo == null) {
                boolean hasDeps = dependencyRepository.hasDependencies(repositoryId);
                if (hasDeps) {
                    report.addInconsistency("Repository has stale dependency records but no longer exists");
                }
            }
        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Dependency integrity verification failed: {}", e.getMessage());
        }
    }

    private void verifyHashConsistency(String repositoryId, HealthReport report) {
        try {
            com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
            if (repo == null || repo.getWorkspacePath() == null) {
                return;
            }

            // Get all hash records for the repository
            List<FileHashRecord> hashRecords = fileHashService.getStoredHashes(repositoryId);
            int mismatchedHashes = 0;

            for (FileHashRecord record : hashRecords) {
                String filePath = record.getFilePath();
                if (Files.exists(Paths.get(filePath))) {
                    String currentHash = fileHashService.computeHash(filePath);
                    if (currentHash != null && record.hasHashChanged(currentHash)) {
                        mismatchedHashes++;
                    }
                }
            }

            if (mismatchedHashes > 0) {
                report.addInconsistency("Found " + mismatchedHashes + " file(s) with mismatched hashes");
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Hash consistency verification failed: {}", e.getMessage());
        }
    }

    // ==================== Private Statistics Methods ====================

    private RepositoryHealthStatistics generateRepositoryHealthStatistics(String repositoryId) {
        RepositoryHealthStatistics stats = new RepositoryHealthStatistics();
        stats.setRepositoryId(repositoryId);

        com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo != null) {
            stats.setRepositoryName(repo.getRepositoryName());
            stats.setLastIndexingTime(repo.getLastIndexingTimestamp());
        }

        try {
            // Count indexed files
            Long fileCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM file_index WHERE file_path LIKE ?",
                    Long.class, "%" + repositoryId + "%");
            stats.setIndexedFiles(fileCount != null ? fileCount : 0);

            // Count indexed symbols (classes + methods + fields)
            Long classCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class_info", Long.class);
            Long methodCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM method_info", Long.class);
            Long fieldCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM field_info", Long.class);
            stats.setIndexedSymbols((classCount != null ? classCount : 0) +
                    (methodCount != null ? methodCount : 0) +
                    (fieldCount != null ? fieldCount : 0));

            // Count Spring components
            Long springCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_component WHERE repository_id = ?",
                    Long.class, repositoryId);
            stats.setSpringComponents(springCount != null ? springCount : 0);

            // Count REST endpoints
            Long restCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'REST_ENDPOINT'",
                    Long.class, repositoryId + ":REST_API");
            stats.setRestEndpoints(restCount != null ? restCount : 0);

            // Count relationships (Spring components with class references)
            Long relCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND class_id > 0",
                    Long.class, repositoryId);
            stats.setRelationships(relCount != null ? relCount : 0);

            // Count dependencies
            int depCount = dependencyRepository.getCountByRepositoryId(repositoryId);
            stats.setDependencies(depCount);

            // Calculate health score
            double healthScore = calculateHealthScore(stats);
            stats.setHealthScore(healthScore);

            // Determine health status
            if (healthScore >= 90.0) {
                stats.setHealthStatus("HEALTHY");
            } else if (healthScore >= 70.0) {
                stats.setHealthStatus("DEGRADED");
            } else if (healthScore >= 50.0) {
                stats.setHealthStatus("WARNING");
            } else {
                stats.setHealthStatus("CRITICAL");
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error generating health statistics for repository {}: {}",
                    repositoryId, e.getMessage());
            stats.setHealthStatus("ERROR");
            stats.setHealthScore(0.0);
        }

        return stats;
    }

    private RepositoryHealthStatistics generateOverallHealthStatistics() {
        RepositoryHealthStatistics stats = new RepositoryHealthStatistics();
        stats.setRepositoryId("ALL");
        stats.setRepositoryName("All Repositories");

        try {
            // Count indexed repositories
            List<com.projectiq.indexerlocal.model.Repository> repos = repositoryRepository.findAll();
            stats.setIndexedRepositories(repos.size());

            // Count indexed files
            Long fileCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM file_index", Long.class);
            stats.setIndexedFiles(fileCount != null ? fileCount : 0);

            // Count indexed symbols
            Long classCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class_info", Long.class);
            Long methodCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM method_info", Long.class);
            Long fieldCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM field_info", Long.class);
            stats.setIndexedSymbols((classCount != null ? classCount : 0) +
                    (methodCount != null ? methodCount : 0) +
                    (fieldCount != null ? fieldCount : 0));

            // Count Spring components
            Long springCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_component", Long.class);
            stats.setSpringComponents(springCount != null ? springCount : 0);

            // Count REST endpoints
            Long restCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_component WHERE component_type = 'REST_ENDPOINT'",
                    Long.class);
            stats.setRestEndpoints(restCount != null ? restCount : 0);

            // Count relationships
            Long relCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_component WHERE class_id > 0",
                    Long.class);
            stats.setRelationships(relCount != null ? relCount : 0);

            // Count dependencies across all repositories
            long totalDeps = 0;
            for (com.projectiq.indexerlocal.model.Repository repo : repos) {
                totalDeps += dependencyRepository.getCountByRepositoryId(repo.getRepositoryId());
            }
            stats.setDependencies(totalDeps);

            // Calculate health score
            double healthScore = calculateHealthScore(stats);
            stats.setHealthScore(healthScore);

            if (healthScore >= 90.0) {
                stats.setHealthStatus("HEALTHY");
            } else if (healthScore >= 70.0) {
                stats.setHealthStatus("DEGRADED");
            } else if (healthScore >= 50.0) {
                stats.setHealthStatus("WARNING");
            } else {
                stats.setHealthStatus("CRITICAL");
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error generating overall health statistics: {}", e.getMessage());
            stats.setHealthStatus("ERROR");
            stats.setHealthScore(0.0);
        }

        return stats;
    }

    private double calculateHealthScore(RepositoryHealthStatistics stats) {
        double score = 100.0;

        // Deduct for missing data
        if (stats.getIndexedFiles() == 0 && stats.getIndexedRepositories() > 0) {
            score -= 20.0;
        }
        if (stats.getIndexedSymbols() == 0 && stats.getIndexedFiles() > 0) {
            score -= 15.0;
        }

        // Deduct for potential issues (these are checked during verification)
        // Lower score if no recent indexing
        if (stats.getLastIndexingTime() != null) {
            long daysSinceLastIndex = Duration.between(stats.getLastIndexingTime(), LocalDateTime.now()).toDays();
            if (daysSinceLastIndex > 7) {
                score -= 10.0;
            }
            if (daysSinceLastIndex > 30) {
                score -= 15.0;
            }
        } else {
            score -= 20.0; // Never indexed
        }

        return Math.max(0.0, Math.min(100.0, score));
    }

    // ==================== Private Counting Methods ====================

    private long countOrphanRecords(String repositoryId) {
        long count = 0;

        try {
            // Count orphaned classes
            Long orphanedClasses = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class_info ci " +
                    "LEFT JOIN file_index fi ON ci.file_index_id = fi.id " +
                    "WHERE fi.id IS NULL", Long.class);
            count += orphanedClasses != null ? orphanedClasses : 0;

            // Count orphaned methods
            Long orphanedMethods = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM method_info mi " +
                    "LEFT JOIN class_info ci ON mi.class_id = ci.id " +
                    "WHERE ci.id IS NULL", Long.class);
            count += orphanedMethods != null ? orphanedMethods : 0;

            // Count orphaned fields
            Long orphanedFields = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM field_info fi " +
                    "LEFT JOIN class_info ci ON fi.class_id = ci.id " +
                    "WHERE ci.id IS NULL", Long.class);
            count += orphanedFields != null ? orphanedFields : 0;

            // Count orphaned Spring components
            Long orphanedSpring = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_component sc " +
                    "LEFT JOIN class_info ci ON sc.class_id = ci.id " +
                    "WHERE sc.class_id > 0 AND ci.id IS NULL", Long.class);
            count += orphanedSpring != null ? orphanedSpring : 0;

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error counting orphan records: {}", e.getMessage());
        }

        return count;
    }

    private long countMissingRecords(String repositoryId, String workspacePath) {
        if (workspacePath == null || workspacePath.isEmpty()) {
            return 0;
        }

        long count = 0;
        try {
            // Count indexed files that no longer exist on disk
            List<Map<String, Object>> fileRecords = jdbcTemplate.queryForList(
                    "SELECT file_path FROM file_index WHERE file_path LIKE ?",
                    "%" + repositoryId + "%");

            for (Map<String, Object> record : fileRecords) {
                String filePath = (String) record.get("file_path");
                if (filePath != null && !Files.exists(Paths.get(filePath))) {
                    count++;
                }
            }

        } catch (Exception e) {
            log.warn("[INDEX-MAINTENANCE] Error counting missing records: {}", e.getMessage());
        }

        return count;
    }

    // ==================== Private Recommendation Methods ====================

    private void generateRecommendations(HealthReport report) {
        if (report.getOrphanRecords() > 0) {
            report.addRecommendation("Run cleanup to remove " + report.getOrphanRecords() + " orphan record(s)");
        }
        if (report.getMissingRecords() > 0) {
            report.addRecommendation("Re-index repository to restore " + report.getMissingRecords() + " missing file record(s)");
        }
        if (!report.isIndexConsistent()) {
            report.addRecommendation("Index inconsistencies detected. Consider running a full re-index.");
        }
        if (report.getHealthStatistics() != null && report.getHealthStatistics().getHealthScore() < 70.0) {
            report.addRecommendation("Repository health score is low (" + report.getHealthStatistics().getHealthScore() +
                    "). Consider re-indexing the repository.");
        }
    }

    private void generateRepositoryRecommendations(String repositoryId, HealthReport report) {
        if (report.getOrphanRecords() > 0) {
            report.addRecommendation("Run cleanup to remove " + report.getOrphanRecords() + " orphan record(s)");
        }
        if (report.getMissingRecords() > 0) {
            report.addRecommendation("Re-index repository to restore " + report.getMissingRecords() + " missing file record(s)");
        }
        if (!report.isIndexConsistent()) {
            report.addRecommendation("Index inconsistencies detected. Consider running a full re-index.");
        }
        if (report.getHealthStatistics() != null && report.getHealthStatistics().getHealthScore() < 70.0) {
            report.addRecommendation("Repository health score is low (" + report.getHealthStatistics().getHealthScore() +
                    "). Consider re-indexing the repository.");
        }
    }
}