package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.FileHashRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for persisting and retrieving file hash records.
 * Stores SHA-256 hashes along with file metadata for change detection.
 */
@Repository
public class FileHashRepository {

    private static final Logger log = LoggerFactory.getLogger(FileHashRepository.class);

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<FileHashRecord> rowMapper = (rs, rowNum) -> {
        FileHashRecord record = new FileHashRecord();
        record.setId(rs.getLong("id"));
        record.setRepositoryId(rs.getString("repository_id"));
        record.setFilePath(rs.getString("file_path"));
        record.setSha256Hash(rs.getString("sha256_hash"));
        record.setFileSize(rs.getLong("file_size"));
        record.setLastModified(rs.getTimestamp("last_modified") != null
                ? rs.getTimestamp("last_modified").toLocalDateTime() : null);
        record.setLastIndexedAt(rs.getTimestamp("last_indexed_at") != null
                ? rs.getTimestamp("last_indexed_at").toLocalDateTime() : null);
        record.setProcessingStatus(rs.getString("processing_status"));
        record.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        record.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return record;
    };

    public FileHashRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Initialize the file_hash table schema.
     */
    public void initSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS file_hash (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "repository_id TEXT NOT NULL, " +
                "file_path TEXT NOT NULL, " +
                "sha256_hash TEXT NOT NULL, " +
                "file_size INTEGER NOT NULL DEFAULT 0, " +
                "last_modified TIMESTAMP, " +
                "last_indexed_at TIMESTAMP, " +
                "processing_status TEXT DEFAULT 'PENDING', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE(repository_id, file_path))";
        jdbcTemplate.execute(sql);
        log.debug("[FILE-HASH-REPO] Schema initialized");
    }

    /**
     * Find a hash record by repository ID and file path.
     */
    public Optional<FileHashRecord> findByRepositoryIdAndFilePath(String repositoryId, String filePath) {
        String sql = "SELECT id, repository_id, file_path, sha256_hash, file_size, last_modified, " +
                "last_indexed_at, processing_status, created_at, updated_at " +
                "FROM file_hash WHERE repository_id = ? AND file_path = ?";
        List<FileHashRecord> results = jdbcTemplate.query(sql, rowMapper, repositoryId, filePath);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find all hash records for a repository.
     */
    public List<FileHashRecord> findByRepositoryId(String repositoryId) {
        String sql = "SELECT id, repository_id, file_path, sha256_hash, file_size, last_modified, " +
                "last_indexed_at, processing_status, created_at, updated_at " +
                "FROM file_hash WHERE repository_id = ? ORDER BY file_path";
        return jdbcTemplate.query(sql, rowMapper, repositoryId);
    }

    /**
     * Save or update a file hash record.
     */
    public void save(FileHashRecord record) {
        initSchema();

        String checkSql = "SELECT COUNT(*) FROM file_hash WHERE repository_id = ? AND file_path = ?";
        Long count = jdbcTemplate.queryForObject(checkSql, Long.class,
                record.getRepositoryId(), record.getFilePath());

        if (count != null && count > 0) {
            update(record);
        } else {
            insert(record);
        }
    }

    /**
     * Insert a new file hash record.
     */
    private void insert(FileHashRecord record) {
        String sql = "INSERT INTO file_hash (repository_id, file_path, sha256_hash, file_size, " +
                "last_modified, last_indexed_at, processing_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                record.getRepositoryId(),
                record.getFilePath(),
                record.getSha256Hash(),
                record.getFileSize(),
                record.getLastModified() != null ? Timestamp.valueOf(record.getLastModified()) : null,
                record.getLastIndexedAt() != null ? Timestamp.valueOf(record.getLastIndexedAt()) : null,
                record.getProcessingStatus(),
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()));
    }

    /**
     * Update an existing file hash record.
     */
    private void update(FileHashRecord record) {
        String sql = "UPDATE file_hash SET sha256_hash = ?, file_size = ?, last_modified = ?, " +
                "last_indexed_at = ?, processing_status = ?, updated_at = ? " +
                "WHERE repository_id = ? AND file_path = ?";
        jdbcTemplate.update(sql,
                record.getSha256Hash(),
                record.getFileSize(),
                record.getLastModified() != null ? Timestamp.valueOf(record.getLastModified()) : null,
                record.getLastIndexedAt() != null ? Timestamp.valueOf(record.getLastIndexedAt()) : null,
                record.getProcessingStatus(),
                Timestamp.valueOf(LocalDateTime.now()),
                record.getRepositoryId(),
                record.getFilePath());
    }

    /**
     * Delete a hash record by repository ID and file path.
     */
    public void deleteByRepositoryIdAndFilePath(String repositoryId, String filePath) {
        String sql = "DELETE FROM file_hash WHERE repository_id = ? AND file_path = ?";
        jdbcTemplate.update(sql, repositoryId, filePath);
    }

    /**
     * Delete all hash records for a repository.
     */
    public void deleteByRepositoryId(String repositoryId) {
        String sql = "DELETE FROM file_hash WHERE repository_id = ?";
        jdbcTemplate.update(sql, repositoryId);
    }

    /**
     * Get the count of hash records for a repository.
     */
    public long countByRepositoryId(String repositoryId) {
        String sql = "SELECT COUNT(*) FROM file_hash WHERE repository_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, repositoryId);
        return count != null ? count : 0;
    }

    /**
     * Get all file paths that have hash records for a repository.
     */
    public List<String> findFilePathsByRepositoryId(String repositoryId) {
        String sql = "SELECT file_path FROM file_hash WHERE repository_id = ? ORDER BY file_path";
        return jdbcTemplate.queryForList(sql, String.class, repositoryId);
    }

    /**
     * Batch save multiple file hash records.
     */
    public void saveAll(List<FileHashRecord> records) {
        initSchema();
        for (FileHashRecord record : records) {
            save(record);
        }
    }

    /**
     * Get processing status counts for a repository.
     */
    public Map<String, Long> getProcessingStatusCounts(String repositoryId) {
        String sql = "SELECT processing_status, COUNT(*) as count FROM file_hash " +
                "WHERE repository_id = ? GROUP BY processing_status";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, repositoryId);
        Map<String, Long> counts = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            counts.put((String) row.get("processing_status"), (Long) row.get("count"));
        }
        return counts;
    }
}