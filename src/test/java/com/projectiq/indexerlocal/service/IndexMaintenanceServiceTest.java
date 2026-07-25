package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IndexMaintenanceService.
 */
@ExtendWith(MockitoExtension.class)
class IndexMaintenanceServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private IndexRepository indexRepository;

    @Mock
    private FileHashService fileHashService;

    @Mock
    private DependencyRepository dependencyRepository;

    @Mock
    private RepositoryStatisticsService repositoryStatisticsService;

    private IndexMaintenanceService indexMaintenanceService;

    @BeforeEach
    void setUp() {
        indexMaintenanceService = new IndexMaintenanceService(
                jdbcTemplate, repositoryRepository, indexRepository,
                fileHashService, dependencyRepository, repositoryStatisticsService);
    }

    @Test
    void testGenerateHealthReport_RepositoryNotFound() {
        when(repositoryRepository.findByRepositoryId("nonexistent")).thenReturn(null);

        HealthReport report = indexMaintenanceService.generateHealthReport("nonexistent");

        assertNotNull(report);
        assertEquals("nonexistent", report.getRepositoryId());
        assertFalse(report.isIndexConsistent());
        assertTrue(report.getInconsistencies().stream()
                .anyMatch(i -> i.contains("Repository not found")));
    }

    @Test
    void testGenerateHealthReport_Success() {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("test-repo");
        repo.setRepositoryName("Test Repository");
        repo.setStatus(RepositoryStatus.INDEXED);
        repo.setLastIndexingTimestamp(LocalDateTime.now());
        repo.setWorkspacePath("/tmp/test-repo");

        when(repositoryRepository.findByRepositoryId("test-repo")).thenReturn(repo);
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(0L);
        when(dependencyRepository.getCountByRepositoryId("test-repo")).thenReturn(0);

        HealthReport report = indexMaintenanceService.generateHealthReport("test-repo");

        assertNotNull(report);
        assertEquals("test-repo", report.getRepositoryId());
        assertEquals("Test Repository", report.getRepositoryName());
        assertNotNull(report.getHealthStatistics());
    }

    @Test
    void testExecuteCleanup_RepositoryNotFound() {
        when(repositoryRepository.findByRepositoryId("nonexistent")).thenReturn(null);

        HealthReport.CleanupSummary summary = indexMaintenanceService.executeCleanup("nonexistent");

        assertNotNull(summary);
        assertEquals(0, summary.getTotalRecordsCleaned());
    }

    @Test
    void testExecuteCleanup_Success() {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("test-repo");
        repo.setWorkspacePath("/tmp/test-repo");

        when(repositoryRepository.findByRepositoryId("test-repo")).thenReturn(repo);
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForList(anyString())).thenReturn(new ArrayList<>());

        HealthReport.CleanupSummary summary = indexMaintenanceService.executeCleanup("test-repo");

        assertNotNull(summary);
        assertEquals(0, summary.getTotalRecordsCleaned());
    }

    @Test
    void testExecuteRepositoryMaintenance_RepositoryNotFound() {
        when(repositoryRepository.findByRepositoryId("nonexistent")).thenReturn(null);

        HealthReport report = indexMaintenanceService.executeRepositoryMaintenance("nonexistent");

        assertNotNull(report);
        assertEquals("nonexistent", report.getRepositoryId());
        assertFalse(report.isIndexConsistent());
    }

    @Test
    void testExecuteRepositoryMaintenance_Success() {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("test-repo");
        repo.setRepositoryName("Test Repository");
        repo.setStatus(RepositoryStatus.INDEXED);
        repo.setLastIndexingTimestamp(LocalDateTime.now());
        repo.setWorkspacePath("/tmp/test-repo");

        when(repositoryRepository.findByRepositoryId("test-repo")).thenReturn(repo);
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForList(anyString())).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(0L);
        when(dependencyRepository.getCountByRepositoryId("test-repo")).thenReturn(0);

        HealthReport report = indexMaintenanceService.executeRepositoryMaintenance("test-repo");

        assertNotNull(report);
        assertEquals("test-repo", report.getRepositoryId());
        assertEquals("Test Repository", report.getRepositoryName());
        assertNotNull(report.getHealthStatistics());
    }

    @Test
    void testVerifyIndex_RepositoryNotFound() {
        when(repositoryRepository.findByRepositoryId("nonexistent")).thenReturn(null);

        HealthReport report = indexMaintenanceService.verifyIndex("nonexistent");

        assertNotNull(report);
        assertEquals("nonexistent", report.getRepositoryId());
        assertFalse(report.isIndexConsistent());
    }

    @Test
    void testVerifyIndex_Success() {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("test-repo");
        repo.setRepositoryName("Test Repository");
        repo.setStatus(RepositoryStatus.INDEXED);
        repo.setWorkspacePath("/tmp/test-repo");

        when(repositoryRepository.findByRepositoryId("test-repo")).thenReturn(repo);
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(fileHashService.getStoredHashes("test-repo")).thenReturn(new ArrayList<>());

        HealthReport report = indexMaintenanceService.verifyIndex("test-repo");

        assertNotNull(report);
        assertEquals("test-repo", report.getRepositoryId());
        assertTrue(report.isIndexConsistent());
    }

    @Test
    void testExecuteFullMaintenance_Success() {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("test-repo");
        repo.setRepositoryName("Test Repository");
        repo.setStatus(RepositoryStatus.INDEXED);
        repo.setLastIndexingTimestamp(LocalDateTime.now());
        repo.setWorkspacePath("/tmp/test-repo");

        when(repositoryRepository.findAll()).thenReturn(List.of(repo));
        when(repositoryRepository.findByRepositoryId("test-repo")).thenReturn(repo);
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForList(anyString())).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(dependencyRepository.getCountByRepositoryId("test-repo")).thenReturn(0);

        HealthReport report = indexMaintenanceService.executeFullMaintenance();

        assertNotNull(report);
        assertEquals("ALL", report.getRepositoryId());
        assertNotNull(report.getHealthStatistics());
    }

    @Test
    void testGetRepositoryHealthStatistics() {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("test-repo");
        repo.setRepositoryName("Test Repository");
        repo.setLastIndexingTimestamp(LocalDateTime.now());

        when(repositoryRepository.findByRepositoryId("test-repo")).thenReturn(repo);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(10L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(5L);
        when(dependencyRepository.getCountByRepositoryId("test-repo")).thenReturn(3);

        RepositoryHealthStatistics stats = indexMaintenanceService.getRepositoryHealthStatistics("test-repo");

        assertNotNull(stats);
        assertEquals("test-repo", stats.getRepositoryId());
        assertEquals("Test Repository", stats.getRepositoryName());
        assertTrue(stats.getHealthScore() >= 0.0);
        assertTrue(stats.getHealthScore() <= 100.0);
        assertNotNull(stats.getHealthStatus());
    }

    @Test
    void testGetOverallHealthStatistics() {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("test-repo");
        repo.setRepositoryName("Test Repository");

        when(repositoryRepository.findAll()).thenReturn(List.of(repo));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);
        when(dependencyRepository.getCountByRepositoryId("test-repo")).thenReturn(3);

        RepositoryHealthStatistics stats = indexMaintenanceService.getOverallHealthStatistics();

        assertNotNull(stats);
        assertEquals("ALL", stats.getRepositoryId());
        assertEquals(1, stats.getIndexedRepositories());
        assertTrue(stats.getHealthScore() >= 0.0);
        assertNotNull(stats.getHealthStatus());
    }

    @Test
    void testGenerateOverallHealthReport() {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("test-repo");
        repo.setRepositoryName("Test Repository");
        repo.setWorkspacePath("/tmp/test-repo");

        when(repositoryRepository.findAll()).thenReturn(List.of(repo));
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(dependencyRepository.getCountByRepositoryId("test-repo")).thenReturn(0);

        HealthReport report = indexMaintenanceService.generateOverallHealthReport();

        assertNotNull(report);
        assertEquals("ALL", report.getRepositoryId());
        assertNotNull(report.getHealthStatistics());
    }

    @Test
    void testHealthReportCleanupSummary() {
        HealthReport.CleanupSummary summary = new HealthReport.CleanupSummary();

        assertEquals(0, summary.getTotalRecordsCleaned());

        summary.incrementDeletedFileRecords();
        assertEquals(1, summary.getDeletedFileRecords());
        assertEquals(1, summary.getTotalRecordsCleaned());

        summary.incrementOrphanedSymbolsRemoved();
        assertEquals(1, summary.getOrphanedSymbolsRemoved());
        assertEquals(2, summary.getTotalRecordsCleaned());

        summary.incrementOrphanedRelationshipsRemoved();
        assertEquals(1, summary.getOrphanedRelationshipsRemoved());
        assertEquals(3, summary.getTotalRecordsCleaned());

        summary.incrementOrphanedSpringComponentsRemoved();
        assertEquals(1, summary.getOrphanedSpringComponentsRemoved());
        assertEquals(4, summary.getTotalRecordsCleaned());

        summary.incrementOrphanedRestEndpointsRemoved();
        assertEquals(1, summary.getOrphanedRestEndpointsRemoved());
        assertEquals(5, summary.getTotalRecordsCleaned());

        summary.incrementStaleDependenciesRemoved();
        assertEquals(1, summary.getStaleDependenciesRemoved());
        assertEquals(6, summary.getTotalRecordsCleaned());

        Map<String, Object> map = summary.toMap();
        assertNotNull(map);
        assertEquals(6L, map.get("totalRecordsCleaned"));
    }

    @Test
    void testRepositoryHealthStatisticsToMap() {
        RepositoryHealthStatistics stats = new RepositoryHealthStatistics();
        stats.setRepositoryId("test-repo");
        stats.setRepositoryName("Test Repository");
        stats.setHealthScore(95.0);
        stats.setHealthStatus("HEALTHY");

        Map<String, Object> map = stats.toMap();
        assertNotNull(map);
        assertEquals("test-repo", map.get("repositoryId"));
        assertEquals("Test Repository", map.get("repositoryName"));
        assertEquals(95.0, map.get("healthScore"));
        assertEquals("HEALTHY", map.get("healthStatus"));
    }

    @Test
    void testHealthReportToMap() {
        HealthReport report = new HealthReport();
        report.setRepositoryId("test-repo");
        report.setRepositoryName("Test Repository");
        report.setIndexConsistent(true);
        report.addRecommendation("Test recommendation");

        Map<String, Object> map = report.toMap();
        assertNotNull(map);
        assertEquals("test-repo", map.get("repositoryId"));
        assertEquals("Test Repository", map.get("repositoryName"));
        assertTrue((Boolean) map.get("indexConsistent"));
        assertNotNull(map.get("recommendations"));
    }

    @Test
    void testHealthScoreCalculation() {
        // Test with recent indexing - should have high score
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("test-repo");
        repo.setRepositoryName("Test Repository");
        repo.setLastIndexingTimestamp(LocalDateTime.now());

        when(repositoryRepository.findByRepositoryId("test-repo")).thenReturn(repo);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(100L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(50L);
        when(dependencyRepository.getCountByRepositoryId("test-repo")).thenReturn(10);

        RepositoryHealthStatistics stats = indexMaintenanceService.getRepositoryHealthStatistics("test-repo");

        assertNotNull(stats);
        assertTrue(stats.getHealthScore() > 0);
    }

    @Test
    void testHealthReportWithInconsistencies() {
        HealthReport report = new HealthReport();
        report.setRepositoryId("test-repo");

        assertTrue(report.isIndexConsistent());

        report.addInconsistency("Test inconsistency");
        assertFalse(report.isIndexConsistent());
        assertEquals(1, report.getInconsistencies().size());
    }
}