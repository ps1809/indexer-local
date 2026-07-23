package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.RepositoryDocumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access layer for persisting repository documentation into SQLite.
 */
@Component
public class DocumentationRepository {

    private static final Logger logger = LoggerFactory.getLogger(DocumentationRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public DocumentationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Initialize the documentation table schema.
     */
    public void initSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS repository_documentation (" +
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
     * Save or update documentation for a repository.
     */
    public Long save(RepositoryDocumentation documentation) {
        initSchema();
        
        // Check if documentation already exists
        RepositoryDocumentation existing = findByRepositoryId(documentation.getRepositoryId());
        
        if (existing != null) {
            // Update existing documentation
            return update(documentation);
        }
        
        // Insert new documentation
        String sql = "INSERT INTO repository_documentation (repository_id, markdown_content, generated_at, content_size, generation_status) " +
                "VALUES (?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, documentation.getRepositoryId());
            ps.setString(2, documentation.getMarkdownContent());
            ps.setTimestamp(3, Timestamp.valueOf(documentation.getGeneratedAt()));
            ps.setLong(4, documentation.getContentSize() != null ? documentation.getContentSize() : 0L);
            ps.setString(5, documentation.getGenerationStatus());
            return ps;
        }, keyHolder);
        
        Long id = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
        documentation.setId(id);
        logger.info("Documentation saved for repository: {}, id: {}", documentation.getRepositoryId(), id);
        return id;
    }

    /**
     * Update existing documentation.
     */
    public Long update(RepositoryDocumentation documentation) {
        initSchema();
        
        String sql = "UPDATE repository_documentation SET markdown_content = ?, generated_at = ?, content_size = ?, generation_status = ? " +
                "WHERE repository_id = ?";
        
        jdbcTemplate.update(sql,
                documentation.getMarkdownContent(),
                Timestamp.valueOf(documentation.getGeneratedAt()),
                documentation.getContentSize() != null ? documentation.getContentSize() : 0L,
                documentation.getGenerationStatus(),
                documentation.getRepositoryId());
        
        logger.info("Documentation updated for repository: {}", documentation.getRepositoryId());
        return documentation.getId();
    }

    /**
     * Find documentation by repository ID.
     */
    public RepositoryDocumentation findByRepositoryId(String repositoryId) {
        initSchema();
        
        String sql = "SELECT id, repository_id, markdown_content, generated_at, content_size, generation_status " +
                "FROM repository_documentation WHERE repository_id = ?";
        
        List<RepositoryDocumentation> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            RepositoryDocumentation doc = new RepositoryDocumentation();
            doc.setId(rs.getLong("id"));
            doc.setRepositoryId(rs.getString("repository_id"));
            doc.setMarkdownContent(rs.getString("markdown_content"));
            doc.setGeneratedAt(rs.getTimestamp("generated_at") != null ? 
                    rs.getTimestamp("generated_at").toLocalDateTime() : null);
            doc.setContentSize(rs.getLong("content_size"));
            doc.setGenerationStatus(rs.getString("generation_status"));
            return doc;
        }, repositoryId);
        
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Delete documentation by repository ID.
     */
    public void deleteByRepositoryId(String repositoryId) {
        initSchema();
        
        String sql = "DELETE FROM repository_documentation WHERE repository_id = ?";
        jdbcTemplate.update(sql, repositoryId);
        logger.info("Documentation deleted for repository: {}", repositoryId);
    }

    /**
     * Check if documentation exists for a repository.
     */
    public boolean existsByRepositoryId(String repositoryId) {
        initSchema();
        
        String sql = "SELECT COUNT(*) FROM repository_documentation WHERE repository_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, repositoryId);
        return count != null && count > 0;
    }
}