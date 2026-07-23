package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.chat.ChatHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Data access layer for persisting chat request history into SQLite.
 */
@Component
public class ChatHistoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public ChatHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Initialize the chat_history table schema.
     */
    public void initSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS chat_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "repository_id TEXT NOT NULL, " +
                "request_type TEXT NOT NULL, " +
                "query TEXT, " +
                "entity_types TEXT, " +
                "entities_retrieved INTEGER, " +
                "execution_duration_ms INTEGER, " +
                "request_timestamp TIMESTAMP NOT NULL)";
        jdbcTemplate.execute(sql);
    }

    /**
     * Row mapper for ChatHistory entity.
     */
    private final RowMapper<ChatHistory> rowMapper = (rs, rowNum) -> {
        ChatHistory history = new ChatHistory();
        history.setId(rs.getLong("id"));
        history.setRepositoryId(rs.getString("repository_id"));
        history.setRequestType(rs.getString("request_type"));
        history.setQuery(rs.getString("query"));
        history.setEntityTypes(rs.getString("entity_types"));
        history.setEntitiesRetrieved(rs.getObject("entities_retrieved") != null ? rs.getInt("entities_retrieved") : null);
        history.setExecutionDurationMs(rs.getObject("execution_duration_ms") != null ? rs.getLong("execution_duration_ms") : null);
        history.setRequestTimestamp(rs.getTimestamp("request_timestamp") != null
                ? rs.getTimestamp("request_timestamp").toLocalDateTime() : null);
        return history;
    };

    /**
     * Save a chat history record.
     */
    public Long save(ChatHistory chatHistory) {
        initSchema();
        String sql = "INSERT INTO chat_history (repository_id, request_type, query, entity_types, " +
                "entities_retrieved, execution_duration_ms, request_timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, chatHistory.getRepositoryId());
            ps.setString(2, chatHistory.getRequestType());
            ps.setString(3, chatHistory.getQuery());
            ps.setString(4, chatHistory.getEntityTypes());
            ps.setObject(5, chatHistory.getEntitiesRetrieved());
            ps.setObject(6, chatHistory.getExecutionDurationMs());
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(chatHistory.getRequestTimestamp()));
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
        chatHistory.setId(id);
        logger.debug("Chat history saved: id={}, repositoryId={}, type={}", id, chatHistory.getRepositoryId(), chatHistory.getRequestType());
        return id;
    }

    /**
     * Find all chat history records for a repository, ordered by most recent first.
     */
    public List<ChatHistory> findByRepositoryId(String repositoryId) {
        initSchema();
        String sql = "SELECT id, repository_id, request_type, query, entity_types, " +
                "entities_retrieved, execution_duration_ms, request_timestamp " +
                "FROM chat_history WHERE repository_id = ? ORDER BY request_timestamp DESC";
        return jdbcTemplate.query(sql, rowMapper, repositoryId);
    }

    /**
     * Find chat history records for a repository with pagination.
     */
    public List<ChatHistory> findByRepositoryIdWithPagination(String repositoryId, int page, int size) {
        initSchema();
        String sql = "SELECT id, repository_id, request_type, query, entity_types, " +
                "entities_retrieved, execution_duration_ms, request_timestamp " +
                "FROM chat_history WHERE repository_id = ? ORDER BY request_timestamp DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, rowMapper, repositoryId, size, page * size);
    }

    /**
     * Count chat history records for a repository.
     */
    public Long countByRepositoryId(String repositoryId) {
        initSchema();
        String sql = "SELECT COUNT(*) FROM chat_history WHERE repository_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, repositoryId);
        return count != null ? count : 0L;
    }

    /**
     * Delete all chat history records for a repository.
     */
    public void deleteByRepositoryId(String repositoryId) {
        initSchema();
        String sql = "DELETE FROM chat_history WHERE repository_id = ?";
        int deleted = jdbcTemplate.update(sql, repositoryId);
        logger.info("Deleted {} chat history records for repository: {}", deleted, repositoryId);
    }
}