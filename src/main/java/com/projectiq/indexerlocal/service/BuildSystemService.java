package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.analyzer.BuildSystemAnalyzer;
import com.projectiq.indexerlocal.analyzer.BuildSystemAnalyzer.WrapperInfo;
import com.projectiq.indexerlocal.model.BuildMetadata;
import com.projectiq.indexerlocal.model.BuildSystemType;
import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.repository.ProjectStructureRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for analyzing and managing build system information.
 */
@Service
public class BuildSystemService {

    private static final Logger logger = LoggerFactory.getLogger(BuildSystemService.class);

    private final BuildSystemAnalyzer buildSystemAnalyzer;
    private final RepositoryRepository repositoryRepository;
    private final ProjectStructureRepository projectStructureRepository;
    private final JdbcTemplate jdbcTemplate;

    public BuildSystemService(BuildSystemAnalyzer buildSystemAnalyzer,
                              RepositoryRepository repositoryRepository,
                              ProjectStructureRepository projectStructureRepository,
                              JdbcTemplate jdbcTemplate) {
        this.buildSystemAnalyzer = buildSystemAnalyzer;
        this.repositoryRepository = repositoryRepository;
        this.projectStructureRepository = projectStructureRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Analyze the build system of a repository.
     */
    public BuildMetadata analyzeBuild(String repositoryId) {
        // Validate repository exists
        Repository repository = validateRepositoryExists(repositoryId);

        // Check if project structure has been analyzed
        long fileCount = projectStructureRepository.countFilesByRepositoryId(repositoryId);
        if (fileCount == 0) {
            throw new IllegalStateException("Project structure must be analyzed before analyzing build system for repository: " + repositoryId);
        }

        logger.info("Starting build analysis for repository: {}", repositoryId);
        long startTime = System.currentTimeMillis();

        Path workspacePath = Path.of(repository.getWorkspacePath());
        
        // Perform build analysis
        BuildMetadata metadata = buildSystemAnalyzer.analyzeBuildSystem(workspacePath);
        metadata.setRepositoryId(repositoryId);

        // Persist build system information
        persistBuildMetadata(repositoryId, metadata);

        // Update repository's build system field
        String buildSystemName = metadata.getBuildSystemType() != null ? metadata.getBuildSystemType().name() : "Unknown";
        repository.setBuildSystem(buildSystemName);
        repository.setLastUpdatedTimestamp(LocalDateTime.now());
        repositoryRepository.save(repository);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Build analysis completed for repository: {}. Build system: {}, Duration: {}ms",
                repositoryId, metadata.getBuildSystemType(), duration);

        return metadata;
    }

    /**
     * Get build system information for a repository.
     */
    public BuildMetadata getBuild(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return getBuildMetadataFromStorage(repositoryId);
    }

    /**
     * Get modules for a repository.
     */
    public List<BuildMetadata.ModuleInfo> getModules(String repositoryId) {
        validateRepositoryExists(repositoryId);
        BuildMetadata metadata = getBuildMetadataFromStorage(repositoryId);
        
        if (metadata == null || metadata.getChildModules() == null) {
            return new ArrayList<>();
        }

        return metadata.getChildModules();
    }

    /**
     * Detect the build system type without extracting full metadata.
     */
    public String detectBuildSystemType(String repositoryId) {
        Repository repository = validateRepositoryExists(repositoryId);
        Path workspacePath = Path.of(repository.getWorkspacePath());
        
        BuildSystemType type = buildSystemAnalyzer.detectBuildSystem(workspacePath);
        return type != null ? type.name() : "Unknown";
    }

    /**
     * Check if Maven Wrapper is present.
     */
    public boolean isMavenWrapperPresent(String repositoryId) {
        Repository repository = validateRepositoryExists(repositoryId);
        Path workspacePath = Path.of(repository.getWorkspacePath());
        
        WrapperInfo wrapperInfo = buildSystemAnalyzer.detectWrappers(workspacePath);
        return wrapperInfo.isMavenWrapperPresent();
    }

    /**
     * Check if Gradle Wrapper is present.
     */
    public boolean isGradleWrapperPresent(String repositoryId) {
        Repository repository = validateRepositoryExists(repositoryId);
        Path workspacePath = Path.of(repository.getWorkspacePath());
        
        WrapperInfo wrapperInfo = buildSystemAnalyzer.detectWrappers(workspacePath);
        return wrapperInfo.isGradleWrapperPresent();
    }

    // ==================== Private Methods ====================

