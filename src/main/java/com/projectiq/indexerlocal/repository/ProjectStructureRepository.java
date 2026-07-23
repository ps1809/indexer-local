package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.DirectoryClassification;
import com.projectiq.indexerlocal.model.DirectoryMetadata;
import com.projectiq.indexerlocal.model.FileClassification;
import com.projectiq.indexerlocal.model.FileMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data access layer for persisting project structure metadata into SQLite.
 */
@Component
public class ProjectStructureRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProjectStructureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== Directory Methods ====================

    public void initDirectorySchema() {
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

    public Long saveDirectory(DirectoryMetadata metadata) {
        initDirectorySchema();
        String sql = "INSERT INTO directory_metadata (repository_id, path, name, relative_path, depth, " +
                "classification, is_hidden, last_modified, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, metadata.getRepositoryId());
            ps.setString(2, metadata.getPath());
            ps.setString(3, metadata.getName());
            ps.setString(4, metadata.getRelativePath());
            ps.setInt(5, metadata.getDepth() != null ? metadata.getDepth() : 0);
            ps.setString(6, metadata.getClassification() != null ? metadata.getClassification().name() : "UNKNOWN");
            ps.setBoolean(7, metadata.getHidden() != null && metadata.getHidden());
            ps.setTimestamp(8, metadata.getLastModified() != null ? Timestamp.valueOf(metadata.getLastModified()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(9, Timestamp.valueOf(metadata.getCreatedAt()));
            ps.setTimestamp(10, Timestamp.valueOf(metadata.getUpdatedAt()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public List<DirectoryMetadata> findDirectoriesByRepositoryId(String repositoryId) {
        initDirectorySchema();
        String sql = "SELECT id, repository_id, path, name, relative_path, depth, classification, " +
                "is_hidden, last_modified, created_at, updated_at FROM directory_metadata " +
                "WHERE repository_id = ? ORDER BY path";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DirectoryMetadata metadata = new DirectoryMetadata();
            metadata.setId(rs.getLong("id"));
            metadata.setRepositoryId(rs.getString("repository_id"));
            metadata.setPath(rs.getString("path"));
            metadata.setName(rs.getString("name"));
            metadata.setRelativePath(rs.getString("relative_path"));
            metadata.setDepth(rs.getInt("depth"));
            String classification = rs.getString("classification");
            if (classification != null) {
                metadata.setClassification(DirectoryClassification.valueOf(classification));
            }
            metadata.setHidden(rs.getInt("is_hidden") == 1);
            Timestamp ts = rs.getTimestamp("last_modified");
            if (ts != null) {
                metadata.setLastModified(ts.toLocalDateTime());
            }
            metadata.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            metadata.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
            return metadata;
        }, repositoryId);
    }

    public Integer countDirectoriesByRepositoryId(String repositoryId) {
        initDirectorySchema();
        String sql = "SELECT COUNT(*) FROM directory_metadata WHERE repository_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, repositoryId);
        return count != null ? count : 0;
    }

    public void deleteDirectoriesByRepositoryId(String repositoryId) {
        initDirectorySchema();
        jdbcTemplate.execute("DELETE FROM directory_metadata WHERE repository_id = '" + repositoryId + "'");
    }

    // ==================== File Methods ====================

    public void initFileSchema() {
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

    public Long saveFile(FileMetadata metadata) {
        initFileSchema();
        String sql = "INSERT INTO file_metadata (repository_id, path, name, relative_path, extension, " +
                "file_size, classification, is_hidden, depth, last_modified, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, metadata.getRepositoryId());
            ps.setString(2, metadata.getPath());
            ps.setString(3, metadata.getName());
            ps.setString(4, metadata.getRelativePath());
            ps.setString(5, metadata.getExtension());
            ps.setLong(6, metadata.getFileSize() != null ? metadata.getFileSize() : 0);
            ps.setString(7, metadata.getClassification() != null ? metadata.getClassification().name() : "UNKNOWN");
            ps.setBoolean(8, metadata.getHidden() != null && metadata.getHidden());
            ps.setInt(9, metadata.getDepth() != null ? metadata.getDepth() : 0);
            ps.setTimestamp(10, metadata.getLastModified() != null ? Timestamp.valueOf(metadata.getLastModified()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(11, Timestamp.valueOf(metadata.getCreatedAt()));
            ps.setTimestamp(12, Timestamp.valueOf(metadata.getUpdatedAt()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public List<FileMetadata> findFilesByRepositoryId(String repositoryId) {
        initFileSchema();
        String sql = "SELECT id, repository_id, path, name, relative_path, extension, file_size, " +
                "classification, is_hidden, depth, last_modified, created_at, updated_at FROM file_metadata " +
                "WHERE repository_id = ? ORDER BY path";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            FileMetadata metadata = new FileMetadata();
            metadata.setId(rs.getLong("id"));
            metadata.setRepositoryId(rs.getString("repository_id"));
            metadata.setPath(rs.getString("path"));
            metadata.setName(rs.getString("name"));
            metadata.setRelativePath(rs.getString("relative_path"));
            metadata.setExtension(rs.getString("extension"));
            metadata.setFileSize(rs.getLong("file_size"));
            String classification = rs.getString("classification");
            if (classification != null) {
                metadata.setClassification(FileClassification.valueOf(classification));
            }
            metadata.setHidden(rs.getInt("is_hidden") == 1);
            metadata.setDepth(rs.getInt("depth"));
            Timestamp ts = rs.getTimestamp("last_modified");
            if (ts != null) {
                metadata.setLastModified(ts.toLocalDateTime());
            }
            metadata.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            metadata.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
            return metadata;
        }, repositoryId);
    }

    public Integer countFilesByRepositoryId(String repositoryId) {
        initFileSchema();
        String sql = "SELECT COUNT(*) FROM file_metadata WHERE repository_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, repositoryId);
        return count != null ? count : 0;
    }

    public void deleteFilesByRepositoryId(String repositoryId) {
        initFileSchema();
        jdbcTemplate.execute("DELETE FROM file_metadata WHERE repository_id = '" + repositoryId + "'");
    }

    // ==================== Statistics Methods ====================

    public void initStatisticsSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS project_structure_statistics (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "repository_id TEXT UNIQUE NOT NULL, " +
                "total_directories INTEGER, " +
                "total_files INTEGER, " +
                "total_size BIGINT, " +
                "file_count_by_extension TEXT, " +
                "directory_count_by_classification TEXT, " +
                "file_count_by_classification TEXT, " +
                "deepest_directory_level INTEGER, " +
                "largest_file_size BIGINT, " +
                "largest_file_name TEXT, " +
                "largest_file_path TEXT, " +
                "analyzed_at TIMESTAMP)";
        jdbcTemplate.execute(sql);
    }

    public void saveStatistics(String repositoryId, Integer totalDirectories, Integer totalFiles,
                               Long totalSize, Map<String, Integer> fileCountByExtension,
                               Map<DirectoryClassification, Integer> directoryCountByClassification,
                               Map<FileClassification, Integer> fileCountByClassification,
                               Integer deepestDirectoryLevel, Long largestFileSize,
                               String largestFileName, String largestFilePath) {
        initStatisticsSchema();
        String sql = "INSERT OR REPLACE INTO project_structure_statistics (" +
                "repository_id, total_directories, total_files, total_size, file_count_by_extension, " +
                "directory_count_by_classification, file_count_by_classification, deepest_directory_level, " +
                "largest_file_size, largest_file_name, largest_file_path, analyzed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, repositoryId, totalDirectories, totalFiles, totalSize,
                mapToString(fileCountByExtension),
                mapToDirectoryClassificationString(directoryCountByClassification),
                mapToFileClassificationString(fileCountByClassification),
                deepestDirectoryLevel, largestFileSize, largestFileName, largestFilePath,
                Timestamp.valueOf(LocalDateTime.now()));
    }

    public Map<String, Integer> findFileCountByExtension(String repositoryId) {
        initStatisticsSchema();
        String sql = "SELECT file_count_by_extension FROM project_structure_statistics WHERE repository_id = ?";
        String result = jdbcTemplate.queryForObject(sql, String.class, repositoryId);
        return stringToMap(result);
    }

    public Map<DirectoryClassification, Integer> findDirectoryCountByClassification(String repositoryId) {
        initStatisticsSchema();
        String sql = "SELECT directory_count_by_classification FROM project_structure_statistics WHERE repository_id = ?";
        String result = jdbcTemplate.queryForObject(sql, String.class, repositoryId);
        return stringToDirectoryMap(result);
    }

    public Map<FileClassification, Integer> findFileCountByClassification(String repositoryId) {
        initStatisticsSchema();
        String sql = "SELECT file_count_by_classification FROM project_structure_statistics WHERE repository_id = ?";
        String result = jdbcTemplate.queryForObject(sql, String.class, repositoryId);
        return stringToFileMap(result);
    }

    public void deleteStatisticsByRepositoryId(String repositoryId) {
        initStatisticsSchema();
        jdbcTemplate.execute("DELETE FROM project_structure_statistics WHERE repository_id = '" + repositoryId + "'");
    }

    // ==================== Helper Methods ====================

    private String mapToString(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private Map<String, Integer> stringToMap(String str) {
        Map<String, Integer> map = new HashMap<>();
        if (str == null || str.isEmpty()) {
            return map;
        }
        String[] pairs = str.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                map.put(parts[0], Integer.parseInt(parts[1]));
            }
        }
        return map;
    }

    private String mapToDirectoryClassificationString(Map<DirectoryClassification, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<DirectoryClassification, Integer> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(entry.getKey().name()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private Map<DirectoryClassification, Integer> stringToDirectoryMap(String str) {
        Map<DirectoryClassification, Integer> map = new HashMap<>();
        if (str == null || str.isEmpty()) {
            return map;
        }
        String[] pairs = str.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                try {
                    map.put(DirectoryClassification.valueOf(parts[0]), Integer.parseInt(parts[1]));
                } catch (IllegalArgumentException e) {
                    // Skip invalid entries
                }
            }
        }
        return map;
    }

    private String mapToFileClassificationString(Map<FileClassification, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<FileClassification, Integer> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(entry.getKey().name()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private Map<FileClassification, Integer> stringToFileMap(String str) {
        Map<FileClassification, Integer> map = new HashMap<>();
        if (str == null || str.isEmpty()) {
            return map;
        }
        String[] pairs = str.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                try {
                    map.put(FileClassification.valueOf(parts[0]), Integer.parseInt(parts[1]));
                } catch (IllegalArgumentException e) {
                    // Skip invalid entries
                }
            }
        }
        return map;
    }
}