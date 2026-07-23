package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.testsupport.TestGenerationHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Repository for persisting test generation request history and statistics.
 */
@Repository
public class TestGenerationRepository {

    private static final Logger logger = LoggerFactory.getLogger(TestGenerationRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public TestGenerationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Initialize the test generation history table schema.
     */
    public void initSchema() {
        String sqlCreateTable =
            "CREATE TABLE IF NOT EXISTS test_generation_history (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "repository_id TEXT NOT NULL, " +
            "request_type TEXT NOT NULL, " +
            "request_parameters TEXT, " +
            "classes_retrieved INTEGER DEFAULT 0, " +
            "methods_retrieved INTEGER DEFAULT 0, " +
            "response_size_bytes INTEGER DEFAULT 0, " +
            "execution_duration_ms INTEGER DEFAULT 0, " +
            "requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "status TEXT NOT NULL, " +
            "error_message TEXT)";

        jdbcTemplate.execute(sqlCreateTable);
    }

    private final RowMapper<TestGenerationHistory> rowMapper = (rs, rowNum) -> {
        TestGenerationHistory history = new TestGenerationHistory();
        history.setId(rs.getLong("id"));
        history.setRepositoryId(rs.getString("repository_id"));
        history.setRequestType(rs.getString("request_type"));
        history.setRequestParameters(rs.getString("request_parameters"));
        history.setClassesRetrieved(rs.getInt("classes_retrieved"));
        history.setMethodsRetrieved(rs.getInt("methods_retrieved"));
        history.setResponseSizeBytes(rs.getLong("response_size_bytes"));
        history.setExecutionDurationMs(rs.getLong("execution_duration_ms"));
        history.setRequestedAt(rs.getTimestamp("requested_at") != null ?
                rs.getTimestamp("requested_at").toLocalDateTime() : null);
        history.setStatus(rs.getString("status"));
        history.setErrorMessage(rs.getString("error_message"));
        return history;
    };

    /**
     * Save a test generation history record.
     */
    public Long save(TestGenerationHistory history) {
        initSchema();
        String sql = "INSERT INTO test_generation_history (repository_id, request_type, request_parameters, " +
                     "classes_retrieved, methods_retrieved, response_size_bytes, execution_duration_ms, " +
                     "requested_at, status, error_message) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, history.getRepositoryId());
            ps.setString(2, history.getRequestType());
            ps.setString(3, history.getRequestParameters());
            ps.setInt(4, history.getClassesRetrieved());
            ps.setInt(5, history.getMethodsRetrieved());
            ps.setLong(6, history.getResponseSizeBytes());
            ps.setLong(7, history.getExecutionDurationMs());
            ps.setTimestamp(8, java.sql.Timestamp.valueOf(history.getRequestedAt()));
            ps.setString(9, history.getStatus());
            ps.setString(10, history.getErrorMessage());
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
        history.setId(id);
        return id;
    }

    /**
     * Find all history records for a repository.
     */
    public List<TestGenerationHistory> findByRepositoryId(String repositoryId) {
        initSchema();
        String sql = "SELECT * FROM test_generation_history WHERE repository_id = ? ORDER BY requested_at DESC";
        return jdbcTemplate.query(sql, rowMapper, repositoryId);
    }

    /**
     * Find history records with pagination.
     */
    public List<TestGenerationHistory> findByRepositoryIdWithPagination(String repositoryId, int page, int size) {
        initSchema();
        String sql = "SELECT * FROM test_generation_history WHERE repository_id = ? ORDER BY requested_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, rowMapper, repositoryId, size, page * size);
    }

    /**
     * Get execution statistics for a repository.
     */
    public java.util.Map<String, Object> getStatistics(String repositoryId) {
        initSchema();
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        try {
            String countSql = "SELECT COUNT(*) FROM test_generation_history WHERE repository_id = ?";
            Long totalCount = jdbcTemplate.queryForObject(countSql, Long.class, repositoryId);
            stats.put("totalRequests", totalCount != null ? totalCount : 0L);

            String successSql = "SELECT COUNT(*) FROM test_generation_history WHERE repository_id = ? AND status = 'SUCCESS'";
            Long successCount = jdbcTemplate.queryForObject(successSql, Long.class, repositoryId);
            stats.put("successfulRequests", successCount != null ? successCount : 0L);

            String errorSql = "SELECT COUNT(*) FROM test_generation_history WHERE repository_id = ? AND status = 'ERROR'";
            Long errorCount = jdbcTemplate.queryForObject(errorSql, Long.class, repositoryId);
            stats.put("failedRequests", errorCount != null ? errorCount : 0L);

            String avgDurationSql = "SELECT AVG(execution_duration_ms) FROM test_generation_history WHERE repository_id = ? AND status = 'SUCCESS'";
            Double avgDuration = jdbcTemplate.queryForObject(avgDurationSql, Double.class, repositoryId);
            stats.put("averageDurationMs", avgDuration != null ? Math.round(avgDuration) : 0L);

            String totalClassesSql = "SELECT SUM(classes_retrieved) FROM test_generation_history WHERE repository_id = ?";
            Long totalClasses = jdbcTemplate.queryForObject(totalClassesSql, Long.class, repositoryId);
            stats.put("totalClassesRetrieved", totalClasses != null ? totalClasses : 0L);

            String totalMethodsSql = "SELECT SUM(methods_retrieved) FROM test_generation_history WHERE repository_id = ?";
            Long totalMethods = jdbcTemplate.queryForObject(totalMethodsSql, Long.class, repositoryId);
            stats.put("totalMethodsRetrieved", totalMethods != null ? totalMethods : 0L);
        } catch (Exception e) {
            logger.warn("Error getting test generation statistics for repository: {}", repositoryId, e);
            stats.put("totalRequests", 0L);
            stats.put("successfulRequests", 0L);
            stats.put("failedRequests", 0L);
            stats.put("averageDurationMs", 0L);
            stats.put("totalClassesRetrieved", 0L);
            stats.put("totalMethodsRetrieved", 0L);
        }

        return stats;
    }

    /**
     * Delete all history for a repository.
     */
    public void deleteByRepositoryId(String repositoryId) {
        initSchema();
        jdbcTemplate.update("DELETE FROM test_generation_history WHERE repository_id = ?", repositoryId);
    }

    /**
     * Count history records for a repository.
     */
    public Long countByRepositoryId(String repositoryId) {
        initSchema();
        String sql = "SELECT COUNT(*) FROM test_generation_history WHERE repository_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, repositoryId);
        return count != null ? count : 0L;
    }
}