    private Repository validateRepositoryExists(String repositoryId) {
        Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with ID '" + repositoryId + "' not found");
        }
        return repository;
    }

    public void initBuildMetadataSchema() {
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

    private void persistBuildMetadata(String repositoryId, BuildMetadata metadata) {
        initBuildMetadataSchema();
        
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT OR REPLACE INTO build_metadata (");
            sql.append("repository_id, build_system_type, build_file_name, project_group_id, ");
            sql.append("project_artifact_id, project_version, packaging, parent_group_id, ");
            sql.append("parent_artifact_id, parent_version, modules, project_name, gradle_group, ");
            sql.append("java_version, maven_wrapper_present, gradle_wrapper_present, project_type, ");
            sql.append("child_modules, spring_boot_version, analyzed_at) ");
            sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            String modulesStr = metadata.getModules() != null && !metadata.getModules().isEmpty()
                    ? String.join(",", metadata.getModules()) : "";

            String childModulesJson = "[]";
            if (metadata.getChildModules() != null && !metadata.getChildModules().isEmpty()) {
                childModulesJson = metadata.getChildModules().stream()
                        .map(m -> String.format("{\"name\":\"%s\",\"path\":\"%s\"}", m.getName(), m.getPath()))
                        .collect(Collectors.joining(",", "[", "]"));
            }

            jdbcTemplate.update(sql.toString(),
                    repositoryId,
                    metadata.getBuildSystemType() != null ? metadata.getBuildSystemType().name() : "UNKNOWN",
                    metadata.getBuildFileName() != null ? metadata.getBuildFileName() : "",
                    metadata.getGroupId() != null ? metadata.getGroupId() : "",
                    metadata.getArtifactId() != null ? metadata.getArtifactId() : "",
                    metadata.getVersion() != null ? metadata.getVersion() : "",
                    metadata.getPackaging() != null ? metadata.getPackaging() : "jar",
                    metadata.getParentGroupId() != null ? metadata.getParentGroupId() : "",
                    metadata.getParentArtifactId() != null ? metadata.getParentArtifactId() : "",
                    metadata.getParentVersion() != null ? metadata.getParentVersion() : "",
                    modulesStr,
                    metadata.getProjectName() != null ? metadata.getProjectName() : "",
                    metadata.getGradleGroup() != null ? metadata.getGradleGroup() : "",
                    metadata.getJavaVersion() != null ? metadata.getJavaVersion() : "",
                    metadata.isMavenWrapperPresent() ? 1 : 0,
                    metadata.isGradleWrapperPresent() ? 1 : 0,
                    metadata.getProjectType() != null ? metadata.getProjectType() : "Single Module",
                    childModulesJson,
                    metadata.getSpringBootVersion() != null ? metadata.getSpringBootVersion() : "",
                    metadata.getAnalyzedAt() != null ? java.sql.Timestamp.valueOf(metadata.getAnalyzedAt())
                            : java.sql.Timestamp.valueOf(LocalDateTime.now())
            );

            logger.info("Build metadata persisted for repository: {}", repositoryId);
        } catch (Exception e) {
            logger.error("Failed to persist build metadata for repository: {}", repositoryId, e);
        }
    }

    private BuildMetadata getBuildMetadataFromStorage(String repositoryId) {
        initBuildMetadataSchema();
        
        try {
            // Query from database
            String sql = "SELECT * FROM build_metadata WHERE repository_id = ?";
            List<BuildMetadata> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                BuildMetadata metadata = new BuildMetadata();
                
                if (rs.getObject("repository_id") != null) {
                    metadata.setRepositoryId(rs.getString("repository_id"));
                }
                
                String buildSystemType = rs.getString("build_system_type");
                if (buildSystemType != null && !buildSystemType.equals("UNKNOWN")) {
                    metadata.setBuildSystemType(BuildSystemType.valueOf(buildSystemType));
                }
                
                metadata.setBuildFileName(rs.getString("build_file_name"));
                metadata.setGroupId(rs.getString("project_group_id"));
                metadata.setArtifactId(rs.getString("project_artifact_id"));
                metadata.setVersion(rs.getString("project_version"));
                metadata.setPackaging(rs.getString("packaging"));
                metadata.setParentGroupId(rs.getString("parent_group_id"));
                metadata.setParentArtifactId(rs.getString("parent_artifact_id"));
                metadata.setParentVersion(rs.getString("parent_version"));
                
                String modulesStr = rs.getString("modules");
                if (modulesStr != null && !modulesStr.isEmpty()) {
                    metadata.setModules(new ArrayList<>(List.of(modulesStr.split(","))));
                }
                
                metadata.setProjectName(rs.getString("project_name"));
                metadata.setGradleGroup(rs.getString("gradle_group"));
                metadata.setJavaVersion(rs.getString("java_version"));
                metadata.setMavenWrapperPresent(rs.getInt("maven_wrapper_present") == 1);
                metadata.setGradleWrapperPresent(rs.getInt("gradle_wrapper_present") == 1);
                metadata.setProjectType(rs.getString("project_type"));
                
                String childModulesJson = rs.getString("child_modules");
                if (childModulesJson != null && !childModulesJson.isEmpty() && !"[]".equals(childModulesJson)) {
                    List<BuildMetadata.ModuleInfo> modules = new ArrayList<>();
                    metadata.setChildModules(modules);
                }
                
                metadata.setSpringBootVersion(rs.getString("spring_boot_version"));
                
                java.sql.Timestamp analyzedAt = rs.getTimestamp("analyzed_at");
                if (analyzedAt != null) {
                    metadata.setAnalyzedAt(analyzedAt.toLocalDateTime());
                }
                
                return metadata;
            }, repositoryId);
            
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.warn("No build metadata found for repository: {}", repositoryId);
            return null;
        }
    }
}