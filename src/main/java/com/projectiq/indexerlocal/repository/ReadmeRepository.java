package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.RepositoryReadme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

/**
 * Data access layer for persisting repository README into SQLite.
 */
@Component
public class ReadmeRepository {

    private static final Logger logger = LoggerFactory.getLogger(ReadmeRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public ReadmeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Initialize the readme table schema.
     */
    public void initSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS repository_readme (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "repository_id TEXT UNIQUE NOT NULL, " +
                "markdown_content TEXT, " +
                "generated_at TIMESTAMP, " +
                "content_size INTEGER, " +
                "generation_status TEXT, " +
                "FOREIGN KEY (repository_id) REFERENCES repository(repository_id))";
        jdbcTemplate.execute(sql);
    }

    /**
     * Save or update readme for a repository.
     */
    public Long save(RepositoryReadme readme) {
        initSchema();
        
        // Check if readme already exists
        RepositoryReadme existing = findByRepositoryId(readme.getRepositoryId());
        
        if (existing != null) {
            // Update existing readme
            return update(readme);
        }
        
        // Insert new readme
        String sql = "INSERT INTO repository_readme (repository_id, markdown_content, generated_at, content_size, generation_status) " +
                "VALUES (?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, readme.getRepositoryId());
            ps.setString(2, readme.getMarkdownContent());
            ps.setTimestamp(3, Timestamp.valueOf(readme.getGeneratedAt()));
            ps.setLong(4, readme.getContentSize() != null ? readme.getContentSize() : 0L);
            ps.setString(5, readme.getGenerationStatus());
            return ps;
        }, keyHolder);
        
        Long id = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
        readme.setId(id);
        logger.info("README saved for repository: {}, id: {}", readme.getRepositoryId(), id);
        return id;
    }

    /**
     * Update existing readme.
     */
    public Long update(RepositoryReadme readme) {
        initSchema();
        
        String sql = "UPDATE repository_readme SET markdown_content = ?, generated_at = ?, content_size = ?, generation_status = ? " +
                "WHERE repository_id = ?";
        
        jdbcTemplate.update(sql,
                readme.getMarkdownContent(),
                Timestamp.valueOf(readme.getGeneratedAt()),
                readme.getContentSize() != null ? readme.getContentSize() : 0L,
                readme.getGenerationStatus(),
                readme.getRepositoryId());
        
        logger.info("README updated for repository: {}", readme.getRepositoryId());
        return readme.getId();
    }

    /**
     * Find readme by repository ID.
     */
    public RepositoryReadme findByRepositoryId(String repositoryId) {
        initSchema();
        
        String sql = "SELECT id, repository_id, markdown_content, generated_at, content_size, generation_status " +
                "FROM repository_readme WHERE repository_id = ?";
        
        List<RepositoryReadme> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            RepositoryReadme readme = new RepositoryReadme();
            readme.setId(rs.getLong("id"));
            readme.setRepositoryId(rs.getString("repository_id"));
            readme.setMarkdownContent(rs.getString("markdown_content"));
            readme.setGeneratedAt(rs.getTimestamp("generated_at") != null ? 
                    rs.getTimestamp("generated_at").toLocalDateTime() : null);
            readme.setContentSize(rs.getLong("content_size"));
            readme.setGenerationStatus(rs.getString("generation_status"));
            return readme;
        }, repositoryId);
        
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Delete readme by repository ID.
     */
    public void deleteByRepositoryId(String repositoryId) {
        initSchema();
        
        String sql = "DELETE FROM repository_readme WHERE repository_id = ?";
        jdbcTemplate.update(sql, repositoryId);
        logger.info("README deleted for repository: {}", repositoryId);
    }

    /**
     * Check if readme exists for a repository.
     */
    public boolean existsByRepositoryId(String repositoryId) {
        initSchema();
        
        String sql = "SELECT COUNT(*) FROM repository_readme WHERE repository_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, repositoryId);
        return count != null && count > 0;
    }
}