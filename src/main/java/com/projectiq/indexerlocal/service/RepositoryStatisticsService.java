package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for generating repository statistics and summary.
 * Aggregates data from all persisted analyzer outputs only.
 */
@Service
public class RepositoryStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryStatisticsService.class);

    private final JdbcTemplate jdbcTemplate;
    private final RepositoryRepository repositoryRepository;

    public RepositoryStatisticsService(RepositoryRepository repositoryRepository, JdbcTemplate jdbcTemplate) {
        this.repositoryRepository = repositoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get repository summary with all aggregated statistics.
     */
    @Transactional
    public RepositorySummary getRepositorySummary(String repositoryId) {
        long startTime = System.currentTimeMillis();
        logger.info("Generating repository summary for repositoryId={}", repositoryId);

        // Verify repository exists and is indexed
        verifyRepositoryExistsAndIndexed(repositoryId);

        RepositorySummary summary = new RepositorySummary();
        summary.setRepositoryId(repositoryId);

        // Get repository information
        List<Repository> repositories = jdbcTemplate.query(
            "SELECT * FROM repository WHERE repository_id = ?", 
            (rs, rowNum) -> {
                Repository repo = new Repository();
                repo.setRepositoryId(rs.getString("repository_id"));
                repo.setRepositoryName(rs.getString("repository_name"));
                repo.setStatus(rs.getString("status") != null ? RepositoryStatus.valueOf(rs.getString("status")) : null);
                repo.setRegistrationTimestamp(rs.getTimestamp("registration_date") != null ? 
                    rs.getTimestamp("registration_date").toLocalDateTime() : null);
                repo.setLastUpdatedTimestamp(rs.getTimestamp("last_updated") != null ? 
                    rs.getTimestamp("last_updated").toLocalDateTime() : null);
                return repo;
            },
            repositoryId);

        if (!repositories.isEmpty()) {
            Repository repo = repositories.get(0);
            summary.setRepositoryName(repo.getRepositoryName());
            summary.setRegistrationDate(repo.getRegistrationTimestamp());
            summary.setLastIndexedDate(repo.getLastUpdatedTimestamp());
            summary.setCurrentStatus(repo.getStatus() != null ? repo.getStatus().name() : "UNKNOWN");
        }

        // Code statistics (from FileIndex)
        Map<String, Object> codeStats = aggregateCodeStatistics(repositoryId);
        summary.setTotalJavaFiles((Long) codeStats.getOrDefault("totalJavaFiles", 0L));
        summary.setTotalPackages((Long) codeStats.getOrDefault("totalPackages", 0L));
        summary.setTotalClasses((Long) codeStats.getOrDefault("totalClasses", 0L));
        summary.setTotalInterfaces((Long) codeStats.getOrDefault("totalInterfaces", 0L));
        summary.setTotalEnums((Long) codeStats.getOrDefault("totalEnums", 0L));
        summary.setTotalRecords((Long) codeStats.getOrDefault("totalRecords", 0L));
        summary.setTotalMethods((Long) codeStats.getOrDefault("totalMethods", 0L));
        summary.setTotalConstructors((Long) codeStats.getOrDefault("totalConstructors", 0L));
        summary.setTotalFields((Long) codeStats.getOrDefault("totalFields", 0L));
        summary.setTotalAnnotations((Long) codeStats.getOrDefault("totalAnnotations", 0L));

        // Spring statistics (from spring_component table)
        Map<String, Object> springStats = aggregateSpringStatistics(repositoryId);
        summary.setTotalComponents((Long) springStats.getOrDefault("totalComponents", 0L));
        summary.setTotalServices((Long) springStats.getOrDefault("totalServices", 0L));
        summary.setTotalControllers((Long) springStats.getOrDefault("totalControllers", 0L));
        summary.setTotalRestControllers((Long) springStats.getOrDefault("totalRestControllers", 0L));
        summary.setTotalRepositories((Long) springStats.getOrDefault("totalRepositories", 0L));
        summary.setTotalConfigurationClasses((Long) springStats.getOrDefault("totalConfigurationClasses", 0L));
        summary.setTotalBeans((Long) springStats.getOrDefault("totalBeans", 0L));

        // REST API statistics (from rest_api_endpoint table)
        Map<String, Object> restApiStats = aggregateRestApiStatistics(repositoryId);
        summary.setTotalRestApiEndpoints((Long) restApiStats.getOrDefault("totalEndpoints", 0L));
        summary.setTotalGetEndpoints((Long) restApiStats.getOrDefault("totalGet", 0L));
        summary.setTotalPostEndpoints((Long) restApiStats.getOrDefault("totalPost", 0L));
        summary.setTotalPutEndpoints((Long) restApiStats.getOrDefault("totalPut", 0L));
        summary.setTotalDeleteEndpoints((Long) restApiStats.getOrDefault("totalDelete", 0L));
        summary.setTotalPatchEndpoints((Long) restApiStats.getOrDefault("totalPatch", 0L));

        // Dependency statistics (from dependency table)
        Map<String, Object> depStats = aggregateDependencyStatistics(repositoryId);
        summary.setTotalDependencies((Long) depStats.getOrDefault("totalDependencies", 0L));
        summary.setCompileDependencies((Long) depStats.getOrDefault("compileDependencies", 0L));
        summary.setRuntimeDependencies((Long) depStats.getOrDefault("runtimeDependencies", 0L));
        summary.setTestDependencies((Long) depStats.getOrDefault("testDependencies", 0L));

        // Configuration statistics (from configuration_file table)
        Map<String, Object> configStats = aggregateConfigurationStatistics(repositoryId);
        summary.setTotalConfigurationFiles((Long) configStats.getOrDefault("totalConfigFiles", 0L));
        summary.setSpringConfigurations((Long) configStats.getOrDefault("springConfigs", 0L));
        summary.setDockerConfigurations((Long) configStats.getOrDefault("dockerConfigs", 0L));
        summary.setKubernetesConfigurations((Long) configStats.getOrDefault("k8sConfigs", 0L));
        summary.setCiCdConfigurations((Long) configStats.getOrDefault("ciCdConfigs", 0L));

        // Database statistics (from database_artifact table)
        Map<String, Object> dbStats = aggregateDatabaseStatistics(repositoryId);
        summary.setDatabasesDetected((Long) dbStats.getOrDefault("databasesDetected", 0L));
        summary.setDatasources((Long) dbStats.getOrDefault("datasources", 0L));
        summary.setSqlFiles((Long) dbStats.getOrDefault("sqlFiles", 0L));
        summary.setMigrationScripts((Long) dbStats.getOrDefault("migrationScripts", 0L));

        // Project summary information
        summary.setBuildSystem(getBuildSystem(repositoryId));
        summary.setProjectType(getProjectType(repositoryId));
        summary.setTechnologyStack(getTechnologyStack(repositoryId));
        summary.setDetectedDatabases(getDetectedDatabases(repositoryId));
        summary.setSpringFrameworkUsage(getSpringFrameworkUsage(repositoryId));

        long endTime = System.currentTimeMillis();
        logger.info("Repository summary generated for repositoryId={} in {}ms", repositoryId, (endTime - startTime));

        return summary;
    }

    /**
     * Get consolidated repository statistics.
     */
    @Transactional
    public Map<String, Object> getRepositoryStatistics(String repositoryId) {
        long startTime = System.currentTimeMillis();
        logger.info("Generating repository statistics for repositoryId={}", repositoryId);

        // Verify repository exists and is indexed
        verifyRepositoryExistsAndIndexed(repositoryId);

        Map<String, Object> statistics = new HashMap<>();

        // Repository Information
        Map<String, Object> repoInfo = getRepositoryInformation(repositoryId);
        statistics.put("repository_information", repoInfo);

        // Project Summary
        Map<String, Object> projectSummary = new HashMap<>();
        projectSummary.put("build_system", getBuildSystem(repositoryId));
        projectSummary.put("project_type", getProjectType(repositoryId));
        projectSummary.put("technology_stack", getTechnologyStack(repositoryId));
        projectSummary.put("detected_databases", getDetectedDatabases(repositoryId));
        projectSummary.put("spring_framework_usage", getSpringFrameworkUsage(repositoryId));
        statistics.put("project_summary", projectSummary);

        // Code Statistics
        Map<String, Object> codeStats = aggregateCodeStatistics(repositoryId);
        statistics.put("code_statistics", codeStats);

        // Spring Statistics
        Map<String, Object> springStats = aggregateSpringStatistics(repositoryId);
        statistics.put("spring_statistics", springStats);

        // REST API Statistics
        Map<String, Object> restApiStats = aggregateRestApiStatistics(repositoryId);
        statistics.put("rest_api_statistics", restApiStats);

        // Dependency Statistics
        Map<String, Object> depStats = aggregateDependencyStatistics(repositoryId);
        statistics.put("dependency_statistics", depStats);

        // Configuration Statistics
        Map<String, Object> configStats = aggregateConfigurationStatistics(repositoryId);
        statistics.put("configuration_statistics", configStats);

        // Database Statistics
        Map<String, Object> dbStats = aggregateDatabaseStatistics(repositoryId);
        statistics.put("database_statistics", dbStats);

        long endTime = System.currentTimeMillis();
        logger.info("Repository statistics generated for repositoryId={} in {}ms", repositoryId, (endTime - startTime));

        return statistics;
    }

    private void verifyRepositoryExistsAndIndexed(String repositoryId) {
        Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo == null) {
            throw new IllegalArgumentException("Repository not found: " + repositoryId);
        }
        if (repo.getStatus() != RepositoryStatus.INDEXED) {
            throw new IllegalStateException("Repository is not indexed. Current status: " + 
                (repo.getStatus() != null ? repo.getStatus().name() : "NULL"));
        }
    }

    private Map<String, Object> getRepositoryInformation(String repositoryId) {
        Map<String, Object> info = new HashMap<>();
        Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo != null) {
            info.put("repository_id", repo.getRepositoryId());
            info.put("repository_name", repo.getRepositoryName());
            info.put("registration_date", repo.getRegistrationTimestamp());
            info.put("last_indexed_date", repo.getLastUpdatedTimestamp());
            info.put("current_status", repo.getStatus() != null ? repo.getStatus().name() : "UNKNOWN");
        }
        return info;
    }

    private String getBuildSystem(String repositoryId) {
        try {
            String result = jdbcTemplate.queryForObject(
                "SELECT build_system FROM build_metadata WHERE repository_id = ?", 
                String.class, repositoryId);
            return result != null ? result : "UNKNOWN";
        } catch (Exception e) {
            logger.warn("Error getting build system for repositoryId={}: {}", repositoryId, e.getMessage());
            return "UNKNOWN";
        }
    }

    private String getProjectType(String repositoryId) {
        try {
            String result = jdbcTemplate.queryForObject(
                "SELECT build_system FROM build_metadata WHERE repository_id = ?", 
                String.class, repositoryId);
            return result != null ? result : "UNKNOWN";
        } catch (Exception e) {
            logger.warn("Error getting project type for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return "UNKNOWN";
    }

    private String getTechnologyStack(String repositoryId) {
        try {
            String result = jdbcTemplate.queryForObject(
                "SELECT GROUP_CONCAT(technology_name SEPARATOR ', ') FROM technology_stack WHERE repository_id = ?", 
                String.class, repositoryId);
            return result != null ? result : "UNKNOWN";
        } catch (Exception e) {
            logger.warn("Error getting technology stack for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return "UNKNOWN";
    }

    private String getDetectedDatabases(String repositoryId) {
        try {
            String result = jdbcTemplate.queryForObject(
                "SELECT GROUP_CONCAT(DISTINCT database_type SEPARATOR ', ') FROM database_artifact WHERE repository_id = ?", 
                String.class, repositoryId);
            return result != null ? result : "NONE";
        } catch (Exception e) {
            logger.warn("Error getting detected databases for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return "NONE";
    }

    private String getSpringFrameworkUsage(String repositoryId) {
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spring_component WHERE repository_id = ?", 
                Long.class, repositoryId);
            return count != null && count > 0 ? "YES" : "NO";
        } catch (Exception e) {
            logger.warn("Error getting Spring framework usage for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return "NO";
    }

    // ==================== Aggregation helper methods ====================

    private Map<String, Object> aggregateCodeStatistics(String repositoryId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalPackages", queryLong("SELECT COUNT(DISTINCT package_name) FROM file_index WHERE repository_id = ?", repositoryId));
            stats.put("totalJavaFiles", queryLong("SELECT COUNT(*) FROM file_index WHERE repository_id = ? AND file_type = 'JAVA'", repositoryId));
            stats.put("totalClasses", queryLong("SELECT COUNT(*) FROM class_info WHERE repository_id = ? AND class_type = 'CLASS'", repositoryId));
            stats.put("totalInterfaces", queryLong("SELECT COUNT(*) FROM class_info WHERE repository_id = ? AND class_type = 'INTERFACE'", repositoryId));
            stats.put("totalEnums", queryLong("SELECT COUNT(*) FROM class_info WHERE repository_id = ? AND class_type = 'ENUM'", repositoryId));
            stats.put("totalRecords", queryLong("SELECT COUNT(*) FROM class_info WHERE repository_id = ? AND class_type = 'RECORD'", repositoryId));
            stats.put("totalMethods", queryLong("SELECT COUNT(*) FROM method_info WHERE repository_id = ?", repositoryId));
            stats.put("totalConstructors", queryLong("SELECT COUNT(*) FROM method_info WHERE repository_id = ? AND is_constructor = 1", repositoryId));
            stats.put("totalFields", queryLong("SELECT COUNT(*) FROM field_info WHERE repository_id = ?", repositoryId));
            stats.put("totalAnnotations", queryLong("SELECT COUNT(DISTINCT annotation_name) FROM annotation_info WHERE repository_id = ?", repositoryId));
        } catch (Exception e) {
            logger.warn("Error aggregating code statistics for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return stats;
    }

    private Map<String, Object> aggregateSpringStatistics(String repositoryId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalComponents", queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'COMPONENT'", repositoryId));
            stats.put("totalServices", queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'SERVICE'", repositoryId));
            stats.put("totalControllers", queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'CONTROLLER'", repositoryId));
            stats.put("totalRestControllers", queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'REST_CONTROLLER'", repositoryId));
            stats.put("totalRepositories", queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'REPOSITORY'", repositoryId));
            stats.put("totalConfigurationClasses", queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'CONFIGURATION'", repositoryId));
            stats.put("totalBeans", queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'BEAN'", repositoryId));
        } catch (Exception e) {
            logger.warn("Error aggregating Spring statistics for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return stats;
    }

    private Map<String, Object> aggregateRestApiStatistics(String repositoryId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalControllers", queryLong("SELECT COUNT(DISTINCT controller_name) FROM rest_api_endpoint WHERE repository_id = ?", repositoryId));
            stats.put("totalEndpoints", queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ?", repositoryId));
            stats.put("totalGet", queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'GET'", repositoryId));
            stats.put("totalPost", queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'POST'", repositoryId));
            stats.put("totalPut", queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'PUT'", repositoryId));
            stats.put("totalDelete", queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'DELETE'", repositoryId));
            stats.put("totalPatch", queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'PATCH'", repositoryId));
        } catch (Exception e) {
            logger.warn("Error aggregating REST API statistics for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return stats;
    }

    private Map<String, Object> aggregateDependencyStatistics(String repositoryId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalDependencies", queryLong("SELECT COUNT(*) FROM dependency WHERE repository_id = ?", repositoryId));
            stats.put("compileDependencies", queryLong("SELECT COUNT(*) FROM dependency WHERE repository_id = ? AND dependency_type = 'COMPILE'", repositoryId));
            stats.put("runtimeDependencies", queryLong("SELECT COUNT(*) FROM dependency WHERE repository_id = ? AND dependency_type = 'RUNTIME'", repositoryId));
            stats.put("testDependencies", queryLong("SELECT COUNT(*) FROM dependency WHERE repository_id = ? AND dependency_type = 'TEST'", repositoryId));
        } catch (Exception e) {
            logger.warn("Error aggregating dependency statistics for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return stats;
    }

    private Map<String, Object> aggregateConfigurationStatistics(String repositoryId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalConfigFiles", queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ?", repositoryId));
            stats.put("springConfigs", queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'SPRING'", repositoryId));
            stats.put("dockerConfigs", queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'DOCKER'", repositoryId));
            stats.put("k8sConfigs", queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'KUBERNETES'", repositoryId));
            stats.put("ciCdConfigs", queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'CI_CD'", repositoryId));
        } catch (Exception e) {
            logger.warn("Error aggregating configuration statistics for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return stats;
    }

    private Map<String, Object> aggregateDatabaseStatistics(String repositoryId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("databasesDetected", queryLong("SELECT COUNT(DISTINCT database_type) FROM database_artifact WHERE repository_id = ?", repositoryId));
            stats.put("datasources", queryLong("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'DATASOURCE'", repositoryId));
            stats.put("sqlFiles", queryLong("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'SQL_FILE'", repositoryId));
            stats.put("migrationScripts", queryLong("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'MIGRATION'", repositoryId));
        } catch (Exception e) {
            logger.warn("Error aggregating database statistics for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return stats;
    }

    private Long queryLong(String sql, String repositoryId) {
        try {
            Number result = jdbcTemplate.queryForObject(sql, Long.class, repositoryId);
            return result != null ? result.longValue() : 0L;
        } catch (Exception e) {
            logger.debug("Query failed for '{}': {}", sql.substring(0, Math.min(50, sql.length())), e.getMessage());
            return 0L;
        }
    }
}