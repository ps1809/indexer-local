package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.RepositoryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access layer for persisting repository metadata into SQLite.
 */
@Component
public class RepositoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final IndexRepository indexRepository;

    public RepositoryRepository(JdbcTemplate jdbcTemplate, IndexRepository indexRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.indexRepository = indexRepository;
    }

    /**
     * Initialize the repositories table schema.
     */
    public void initSchema() {
        String sqlCreateRepository = 
            "CREATE TABLE IF NOT EXISTS repository (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "repository_id TEXT UNIQUE NOT NULL, " +
            "repository_name TEXT NOT NULL, " +
            "original_path TEXT NOT NULL, " +
            "workspace_path TEXT, " +
            "registration_timestamp TIMESTAMP NOT NULL, " +
            "last_updated_timestamp TIMESTAMP NOT NULL, " +
            "last_refresh_timestamp TIMESTAMP, " +
            "last_indexing_timestamp TIMESTAMP, " +
            "status TEXT NOT NULL, " +
            "build_system TEXT, " +
            "technology_stack TEXT)";

        jdbcTemplate.execute(sqlCreateRepository);
    }

    /**
     * Row mapper for Repository entity.
     */
    private final org.springframework.jdbc.core.RowMapper<com.projectiq.indexerlocal.model.Repository> rowMapper = (rs, rowNum) -> {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setId(rs.getLong("id"));
        repo.setRepositoryId(rs.getString("repository_id"));
        repo.setRepositoryName(rs.getString("repository_name"));
        repo.setOriginalPath(rs.getString("original_path"));
        repo.setWorkspacePath(rs.getString("workspace_path"));
        repo.setRegistrationTimestamp(rs.getTimestamp("registration_timestamp") != null 
            ? rs.getTimestamp("registration_timestamp").toLocalDateTime() : null);
        repo.setLastUpdatedTimestamp(rs.getTimestamp("last_updated_timestamp") != null 
            ? rs.getTimestamp("last_updated_timestamp").toLocalDateTime() : null);
        repo.setLastRefreshTimestamp(rs.getTimestamp("last_refresh_timestamp") != null 
            ? rs.getTimestamp("last_refresh_timestamp").toLocalDateTime() : null);
        repo.setLastIndexingTimestamp(rs.getTimestamp("last_indexing_timestamp") != null 
            ? rs.getTimestamp("last_indexing_timestamp").toLocalDateTime() : null);
        repo.setStatus(RepositoryStatus.valueOf(rs.getString("status")));
        repo.setBuildSystem(rs.getString("build_system"));
        repo.setTechnologyStack(rs.getString("technology_stack"));
        return repo;
    };

    /**
     * Insert a new repository record.
     */
    public Long save(com.projectiq.indexerlocal.model.Repository repository) {
        initSchema();
        String sql = "INSERT INTO repository (repository_id, repository_name, original_path, workspace_path, " +
                     "registration_timestamp, last_updated_timestamp, last_refresh_timestamp, last_indexing_timestamp, " +
                     "status, build_system, technology_stack) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, repository.getRepositoryId());
            ps.setString(2, repository.getRepositoryName());
            ps.setString(3, repository.getOriginalPath());
            ps.setString(4, repository.getWorkspacePath());
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(repository.getRegistrationTimestamp()));
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(repository.getLastUpdatedTimestamp()));
            ps.setTimestamp(7, repository.getLastRefreshTimestamp() != null 
                ? java.sql.Timestamp.valueOf(repository.getLastRefreshTimestamp()) : null);
            ps.setTimestamp(8, repository.getLastIndexingTimestamp() != null 
                ? java.sql.Timestamp.valueOf(repository.getLastIndexingTimestamp()) : null);
            ps.setString(9, repository.getStatus().name());
            ps.setString(10, repository.getBuildSystem());
            ps.setString(11, repository.getTechnologyStack());
            return ps;
        }, keyHolder);
        
        Long id = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
        repository.setId(id);
        return id;
    }

    /**
     * Update an existing repository record.
     */
    public void update(com.projectiq.indexerlocal.model.Repository repository) {
        initSchema();
        String sql = "UPDATE repository SET repository_name = ?, original_path = ?, workspace_path = ?, " +
                     "last_updated_timestamp = ?, last_refresh_timestamp = ?, last_indexing_timestamp = ?, " +
                     "status = ?, build_system = ?, technology_stack = ? WHERE id = ?";
        
        jdbcTemplate.update(sql,
            repository.getRepositoryName(),
            repository.getOriginalPath(),
            repository.getWorkspacePath(),
            java.sql.Timestamp.valueOf(repository.getLastUpdatedTimestamp()),
            repository.getLastRefreshTimestamp() != null 
                ? java.sql.Timestamp.valueOf(repository.getLastRefreshTimestamp()) : null,
            repository.getLastIndexingTimestamp() != null 
                ? java.sql.Timestamp.valueOf(repository.getLastIndexingTimestamp()) : null,
            repository.getStatus().name(),
            repository.getBuildSystem(),
            repository.getTechnologyStack(),
            repository.getId()
        );
    }

    /**
     * Find all registered repositories.
     */
    public List<com.projectiq.indexerlocal.model.Repository> findAll() {
        initSchema();
        String sql = "SELECT id, repository_id, repository_name, original_path, workspace_path, " +
                     "registration_timestamp, last_updated_timestamp, last_refresh_timestamp, last_indexing_timestamp, " +
                     "status, build_system, technology_stack " +
                     "FROM repository ORDER BY id";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Find a repository by its ID.
     */
    public com.projectiq.indexerlocal.model.Repository findById(Long id) {
        initSchema();
        String sql = "SELECT id, repository_id, repository_name, original_path, workspace_path, " +
                     "registration_timestamp, last_updated_timestamp, last_refresh_timestamp, last_indexing_timestamp, " +
                     "status, build_system, technology_stack " +
                     "FROM repository WHERE id = ?";
        List<com.projectiq.indexerlocal.model.Repository> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a repository by its unique repository ID.
     */
    public com.projectiq.indexerlocal.model.Repository findByRepositoryId(String repositoryId) {
        initSchema();
        String sql = "SELECT id, repository_id, repository_name, original_path, workspace_path, " +
                     "registration_timestamp, last_updated_timestamp, last_refresh_timestamp, last_indexing_timestamp, " +
                     "status, build_system, technology_stack " +
                     "FROM repository WHERE repository_id = ?";
        List<com.projectiq.indexerlocal.model.Repository> results = jdbcTemplate.query(sql, rowMapper, repositoryId);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a repository by its original path.
     */
    public com.projectiq.indexerlocal.model.Repository findByOriginalPath(String originalPath) {
        initSchema();
        String sql = "SELECT id, repository_id, repository_name, original_path, workspace_path, " +
                     "registration_timestamp, last_updated_timestamp, last_refresh_timestamp, last_indexing_timestamp, " +
                     "status, build_system, technology_stack " +
                     "FROM repository WHERE original_path = ?";
        List<com.projectiq.indexerlocal.model.Repository> results = jdbcTemplate.query(sql, rowMapper, originalPath);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Check if a repository with the given ID exists.
     */
    public boolean existsById(Long id) {
        initSchema();
        String sql = "SELECT COUNT(*) FROM repository WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    /**
     * Check if a repository with the given repository ID exists.
     */
    public boolean existsByRepositoryId(String repositoryId) {
        initSchema();
        String sql = "SELECT COUNT(*) FROM repository WHERE repository_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, repositoryId);
        return count != null && count > 0;
    }

    /**
     * Check if a repository with the given original path exists.
     */
    public boolean existsByOriginalPath(String originalPath) {
        initSchema();
        String sql = "SELECT COUNT(*) FROM repository WHERE original_path = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, originalPath);
        return count != null && count > 0;
    }

    /**
     * Delete a repository by its ID.
     */
    public void deleteById(Long id) {
        initSchema();
        String sql = "DELETE FROM repository WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Update the status of a repository.
     */
    public void updateStatus(Long id, RepositoryStatus status) {
        initSchema();
        String sql = "UPDATE repository SET status = ?, last_updated_timestamp = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), 
            java.sql.Timestamp.valueOf(LocalDateTime.now()), id);
    }

    /**
     * Find repositories with pagination and optional status filter.
     */
    public List<com.projectiq.indexerlocal.model.Repository> findAllWithPagination(int page, int size, String sortBy, String sortOrder, RepositoryStatus status) {
        initSchema();
        StringBuilder sql = new StringBuilder(
            "SELECT id, repository_id, repository_name, original_path, workspace_path, " +
            "registration_timestamp, last_updated_timestamp, last_refresh_timestamp, last_indexing_timestamp, " +
            "status, build_system, technology_stack " +
            "FROM repository WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        // Add status filter if provided
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }

        // Add sorting
        String direction = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
        switch (sortBy.toLowerCase()) {
            case "name":
                sql.append(" ORDER BY repository_name ").append(direction);
                break;
            case "status":
                sql.append(" ORDER BY status ").append(direction);
                break;
            case "registration_date":
                sql.append(" ORDER BY registration_timestamp ").append(direction);
                break;
            case "last_refresh":
                sql.append(" ORDER BY last_refresh_timestamp ").append(direction);
                break;
            case "last_indexing":
                sql.append(" ORDER BY last_indexing_timestamp ").append(direction);
                break;
            default:
                sql.append(" ORDER BY id ").append(direction);
                break;
        }

        // Add pagination with LIMIT/OFFSET
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    /**
     * Count repositories by status.
     */
    public Long countByStatus(RepositoryStatus status) {
        initSchema();
        String sql = "SELECT COUNT(*) FROM repository WHERE status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count != null ? count : 0L;
    }

    /**
     * Count all repositories.
     */
    public Long countAll() {
        initSchema();
        String sql = "SELECT COUNT(*) FROM repository";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * Delete all indexing data associated with a repository.
     * This removes all indexed metadata from all index tables.
     */
    public void deleteAllDataByRepositoryId(String repositoryId) {
        // Get repository info before deleting
        com.projectiq.indexerlocal.model.Repository repo = findByRepositoryId(repositoryId);
        
        if (repo == null) {
            logger.warn("Repository not found for deletion: {}", repositoryId);
            return;
        }

        // Delete all indexed data from all index tables via IndexRepository
        if (indexRepository != null) {
            try {
                indexRepository.deleteAllJavaIndexData(repositoryId);
                logger.info("Deleted Java index data for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete Java index data for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllRestApiIndexData(repositoryId);
                logger.info("Deleted REST API index data for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete REST API index data for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllSpringComponentData(repositoryId);
                logger.info("Deleted Spring Component index data for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete Spring Component index data for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllDependencyData(repositoryId);
                logger.info("Deleted dependency metadata for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete dependency metadata for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllConfigurationData(repositoryId);
                logger.info("Deleted configuration metadata for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete configuration metadata for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllDatabaseData(repositoryId);
                logger.info("Deleted database metadata for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete database metadata for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllStatistics(repositoryId);
                logger.info("Deleted statistics for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete statistics for repository {}: {}", repositoryId, e.getMessage());
            }
            
            try {
                indexRepository.deleteAllIndexingHistory(repositoryId);
                logger.info("Deleted indexing history for repository: {}", repositoryId);
            } catch (Exception e) {
                logger.warn("Failed to delete indexing history for repository {}: {}", repositoryId, e.getMessage());
            }
        }

        // Delete workspace directory if it exists
        if (repo.getWorkspacePath() != null) {
            try {
                java.nio.file.Path workspacePath = java.nio.file.Paths.get(repo.getWorkspacePath());
                if (java.nio.file.Files.exists(workspacePath)) {
                    deleteDirectoryRecursive(workspacePath);
                    logger.info("Deleted workspace directory: {}", repo.getWorkspacePath());
                }
            } catch (Exception e) {
                logger.warn("Failed to delete workspace directory: {}", repo.getWorkspacePath(), e);
            }
        }

        // Delete repository refresh history
        try {
            String deleteRefreshHistorySql = "DELETE FROM repository_refresh_history WHERE repository_id = ?";
            jdbcTemplate.update(deleteRefreshHistorySql, repositoryId);
            logger.info("Deleted repository refresh history for: {}", repositoryId);
        } catch (Exception e) {
            logger.warn("Failed to delete repository refresh history for {}: {}", repositoryId, e.getMessage());
        }

        // Delete chat history
        try {
            String deleteChatHistorySql = "DELETE FROM chat_history WHERE repository_id = ?";
            jdbcTemplate.update(deleteChatHistorySql, repositoryId);
            logger.info("Deleted chat history for: {}", repositoryId);
        } catch (Exception e) {
            logger.warn("Failed to delete chat history for {}: {}", repositoryId, e.getMessage());
        }

        // Delete test generation history
        try {
            String deleteTestGenHistorySql = "DELETE FROM test_generation_history WHERE repository_id = ?";
            jdbcTemplate.update(deleteTestGenHistorySql, repositoryId);
            logger.info("Deleted test generation history for: {}", repositoryId);
        } catch (Exception e) {
            logger.warn("Failed to delete test generation history for {}: {}", repositoryId, e.getMessage());
        }

        // Delete repository record last
        String sql = "DELETE FROM repository WHERE repository_id = ?";
        jdbcTemplate.update(sql, repositoryId);
        
        logger.info("Repository and all indexed data fully deleted: id={}, name={}", repositoryId, repo.getRepositoryName());
    }

    private void deleteDirectoryRecursive(java.nio.file.Path path) throws Exception {
        if (java.nio.file.Files.isDirectory(path)) {
            java.nio.file.Files.list(path).forEach(child -> {
                try {
                    if (java.nio.file.Files.isDirectory(child)) {
                        deleteDirectoryRecursive(child);
                    } else {
                        java.nio.file.Files.delete(child);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to delete: {}", child, e);
                }
            });
        }
        java.nio.file.Files.delete(path);
    }

    /**
     * Get repository statistics summary.
     */
    public Map<String, Object> getStatistics() {
        initSchema();

        Long totalRepositories = countAll();
        Long readyRepositories = countByStatus(RepositoryStatus.READY);
        Long failedRepositories = countByStatus(RepositoryStatus.FAILED);
        Long indexingRepositories = countByStatus(RepositoryStatus.INDEXING);
        Long registeredRepositories = countByStatus(RepositoryStatus.REGISTERED);
        Long indexedRepositories = countByStatus(RepositoryStatus.INDEXED);
        Long refreshingRepositories = countByStatus(RepositoryStatus.REFRESHING);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRepositories", totalRepositories != null ? totalRepositories : 0);
        stats.put("readyRepositories", readyRepositories != null ? readyRepositories : 0);
        stats.put("failedRepositories", failedRepositories != null ? failedRepositories : 0);
        stats.put("indexingRepositories", indexingRepositories != null ? indexingRepositories : 0);
        stats.put("registeredRepositories", registeredRepositories != null ? registeredRepositories : 0);
        stats.put("indexedRepositories", indexedRepositories != null ? indexedRepositories : 0);
        stats.put("refreshingRepositories", refreshingRepositories != null ? refreshingRepositories : 0);

        // Indexed repositories count (READY or READY status)
        stats.put("indexedRepositories", readyRepositories != null ? readyRepositories : 0);

        // Get total indexed Java files from statistics table
        try {
            String javaFilesSql = "SELECT COALESCE(SUM(covered_files), 0) FROM java_indexing_statistics";
            Long totalJavaFiles = jdbcTemplate.queryForObject(javaFilesSql, Long.class);
            stats.put("totalIndexedJavaFiles", totalJavaFiles != null ? totalJavaFiles : 0L);
        } catch (Exception e) {
            stats.put("totalIndexedJavaFiles", 0L);
        }

        try {
            String classesSql = "SELECT COALESCE(SUM(classes_found), 0) FROM java_indexing_statistics";
            Long totalClasses = jdbcTemplate.queryForObject(classesSql, Long.class);
            stats.put("totalIndexedClasses", totalClasses != null ? totalClasses : 0L);
        } catch (Exception e) {
            stats.put("totalIndexedClasses", 0L);
        }

        try {
            String endpointsSql = "SELECT COALESCE(SUM(endpoints_found), 0) FROM rest_api_statistics";
            Long totalEndpoints = jdbcTemplate.queryForObject(endpointsSql, Long.class);
            stats.put("totalIndexedEndpoints", totalEndpoints != null ? totalEndpoints : 0L);
        } catch (Exception e) {
            stats.put("totalIndexedEndpoints", 0L);
        }

        try {
            String springComponentsSql = "SELECT COALESCE(SUM(components_found), 0) FROM spring_component_statistics";
            Long totalSpringComponents = jdbcTemplate.queryForObject(springComponentsSql, Long.class);
            stats.put("totalIndexedSpringComponents", totalSpringComponents != null ? totalSpringComponents : 0L);
        } catch (Exception e) {
            stats.put("totalIndexedSpringComponents", 0L);
        }

        return stats;
    }
}
