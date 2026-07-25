package com.projectiq.indexerlocal.service.impl;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.repositorysearch.RepositorySearchResult;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import com.projectiq.indexerlocal.service.RepositorySearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of RepositorySearchService that queries the indexed SQLite database
 * for fast, deterministic repository resource lookups without filesystem scanning.
 */
@Service
public class RepositorySearchServiceImpl implements RepositorySearchService {

    private static final Logger logger = LoggerFactory.getLogger(RepositorySearchServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final RepositoryRepository repositoryRepository;

    public RepositorySearchServiceImpl(JdbcTemplate jdbcTemplate,
                                       RepositoryRepository repositoryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.repositoryRepository = repositoryRepository;
    }

    @Override
    public PaginatedResponse<RepositorySearchResult> findFiles(
            String repositoryId, String fileName, String extension, String module,
            String matchMode, Long minSize, Long maxSize,
            int page, int size) {
        initFileSchema();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM file_metadata WHERE 1=1");
        List<Object> params = new ArrayList<>();

        applyCommonFileFilters(sql, params, repositoryId, module);
        applyFileSearchFilter(sql, params, "name", fileName, matchMode);
        applyFileSearchFilter(sql, params, "extension", extension, matchMode);

        if (minSize != null) {
            sql.append(" AND file_size >= ?");
            params.add(minSize);
        }
        if (maxSize != null) {
            sql.append(" AND file_size <= ?");
            params.add(maxSize);
        }

        return executeFileSearchQuery(sql.toString(), params, page, size, "FILE");
    }

    @Override
    public PaginatedResponse<RepositorySearchResult> findFolders(
            String repositoryId, String folderName, String classification,
            String matchMode,
            int page, int size) {
        initDirectorySchema();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM directory_metadata WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        applyFileSearchFilter(sql, params, "name", folderName, matchMode);
        if (classification != null && !classification.isEmpty()) {
            sql.append(" AND classification = ?");
            params.add(classification.toUpperCase());
        }

        return executeDirectorySearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<RepositorySearchResult> findExtensions(
            String repositoryId, String extension, String language,
            String matchMode,
            int page, int size) {
        // Query distinct extensions from file_metadata
        initFileSchema();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT extension FROM file_metadata WHERE extension IS NOT NULL AND extension != ''");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        applyFileSearchFilter(sql, params, "extension", extension, matchMode);

        // Use a separate query for paging distinct extensions
        try {
            // Count distinct extensions
            String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ")";
            Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            long totalElements = total != null ? total : 0L;

            // Paginate distinct extensions
            String querySql = sql.toString() + " ORDER BY extension ASC LIMIT ? OFFSET ?";
            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(size);
            queryParams.add(page * size);

            List<String> extensions = jdbcTemplate.query(querySql, (rs, rowNum) -> rs.getString("extension"), queryParams.toArray());

            // Build results
            List<RepositorySearchResult> results = extensions.stream()
                    .map(ext -> {
                        RepositorySearchResult result = new RepositorySearchResult();
                        result.setExtension(ext);
                        result.setResourceType("EXTENSION");
                        result.setName(ext);
                        // Look up language from file classification
                        result.setLanguage(mapExtensionToLanguage(ext));
                        // If repository ID is specified, include repository info
                        if (repositoryId != null && !repositoryId.isEmpty()) {
                            enrichWithRepositoryInfo(result, repositoryId);
                        }
                        return result;
                    })
                    .collect(Collectors.toList());

            long totalPages = (long) Math.ceil((double) totalElements / size);
            return PaginatedResponse.of(results, page, size, totalPages, totalElements);

        } catch (Exception e) {
            logger.warn("Extension search query failed: {}", e.getMessage());
            return PaginatedResponse.of(List.of(), page, size, 0, 0);
        }
    }

    @Override
    public PaginatedResponse<RepositorySearchResult> findRepositories(
            String repositoryId, String repositoryName, String status,
            String buildSystem, String technologyStack,
            String matchMode,
            int page, int size) {
        repositoryRepository.initSchema();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM repository WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            applyFileSearchFilter(sql, params, "repository_id", repositoryId, matchMode);
        }
        if (repositoryName != null && !repositoryName.isEmpty()) {
            applyFileSearchFilter(sql, params, "repository_name", repositoryName, matchMode);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status.toUpperCase());
        }
        if (buildSystem != null && !buildSystem.isEmpty()) {
            applyFileSearchFilter(sql, params, "build_system", buildSystem, matchMode);
        }
        if (technologyStack != null && !technologyStack.isEmpty()) {
            applyFileSearchFilter(sql, params, "technology_stack", technologyStack, matchMode);
        }

        return executeRepositorySearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<RepositorySearchResult> findLanguages(
            String repositoryId, String language,
            String matchMode,
            int page, int size) {
        // Query distinct languages from technology_stack data or from file classifications
        initFileSchema();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT classification FROM file_metadata WHERE classification IS NOT NULL AND classification != ''");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }

        try {
            // Count
            String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ")";
            Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            long totalElements = total != null ? total : 0L;

            // Query paginated
            String querySql = sql.toString() + " ORDER BY classification ASC LIMIT ? OFFSET ?";
            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(size);
            queryParams.add(page * size);

            List<String> classifications = jdbcTemplate.query(querySql, (rs, rowNum) -> rs.getString("classification"), queryParams.toArray());

            List<RepositorySearchResult> results = classifications.stream()
                    .map(cls -> {
                        RepositorySearchResult result = new RepositorySearchResult();
                        String languageName = mapClassificationToLanguage(cls);
                        result.setLanguage(languageName);
                        result.setName(languageName);
                        result.setExtension(cls);
                        result.setResourceType("LANGUAGE");
                        if (repositoryId != null && !repositoryId.isEmpty()) {
                            enrichWithRepositoryInfo(result, repositoryId);
                        }
                        return result;
                    })
                    .filter(r -> language == null || language.isEmpty() || matchesPattern(r.getLanguage(), language, matchMode))
                    .collect(Collectors.toList());

            // Recalculate pagination after filtering
            long filteredTotal = results.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, (int) filteredTotal);
            List<RepositorySearchResult> paged;
            if (fromIndex < filteredTotal) {
                paged = results.subList(fromIndex, toIndex);
            } else {
                paged = List.of();
            }

            long totalPages = (long) Math.ceil((double) filteredTotal / size);
            return PaginatedResponse.of(paged, page, size, totalPages, filteredTotal);

        } catch (Exception e) {
            logger.warn("Language search query failed: {}", e.getMessage());
            return PaginatedResponse.of(List.of(), page, size, 0, 0);
        }
    }

