package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.RepositoryStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer for persisting repository metadata into SQLite.
 */
@Component
public class RepositoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public RepositoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
                     "registration_timestamp, last_updated_timestamp, status, build_system, technology_stack) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, repository.getRepositoryId());
            ps.setString(2, repository.getRepositoryName());
            ps.setString(3, repository.getOriginalPath());
            ps.setString(4, repository.getWorkspacePath());
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(repository.getRegistrationTimestamp()));
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(repository.getLastUpdatedTimestamp()));
            ps.setString(7, repository.getStatus().name());
            ps.setString(8, repository.getBuildSystem());
            ps.setString(9, repository.getTechnologyStack());
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
                     "last_updated_timestamp = ?, status = ?, build_system = ?, technology_stack = ? WHERE id = ?";
        
        jdbcTemplate.update(sql,
            repository.getRepositoryName(),
            repository.getOriginalPath(),
            repository.getWorkspacePath(),
            java.sql.Timestamp.valueOf(repository.getLastUpdatedTimestamp()),
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
                     "registration_timestamp, last_updated_timestamp, status, build_system, technology_stack " +
                     "FROM repository ORDER BY id";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Find a repository by its ID.
     */
    public com.projectiq.indexerlocal.model.Repository findById(Long id) {
        initSchema();
        String sql = "SELECT id, repository_id, repository_name, original_path, workspace_path, " +
                     "registration_timestamp, last_updated_timestamp, status, build_system, technology_stack " +
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
                     "registration_timestamp, last_updated_timestamp, status, build_system, technology_stack " +
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
                     "registration_timestamp, last_updated_timestamp, status, build_system, technology_stack " +
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
}