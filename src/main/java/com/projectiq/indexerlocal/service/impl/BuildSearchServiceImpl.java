package com.projectiq.indexerlocal.service.impl;

import com.projectiq.indexerlocal.model.BuildMetadata;
import com.projectiq.indexerlocal.model.BuildSystemType;
import com.projectiq.indexerlocal.model.Dependency;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.buildsearch.BuildSearchResult;
import com.projectiq.indexerlocal.repository.DependencyRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import com.projectiq.indexerlocal.service.BuildSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of BuildSearchService that queries the indexed SQLite database
 * for fast, deterministic build metadata lookups without filesystem scanning.
 */
@Service
public class BuildSearchServiceImpl implements BuildSearchService {

    private static final Logger logger = LoggerFactory.getLogger(BuildSearchServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final RepositoryRepository repositoryRepository;
    private final DependencyRepository dependencyRepository;

    public BuildSearchServiceImpl(JdbcTemplate jdbcTemplate,
                                  RepositoryRepository repositoryRepository,
                                  DependencyRepository dependencyRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.repositoryRepository = repositoryRepository;
        this.dependencyRepository = dependencyRepository;
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findMavenProjects(
            String repositoryId, String groupId, String artifactId, String version,
            int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE build_system_type = ?");
        List<Object> params = new ArrayList<>();
        params.add("MAVEN");

        applyCommonFilters(sql, params, repositoryId, groupId, artifactId, version);

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findGradleProjects(
            String repositoryId, String projectName, String group,
            int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE build_system_type = ?");
        List<Object> params = new ArrayList<>();
        params.add("GRADLE");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (projectName != null && !projectName.isEmpty()) {
            sql.append(" AND project_name LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLike(projectName) + "%");
        }
        if (group != null && !group.isEmpty()) {
            sql.append(" AND gradle_group LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLike(group) + "%");
        }

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findBuildFiles(
            String repositoryId, String buildSystem, String buildFileName,
            int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (buildSystem != null && !buildSystem.isEmpty()) {
            sql.append(" AND build_system_type = ?");
            params.add(buildSystem.toUpperCase());
        }
        if (buildFileName != null && !buildFileName.isEmpty()) {
            sql.append(" AND build_file_name LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLike(buildFileName) + "%");
        }

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findModules(
            String repositoryId, String moduleName, String parentModule,
            int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (moduleName != null && !moduleName.isEmpty()) {
            sql.append(" AND (modules LIKE ? ESCAPE '\\' OR project_name LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(moduleName) + "%");
            params.add("%" + escapeLike(moduleName) + "%");
        }
        if (parentModule != null && !parentModule.isEmpty()) {
            sql.append(" AND parent_artifact_id LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLike(parentModule) + "%");
        }

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findParentProjects(
            String repositoryId, String parentGroupId, String parentArtifactId,
            int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE parent_group_id IS NOT NULL AND parent_group_id != ''");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (parentGroupId != null && !parentGroupId.isEmpty()) {
            sql.append(" AND parent_group_id LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLike(parentGroupId) + "%");
        }
        if (parentArtifactId != null && !parentArtifactId.isEmpty()) {
            sql.append(" AND parent_artifact_id LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLike(parentArtifactId) + "%");
        }

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findChildModules(
            String repositoryId, String parentModule,
            int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE project_type = ?");
        List<Object> params = new ArrayList<>();
        params.add("Multi Module");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (parentModule != null && !parentModule.isEmpty()) {
            sql.append(" AND (project_artifact_id LIKE ? ESCAPE '\\' OR project_name LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(parentModule) + "%");
            params.add("%" + escapeLike(parentModule) + "%");
        }

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findPlugins(
            String repositoryId, String pluginName, String moduleName,
            int page, int size) {
        // Plugins are stored in additional_metadata as JSON; search via LIKE on the full metadata
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (pluginName != null && !pluginName.isEmpty()) {
            // Search in spring_boot_version field as proxy for plugin info
            // Also search in modules field for multi-module projects
            sql.append(" AND (spring_boot_version LIKE ? ESCAPE '\\' OR modules LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(pluginName) + "%");
            params.add("%" + escapeLike(pluginName) + "%");
        }
        if (moduleName != null && !moduleName.isEmpty()) {
            sql.append(" AND (modules LIKE ? ESCAPE '\\' OR project_name LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(moduleName) + "%");
            params.add("%" + escapeLike(moduleName) + "%");
        }

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findDependencies(
            String repositoryId, String groupId, String artifactId, String version,
            String moduleName,
            int page, int size) {
        // Query from dependency_repository (in-memory) and build_metadata
        List<BuildSearchResult> allResults = new ArrayList<>();

        // Get all matching dependencies from the in-memory repository
        List<Dependency> matchingDeps;
        if (repositoryId != null && !repositoryId.isEmpty()) {
            matchingDeps = dependencyRepository.findByRepositoryId(repositoryId);
        } else {
            // Get all repositories and collect dependencies
            matchingDeps = new ArrayList<>();
            var repos = repositoryRepository.findAll();
            for (var repo : repos) {
                matchingDeps.addAll(dependencyRepository.findByRepositoryId(repo.getRepositoryId()));
            }
        }

        // Apply filters
        List<Dependency> filtered = matchingDeps.stream()
                .filter(d -> groupId == null || groupId.isEmpty() || d.getGroupId().toLowerCase().contains(groupId.toLowerCase()))
                .filter(d -> artifactId == null || artifactId.isEmpty() || d.getArtifactId().toLowerCase().contains(artifactId.toLowerCase()))
                .filter(d -> version == null || version.isEmpty() || (d.getVersion() != null && d.getVersion().toLowerCase().contains(version.toLowerCase())))
                .collect(Collectors.toList());

        // Group by repository and create results
        var byRepo = filtered.stream().collect(Collectors.groupingBy(Dependency::getRepositoryId));
        for (var entry : byRepo.entrySet()) {
            BuildSearchResult result = new BuildSearchResult();
            result.setRepositoryId(entry.getKey());
            result.setDependencies(entry.getValue().stream()
                    .map(d -> d.getGroupId() + ":" + d.getArtifactId() + ":" + (d.getVersion() != null ? d.getVersion() : ""))
                    .collect(Collectors.toList()));
            result.setBuildSystem("MAVEN"); // Default assumption
            allResults.add(result);
        }

        // Apply pagination
        long totalElements = allResults.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, (int) totalElements);
        List<BuildSearchResult> paged;
        if (fromIndex < totalElements) {
            paged = allResults.subList(fromIndex, toIndex);
        } else {
            paged = List.of();
        }

        long totalPages = (long) Math.ceil((double) totalElements / size);
        return PaginatedResponse.of(paged, page, size, totalPages, totalElements);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findBuildProfiles(
            String repositoryId, String profileName, String moduleName,
            int page, int size) {
        // Build profiles are stored in additional_metadata; search via build_metadata
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (profileName != null && !profileName.isEmpty()) {
            sql.append(" AND (project_type LIKE ? ESCAPE '\\' OR build_file_name LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(profileName) + "%");
            params.add("%" + escapeLike(profileName) + "%");
        }
        if (moduleName != null && !moduleName.isEmpty()) {
            sql.append(" AND (modules LIKE ? ESCAPE '\\' OR project_name LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(moduleName) + "%");
            params.add("%" + escapeLike(moduleName) + "%");
        }

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> findBuildConfigurations(
            String repositoryId, String buildSystem, String packaging,
            String moduleName,
            int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (buildSystem != null && !buildSystem.isEmpty()) {
            sql.append(" AND build_system_type = ?");
            params.add(buildSystem.toUpperCase());
        }
        if (packaging != null && !packaging.isEmpty()) {
            sql.append(" AND packaging LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLike(packaging) + "%");
        }
        if (moduleName != null && !moduleName.isEmpty()) {
            sql.append(" AND (modules LIKE ? ESCAPE '\\' OR project_name LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(moduleName) + "%");
            params.add("%" + escapeLike(moduleName) + "%");
        }

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<BuildSearchResult> searchBuild(
            String repositoryId, String buildSystem, String moduleName,
            String groupId, String artifactId, String version,
            String plugin, String dependency, String profile,
            int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM build_metadata WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (buildSystem != null && !buildSystem.isEmpty()) {
            sql.append(" AND build_system_type = ?");
            params.add(buildSystem.toUpperCase());
        }
        if (moduleName != null && !moduleName.isEmpty()) {
            sql.append(" AND (modules LIKE ? ESCAPE '\\' OR project_name LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(moduleName) + "%");
            params.add("%" + escapeLike(moduleName) + "%");
        }
        if (groupId != null && !groupId.isEmpty()) {
            sql.append(" AND (project_group_id LIKE ? ESCAPE '\\' OR parent_group_id LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(groupId) + "%");
            params.add("%" + escapeLike(groupId) + "%");
        }
        if (artifactId != null && !artifactId.isEmpty()) {
            sql.append(" AND (project_artifact_id LIKE ? ESCAPE '\\' OR parent_artifact_id LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(artifactId) + "%");
            params.add("%" + escapeLike(artifactId) + "%");
        }
        if (version != null && !version.isEmpty()) {
            sql.append(" AND (project_version LIKE ? ESCAPE '\\' OR parent_version LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(version) + "%");
            params.add("%" + escapeLike(version) + "%");
        }
        if (plugin != null && !plugin.isEmpty()) {
            sql.append(" AND (spring_boot_version LIKE ? ESCAPE '\\' OR modules LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(plugin) + "%");
            params.add("%" + escapeLike(plugin) + "%");
        }
        if (profile != null && !profile.isEmpty()) {
            sql.append(" AND (project_type LIKE ? ESCAPE '\\' OR build_file_name LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(profile) + "%");
            params.add("%" + escapeLike(profile) + "%");
        }

        return executeBuildSearchQuery(sql.toString(), params, page, size);
    }

    // ==================== Private Helper Methods ====================

    private void applyCommonFilters(StringBuilder sql, List<Object> params,
                                     String repositoryId, String groupId,
                                     String artifactId, String version) {
        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (groupId != null && !groupId.isEmpty()) {
            sql.append(" AND (project_group_id LIKE ? ESCAPE '\\' OR parent_group_id LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(groupId) + "%");
            params.add("%" + escapeLike(groupId) + "%");
        }
        if (artifactId != null && !artifactId.isEmpty()) {
            sql.append(" AND (project_artifact_id LIKE ? ESCAPE '\\' OR parent_artifact_id LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(artifactId) + "%");
            params.add("%" + escapeLike(artifactId) + "%");
        }
        if (version != null && !version.isEmpty()) {
            sql.append(" AND (project_version LIKE ? ESCAPE '\\' OR parent_version LIKE ? ESCAPE '\\')");
            params.add("%" + escapeLike(version) + "%");
            params.add("%" + escapeLike(version) + "%");
        }
    }

    private PaginatedResponse<BuildSearchResult> executeBuildSearchQuery(
            String baseSql, List<Object> params, int page, int size) {
        try {
            // Ensure build_metadata table exists
            initBuildMetadataSchema();

            // Count total
            String countSql = "SELECT COUNT(*) FROM (" + baseSql + ")";
            Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            long totalElements = total != null ? total : 0L;

            // Apply pagination
            String querySql = baseSql + " ORDER BY repository_id ASC LIMIT ? OFFSET ?";
            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(size);
            queryParams.add(page * size);

            List<BuildSearchResult> results = jdbcTemplate.query(querySql, (rs, rowNum) -> {
                BuildSearchResult result = new BuildSearchResult();
                result.setRepositoryId(rs.getString("repository_id"));
                result.setBuildSystem(rs.getString("build_system_type"));
                result.setBuildFilePath(rs.getString("build_file_name"));
                result.setGroupId(rs.getString("project_group_id"));
                result.setArtifactId(rs.getString("project_artifact_id"));
                result.setVersion(rs.getString("project_version"));
                result.setPackaging(rs.getString("packaging"));
                result.setProjectType(rs.getString("project_type"));

                // Module info
                String modulesStr = rs.getString("modules");
                if (modulesStr != null && !modulesStr.isEmpty()) {
                    String[] mods = modulesStr.split(",");
                    if (mods.length > 0) {
                        result.setModuleName(mods[0]);
                    }
                }

                // Parent info
                String parentArtifactId = rs.getString("parent_artifact_id");
                if (parentArtifactId != null && !parentArtifactId.isEmpty()) {
                    result.setParentModule(parentArtifactId);
                }

                // Java version
                result.setJavaVersion(rs.getString("java_version"));

                // Spring Boot version
                result.setSpringBootVersion(rs.getString("spring_boot_version"));

                // Build profiles (from project_type and build_file_name)
                List<String> profiles = new ArrayList<>();
                String projectType = rs.getString("project_type");
                if (projectType != null && !projectType.isEmpty()) {
                    profiles.add(projectType);
                }
                result.setProfiles(profiles);

                // Plugins (from spring_boot_version as proxy)
                List<String> plugins = new ArrayList<>();
                String sbVersion = rs.getString("spring_boot_version");
                if (sbVersion != null && !sbVersion.isEmpty()) {
                    plugins.add("spring-boot:" + sbVersion);
                }
                result.setPlugins(plugins);

                // Dependencies from in-memory repository
                String repoId = rs.getString("repository_id");
                if (repoId != null) {
                    List<Dependency> deps = dependencyRepository.findByRepositoryId(repoId);
                    result.setDependencies(deps.stream()
                            .map(d -> d.getGroupId() + ":" + d.getArtifactId() + ":" + (d.getVersion() != null ? d.getVersion() : ""))
                            .collect(Collectors.toList()));
                }

                return result;
            }, queryParams.toArray());

            long totalPages = (long) Math.ceil((double) totalElements / size);
            return PaginatedResponse.of(results, page, size, totalPages, totalElements);

        } catch (Exception e) {
            logger.warn("Build search query failed: {}", e.getMessage());
            return PaginatedResponse.of(List.of(), page, size, 0, 0);
        }
    }

    private void initBuildMetadataSchema() {
        try {
            jdbcTemplate.execute("SELECT 1 FROM build_metadata LIMIT 1");
        } catch (Exception e) {
            // Table doesn't exist, create it
            String sql = "CREATE TABLE IF NOT EXISTS build_metadata (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "repository_id TEXT UNIQUE NOT NULL, " +
                    "build_system_type TEXT, " +
                    "build_file_name TEXT, " +
                    "project_group_id TEXT, " +
                    "project_artifact_id TEXT, " +
                    "project_version TEXT, " +
                    "packaging TEXT, " +
                    "parent_group_id TEXT, " +
                    "parent_artifact_id TEXT, " +
                    "parent_version TEXT, " +
                    "modules TEXT, " +
                    "project_name TEXT, " +
                    "gradle_group TEXT, " +
                    "java_version TEXT, " +
                    "maven_wrapper_present INTEGER, " +
                    "gradle_wrapper_present INTEGER, " +
                    "project_type TEXT, " +
                    "child_modules TEXT, " +
                    "spring_boot_version TEXT, " +
                    "analyzed_at TIMESTAMP, " +
                    "FOREIGN KEY (repository_id) REFERENCES repository(repository_id))";
            jdbcTemplate.execute(sql);
        }
    }

    private String escapeLike(String value) {
        if (value == null) return "";
        return value.replace("%", "\\%").replace("_", "\\_");
    }
}