    @Override
    public PaginatedResponse<RepositorySearchResult> findRootModules(
            String repositoryId, String moduleName,
            String matchMode,
            int page, int size) {
        // Query from file_metadata by depth = 1 directories or classification
        initDirectorySchema();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM directory_metadata WHERE depth = 1");
        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        applyFileSearchFilter(sql, params, "name", moduleName, matchMode);

        return executeDirectorySearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<RepositorySearchResult> findSourceDirectories(
            String repositoryId, String directoryName,
            String matchMode,
            int page, int size) {
        initDirectorySchema();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM directory_metadata WHERE classification = ?");
        List<Object> params = new ArrayList<>();
        params.add("SOURCE");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        applyFileSearchFilter(sql, params, "name", directoryName, matchMode);

        return executeDirectorySearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<RepositorySearchResult> findResourceDirectories(
            String repositoryId, String directoryName,
            String matchMode,
            int page, int size) {
        initDirectorySchema();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM directory_metadata WHERE classification = ?");
        List<Object> params = new ArrayList<>();
        params.add("RESOURCE");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        applyFileSearchFilter(sql, params, "name", directoryName, matchMode);

        return executeDirectorySearchQuery(sql.toString(), params, page, size);
    }

    @Override
    public PaginatedResponse<RepositorySearchResult> findTestDirectories(
            String repositoryId, String directoryName,
            String matchMode,
            int page, int size) {
        initDirectorySchema();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM directory_metadata WHERE classification = ?");
        List<Object> params = new ArrayList<>();
        params.add("TEST");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        applyFileSearchFilter(sql, params, "name", directoryName, matchMode);

        return executeDirectorySearchQuery(sql.toString(), params, page, size);
    }

    // ==================== Private Helper Methods ====================

    private void initFileSchema() {
        try {
            jdbcTemplate.execute("SELECT 1 FROM file_metadata LIMIT 1");
        } catch (Exception e) {
            String sql = "CREATE TABLE IF NOT EXISTS file_metadata (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "repository_id TEXT NOT NULL, " +
                    "path TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "relative_path TEXT, " +
                    "extension TEXT, " +
                    "file_size BIGINT, " +
                    "classification TEXT, " +
                    "is_hidden INTEGER, " +
                    "depth INTEGER, " +
                    "last_modified TIMESTAMP, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "updated_at TIMESTAMP NOT NULL, " +
                    "FOREIGN KEY (repository_id) REFERENCES repository(repository_id))";
            jdbcTemplate.execute(sql);
        }
    }

    private void initDirectorySchema() {
        try {
            jdbcTemplate.execute("SELECT 1 FROM directory_metadata LIMIT 1");
        } catch (Exception e) {
            String sql = "CREATE TABLE IF NOT EXISTS directory_metadata (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "repository_id TEXT NOT NULL, " +
                    "path TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "relative_path TEXT, " +
                    "depth INTEGER, " +
                    "classification TEXT, " +
                    "is_hidden INTEGER, " +
                    "last_modified TIMESTAMP, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "updated_at TIMESTAMP NOT NULL, " +
                    "FOREIGN KEY (repository_id) REFERENCES repository(repository_id))";
            jdbcTemplate.execute(sql);
        }
    }

    private void applyCommonFileFilters(StringBuilder sql, List<Object> params,
                                         String repositoryId, String module) {
        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND repository_id = ?");
            params.add(repositoryId);
        }
        if (module != null && !module.isEmpty()) {
            // Module corresponds to a path prefix pattern
            sql.append(" AND (relative_path LIKE ? ESCAPE '\\' OR path LIKE ? ESCAPE '\\')");
            params.add(escapeLike(module) + "%");
            params.add("%/" + escapeLike(module) + "/%");
        }
    }

    private void applyFileSearchFilter(StringBuilder sql, List<Object> params,
                                        String column, String value, String matchMode) {
        if (value == null || value.isEmpty()) {
            return;
        }

        String mode = (matchMode != null) ? matchMode.toLowerCase() : "partial";

        switch (mode) {
            case "exact":
                sql.append(" AND ").append(column).append(" = ?");
                params.add(value);
                break;
            case "prefix":
                sql.append(" AND ").append(column).append(" LIKE ? ESCAPE '\\'");
                params.add(escapeLike(value) + "%");
                break;
            case "wildcard":
                // Convert SQL wildcards
                String wildcardPattern = value.replace("*", "%").replace("?", "_");
                sql.append(" AND ").append(column).append(" LIKE ? ESCAPE '\\'");
                params.add(wildcardPattern);
                break;
            default: // partial
                sql.append(" AND LOWER(").append(column).append(") LIKE ? ESCAPE '\\'");
                params.add("%" + escapeLike(value.toLowerCase()) + "%");
                break;
        }
    }

    private boolean matchesPattern(String value, String pattern, String matchMode) {
        if (value == null || pattern == null) {
            return true;
        }
        String mode = (matchMode != null) ? matchMode.toLowerCase() : "partial";

        switch (mode) {
            case "exact":
                return value.equalsIgnoreCase(pattern);
            case "prefix":
                return value.toLowerCase().startsWith(pattern.toLowerCase());
            case "wildcard":
                String regex = pattern.replace("*", ".*").replace("?", ".");
                return value.matches("(?i)" + regex);
            default: // partial
                return value.toLowerCase().contains(pattern.toLowerCase());
        }
    }

    private PaginatedResponse<RepositorySearchResult> executeFileSearchQuery(
            String baseSql, List<Object> params, int page, int size, String resourceType) {
        try {
            // Count total
            String countSql = "SELECT COUNT(*) FROM (" + baseSql + ")";
            Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            long totalElements = total != null ? total : 0L;

            // Apply pagination
            String querySql = baseSql + " ORDER BY repository_id ASC, path ASC LIMIT ? OFFSET ?";
            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(size);
            queryParams.add(page * size);

            List<RepositorySearchResult> results = jdbcTemplate.query(querySql, (rs, rowNum) -> {
                RepositorySearchResult result = new RepositorySearchResult();
                result.setRepositoryId(rs.getString("repository_id"));
                result.setAbsolutePath(rs.getString("path"));
                result.setRelativePath(rs.getString("relative_path"));
                result.setName(rs.getString("name"));
                result.setExtension(rs.getString("extension"));
                result.setResourceType(resourceType);

                // File size
                long fileSize = rs.getLong("file_size");
                if (!rs.wasNull()) {
                    result.setFileSize(fileSize);
                    // Classify language based on extension
                    String ext = rs.getString("extension");
                    if (ext != null) {
                        result.setLanguage(mapExtensionToLanguage(ext));
                    }
                }

                // Classification
                String classification = rs.getString("classification");
                if (classification != null) {
                    result.setFileClassification(classification);
                }

                // Hidden
                result.setHidden(rs.getInt("is_hidden") == 1);
                result.setDepth(rs.getInt("depth"));

                // Last modified
                Timestamp ts = rs.getTimestamp("last_modified");
                if (ts != null) {
                    result.setLastModified(ts.toLocalDateTime());
                }

                // Enrich with repository name and indexed timestamp
                enrichWithRepositoryInfo(result, result.getRepositoryId());

                return result;
            }, queryParams.toArray());

            long totalPages = (long) Math.ceil((double) totalElements / size);
            return PaginatedResponse.of(results, page, size, totalPages, totalElements);

        } catch (Exception e) {
            logger.warn("File search query failed: {}", e.getMessage());
            return PaginatedResponse.of(List.of(), page, size, 0, 0);
        }
    }

    private PaginatedResponse<RepositorySearchResult> executeDirectorySearchQuery(
            String baseSql, List<Object> params, int page, int size) {
        try {
            // Count total
            String countSql = "SELECT COUNT(*) FROM (" + baseSql + ")";
            Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            long totalElements = total != null ? total : 0L;

            // Apply pagination
            String querySql = baseSql + " ORDER BY repository_id ASC, path ASC LIMIT ? OFFSET ?";
            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(size);
            queryParams.add(page * size);

            List<RepositorySearchResult> results = jdbcTemplate.query(querySql, (rs, rowNum) -> {
                RepositorySearchResult result = new RepositorySearchResult();
                result.setRepositoryId(rs.getString("repository_id"));
                result.setAbsolutePath(rs.getString("path"));
                result.setRelativePath(rs.getString("relative_path"));
                result.setName(rs.getString("name"));
                result.setResourceType("FOLDER");

                // Classification
                String classification = rs.getString("classification");
                if (classification != null) {
                    result.setDirectoryClassification(classification);
                }

                // Hidden
                result.setHidden(rs.getInt("is_hidden") == 1);
                result.setDepth(rs.getInt("depth"));

                // Enrich with repository info
                enrichWithRepositoryInfo(result, result.getRepositoryId());

                return result;
            }, queryParams.toArray());

            long totalPages = (long) Math.ceil((double) totalElements / size);
            return PaginatedResponse.of(results, page, size, totalPages, totalElements);

        } catch (Exception e) {
            logger.warn("Directory search query failed: {}", e.getMessage());
            return PaginatedResponse.of(List.of(), page, size, 0, 0);
        }
    }

    private PaginatedResponse<RepositorySearchResult> executeRepositorySearchQuery(
            String baseSql, List<Object> params, int page, int size) {
        try {
            // Count total
            String countSql = "SELECT COUNT(*) FROM (" + baseSql + ")";
            Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            long totalElements = total != null ? total : 0L;

            // Apply pagination
            String querySql = baseSql + " ORDER BY repository_name ASC LIMIT ? OFFSET ?";
            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(size);
            queryParams.add(page * size);

            List<RepositorySearchResult> results = jdbcTemplate.query(querySql, (rs, rowNum) -> {
                RepositorySearchResult result = new RepositorySearchResult();
                result.setRepositoryId(rs.getString("repository_id"));
                result.setRepositoryName(rs.getString("repository_name"));
                result.setAbsolutePath(rs.getString("original_path"));
                result.setName(rs.getString("repository_name"));
                result.setResourceType("REPOSITORY");

                // Build system
                result.setModule(rs.getString("build_system"));

                // Status
                String status = rs.getString("status");
                result.setRepositoryStatus(status);

                // Extensions and languages from file_metadata
                String repoId = rs.getString("repository_id");
                if (repoId != null) {
                    enrichWithRepositoryInfo(result, repoId);
                    enrichWithLanguagesAndExtensions(result, repoId);
                }

                return result;
            }, queryParams.toArray());

            long totalPages = (long) Math.ceil((double) totalElements / size);
            return PaginatedResponse.of(results, page, size, totalPages, totalElements);

        } catch (Exception e) {
            logger.warn("Repository search query failed: {}", e.getMessage());
            return PaginatedResponse.of(List.of(), page, size, 0, 0);
        }
    }

    private void enrichWithRepositoryInfo(RepositorySearchResult result, String repoId) {
        if (repoId == null) return;
        try {
            com.projectiq.indexerlocal.model.Repository repo = repositoryRepository.findByRepositoryId(repoId);
            if (repo != null) {
                result.setRepositoryName(repo.getRepositoryName());
                if (result.getAbsolutePath() == null) {
                    result.setAbsolutePath(repo.getOriginalPath());
                }
                result.setIndexedTimestamp(repo.getLastIndexingTimestamp());
                result.setRepositoryStatus(repo.getStatus() != null ? repo.getStatus().name() : null);
            }
        } catch (Exception e) {
            logger.debug("Could not enrich repository info for {}: {}", repoId, e.getMessage());
        }
    }

    private void enrichWithLanguagesAndExtensions(RepositorySearchResult result, String repoId) {
        try {
            // Get distinct extensions
            String extSql = "SELECT DISTINCT extension FROM file_metadata WHERE repository_id = ? AND extension IS NOT NULL AND extension != ''";
            List<String> extensions = jdbcTemplate.query(extSql, (rs, rowNum) -> rs.getString("extension"), repoId);
            result.setExtensions(extensions);

            // Get distinct classifications as languages
            String langSql = "SELECT DISTINCT classification FROM file_metadata WHERE repository_id = ? AND classification IS NOT NULL AND classification != ''";
            List<String> classifications = jdbcTemplate.query(langSql, (rs, rowNum) -> rs.getString("classification"), repoId);
            List<String> languages = classifications.stream()
                    .map(this::mapClassificationToLanguage)
                    .distinct()
                    .collect(Collectors.toList());
            result.setLanguages(languages);
        } catch (Exception e) {
            logger.debug("Could not enrich languages/extensions for {}: {}", repoId, e.getMessage());
        }
    }

    private String mapExtensionToLanguage(String extension) {
        if (extension == null) return null;
        switch (extension.toLowerCase()) {
            case "java": return "Java";
            case "kt": case "kts": return "Kotlin";
            case "groovy": case "gvy": return "Groovy";
            case "xml": return "XML";
            case "yaml": case "yml": return "YAML";
            case "properties": return "Properties";
            case "json": return "JSON";
            case "sql": return "SQL";
            case "md": return "Markdown";
            case "html": case "htm": return "HTML";
            case "js": return "JavaScript";
            case "ts": return "TypeScript";
            case "css": return "CSS";
            case "sh": case "bash": return "Shell";
            case "gradle": return "Gradle";
            case "pom": return "Maven";
            default: return extension.toUpperCase();
        }
    }

    private String mapClassificationToLanguage(String classification) {
        if (classification == null) return "Unknown";
        switch (classification.toUpperCase()) {
            case "JAVA_SOURCE": return "Java";
            case "KOTLIN": return "Kotlin";
            case "GROOVY": return "Groovy";
            case "XML": return "XML";
            case "YAML": return "YAML";
            case "PROPERTIES": return "Properties";
            case "JSON": return "JSON";
            case "SQL": return "SQL";
            case "MARKDOWN": return "Markdown";
            case "HTML": return "HTML";
            case "JAVASCRIPT": return "JavaScript";
            case "TYPESCRIPT": return "TypeScript";
            case "CSS": return "CSS";
            case "SHELL_SCRIPT": return "Shell Script";
            case "BUILD_FILE": return "Build Configuration";
            default: return classification;
        }
    }

    private String escapeLike(String value) {
        if (value == null) return "";
        return value.replace("%", "\\%").replace("_", "\\_");
    }
}