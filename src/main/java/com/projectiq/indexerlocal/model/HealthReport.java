package com.projectiq.indexerlocal.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a comprehensive health report for a repository index.
 * Includes consistency verification results, cleanup summary, and recommendations.
 */
public class HealthReport {

    private String repositoryId;
    private String repositoryName;
    private LocalDateTime reportGeneratedAt;
    private long processingDurationMs;
    private RepositoryStatus repositoryStatus;
    private boolean indexConsistent;
    private long missingRecords;
    private long orphanRecords;
    private CleanupSummary cleanupSummary;
    private List<String> inconsistencies;
    private List<String> recommendations;
    private RepositoryHealthStatistics healthStatistics;

    public HealthReport() {
        this.reportGeneratedAt = LocalDateTime.now();
        this.inconsistencies = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.cleanupSummary = new CleanupSummary();
        this.indexConsistent = true;
        this.missingRecords = 0;
        this.orphanRecords = 0;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public LocalDateTime getReportGeneratedAt() {
        return reportGeneratedAt;
    }

    public void setReportGeneratedAt(LocalDateTime reportGeneratedAt) {
        this.reportGeneratedAt = reportGeneratedAt;
    }

    public long getProcessingDurationMs() {
        return processingDurationMs;
    }

    public void setProcessingDurationMs(long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
    }

    public RepositoryStatus getRepositoryStatus() {
        return repositoryStatus;
    }

    public void setRepositoryStatus(RepositoryStatus repositoryStatus) {
        this.repositoryStatus = repositoryStatus;
    }

    public boolean isIndexConsistent() {
        return indexConsistent;
    }

    public void setIndexConsistent(boolean indexConsistent) {
        this.indexConsistent = indexConsistent;
    }

    public long getMissingRecords() {
        return missingRecords;
    }

    public void setMissingRecords(long missingRecords) {
        this.missingRecords = missingRecords;
    }

    public long getOrphanRecords() {
        return orphanRecords;
    }

    public void setOrphanRecords(long orphanRecords) {
        this.orphanRecords = orphanRecords;
    }

    public CleanupSummary getCleanupSummary() {
        return cleanupSummary;
    }

    public void setCleanupSummary(CleanupSummary cleanupSummary) {
        this.cleanupSummary = cleanupSummary;
    }

    public List<String> getInconsistencies() {
        return inconsistencies;
    }

    public void setInconsistencies(List<String> inconsistencies) {
        this.inconsistencies = inconsistencies;
    }

    public void addInconsistency(String inconsistency) {
        this.inconsistencies.add(inconsistency);
        this.indexConsistent = false;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }

    public RepositoryHealthStatistics getHealthStatistics() {
        return healthStatistics;
    }

    public void setHealthStatistics(RepositoryHealthStatistics healthStatistics) {
        this.healthStatistics = healthStatistics;
    }

    /**
     * Convert to a map representation for API responses.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("repositoryId", repositoryId);
        map.put("repositoryName", repositoryName);
        map.put("reportGeneratedAt", reportGeneratedAt != null ? reportGeneratedAt.toString() : null);
        map.put("processingDurationMs", processingDurationMs);
        map.put("repositoryStatus", repositoryStatus != null ? repositoryStatus.name() : null);
        map.put("indexConsistent", indexConsistent);
        map.put("missingRecords", missingRecords);
        map.put("orphanRecords", orphanRecords);
        map.put("cleanupSummary", cleanupSummary != null ? cleanupSummary.toMap() : null);
        map.put("inconsistencies", inconsistencies);
        map.put("recommendations", recommendations);
        map.put("healthStatistics", healthStatistics != null ? healthStatistics.toMap() : null);
        return map;
    }

    /**
     * Summary of cleanup operations performed during maintenance.
     */
    public static class CleanupSummary {
        private long deletedFileRecords;
        private long orphanedSymbolsRemoved;
        private long orphanedRelationshipsRemoved;
        private long orphanedSpringComponentsRemoved;
        private long orphanedRestEndpointsRemoved;
        private long staleDependenciesRemoved;
        private long totalRecordsCleaned;

        public CleanupSummary() {
            this.deletedFileRecords = 0;
            this.orphanedSymbolsRemoved = 0;
            this.orphanedRelationshipsRemoved = 0;
            this.orphanedSpringComponentsRemoved = 0;
            this.orphanedRestEndpointsRemoved = 0;
            this.staleDependenciesRemoved = 0;
            this.totalRecordsCleaned = 0;
        }

        public long getDeletedFileRecords() {
            return deletedFileRecords;
        }

        public void setDeletedFileRecords(long deletedFileRecords) {
            this.deletedFileRecords = deletedFileRecords;
        }

        public long getOrphanedSymbolsRemoved() {
            return orphanedSymbolsRemoved;
        }

        public void setOrphanedSymbolsRemoved(long orphanedSymbolsRemoved) {
            this.orphanedSymbolsRemoved = orphanedSymbolsRemoved;
        }

        public long getOrphanedRelationshipsRemoved() {
            return orphanedRelationshipsRemoved;
        }

        public void setOrphanedRelationshipsRemoved(long orphanedRelationshipsRemoved) {
            this.orphanedRelationshipsRemoved = orphanedRelationshipsRemoved;
        }

        public long getOrphanedSpringComponentsRemoved() {
            return orphanedSpringComponentsRemoved;
        }

        public void setOrphanedSpringComponentsRemoved(long orphanedSpringComponentsRemoved) {
            this.orphanedSpringComponentsRemoved = orphanedSpringComponentsRemoved;
        }

        public long getOrphanedRestEndpointsRemoved() {
            return orphanedRestEndpointsRemoved;
        }

        public void setOrphanedRestEndpointsRemoved(long orphanedRestEndpointsRemoved) {
            this.orphanedRestEndpointsRemoved = orphanedRestEndpointsRemoved;
        }

        public long getStaleDependenciesRemoved() {
            return staleDependenciesRemoved;
        }

        public void setStaleDependenciesRemoved(long staleDependenciesRemoved) {
            this.staleDependenciesRemoved = staleDependenciesRemoved;
        }

        public long getTotalRecordsCleaned() {
            return totalRecordsCleaned;
        }

        public void setTotalRecordsCleaned(long totalRecordsCleaned) {
            this.totalRecordsCleaned = totalRecordsCleaned;
        }

        public void incrementDeletedFileRecords() {
            this.deletedFileRecords++;
            this.totalRecordsCleaned++;
        }

        public void incrementOrphanedSymbolsRemoved() {
            this.orphanedSymbolsRemoved++;
            this.totalRecordsCleaned++;
        }

        public void incrementOrphanedRelationshipsRemoved() {
            this.orphanedRelationshipsRemoved++;
            this.totalRecordsCleaned++;
        }

        public void incrementOrphanedSpringComponentsRemoved() {
            this.orphanedSpringComponentsRemoved++;
            this.totalRecordsCleaned++;
        }

        public void incrementOrphanedRestEndpointsRemoved() {
            this.orphanedRestEndpointsRemoved++;
            this.totalRecordsCleaned++;
        }

        public void incrementStaleDependenciesRemoved() {
            this.staleDependenciesRemoved++;
            this.totalRecordsCleaned++;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("deletedFileRecords", deletedFileRecords);
            map.put("orphanedSymbolsRemoved", orphanedSymbolsRemoved);
            map.put("orphanedRelationshipsRemoved", orphanedRelationshipsRemoved);
            map.put("orphanedSpringComponentsRemoved", orphanedSpringComponentsRemoved);
            map.put("orphanedRestEndpointsRemoved", orphanedRestEndpointsRemoved);
            map.put("staleDependenciesRemoved", staleDependenciesRemoved);
            map.put("totalRecordsCleaned", totalRecordsCleaned);
            return map;
        }
    }
}