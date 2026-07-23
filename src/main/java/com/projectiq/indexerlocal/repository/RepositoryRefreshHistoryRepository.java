package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.RepositoryRefreshHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for repository refresh history using JdbcTemplate.
 */
@Repository
public class RepositoryRefreshHistoryRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "repository_refresh_history";

    /**
     * Create a new refresh history record.
     */
    public RepositoryRefreshHistory create(String repositoryId, String refreshType) {
        String sql = "INSERT INTO " + TABLE_NAME + " (repository_id, refresh_type, status, start_time, created_at) VALUES (?, ?, 'IN_PROGRESS', ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(sql, repositoryId, refreshType, now, now);

        // Return the created record with defaults
        RepositoryRefreshHistory history = new RepositoryRefreshHistory();
        history.setRepositoryId(repositoryId);
        history.setRefreshType(refreshType);
        history.setStatus("IN_PROGRESS");
        history.setStartTime(now);
        history.setCreatedAt(now);
        return history;
    }

    /**
     * Update refresh history with completion details.
     */
    public void updateCompletion(String repositoryId, String refreshType, String status, Long executionDurationMs, String errorMessage) {
        String sql = "UPDATE " + TABLE_NAME + " SET status = ?, end_time = ?, execution_duration_ms = ?, error_message = ? WHERE repository_id = ? AND refresh_type = ? AND start_time = (SELECT MIN(start_time) FROM " + TABLE_NAME + " WHERE repository_id = ? AND refresh_type = ?)";
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(sql, status, now, executionDurationMs, errorMessage, repositoryId, refreshType, repositoryId, refreshType);
    }

    /**
     * Get recent refresh history for a repository.
     */
    public List<RepositoryRefreshHistory> getRecentHistory(String repositoryId, int limit) {
        String sql = "SELECT id, repository_id, refresh_type, status, start_time, end_time, execution_duration_ms, error_message, created_at FROM " + TABLE_NAME + " WHERE repository_id = ? ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, this::mapRow, repositoryId, limit);
    }

    /**
     * Get latest refresh history entry for a repository.
     */
    public RepositoryRefreshHistory getLatestHistory(String repositoryId) {
        String sql = "SELECT id, repository_id, refresh_type, status, start_time, end_time, execution_duration_ms, error_message, created_at FROM " + TABLE_NAME + " WHERE repository_id = ? ORDER BY created_at DESC LIMIT 1";
        List<RepositoryRefreshHistory> results = jdbcTemplate.query(sql, this::mapRow, repositoryId);
        return results.isEmpty() ? null : results.get(0);
    }

    private RepositoryRefreshHistory mapRow(java.sql.ResultSet rs, int rowNum) {
        try {
            RepositoryRefreshHistory history = new RepositoryRefreshHistory();
            history.setId(rs.getLong("id"));
            history.setRepositoryId(rs.getString("repository_id"));
            history.setRefreshType(rs.getString("refresh_type"));
            history.setStatus(rs.getString("status"));
            history.setStartTime(rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toLocalDateTime() : null);
            history.setEndTime(rs.getTimestamp("end_time") != null ? rs.getTimestamp("end_time").toLocalDateTime() : null);
            history.setExecutionDurationMs(rs.getLong("execution_duration_ms"));
            history.setErrorMessage(rs.getString("error_message"));
            history.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            return history;
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error mapping row", e);
        }
    }
}