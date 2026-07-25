package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.config.WorkspaceProperties;
import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.IndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for performing incremental indexing of Java repositories.
 * Detects file additions, modifications, deletions, and renames,
 * then updates only the affected metadata.
 */
@Service
public class IncrementalIndexingService {

    private static final Logger log = LoggerFactory.getLogger(IncrementalIndexingService.class);

    private static final Set<String> JAVA_EXTENSIONS = Set.of(".java");
    private static final Set<String> BUILD_EXTENSIONS = Set.of(".xml", ".gradle", ".kts", ".properties");
    private static final Set<String> CONFIG_EXTENSIONS = Set.of(".properties", ".yml", ".yaml", ".json", ".xml", ".conf", ".cfg", ".ini");
    private static final Set<String> DOC_EXTENSIONS = Set.of(".md", ".adoc", ".rst", ".txt", ".html");

    private final JdbcTemplate jdbcTemplate;
    private final IndexRepository indexRepository;
    private final JavaCodeIndexer javaCodeIndexer;
    private final IndexerService indexerService;
    private final WorkspaceProperties workspaceProperties;

    public IncrementalIndexingService(JdbcTemplate jdbcTemplate,
                                      IndexRepository indexRepository,
                                      JavaCodeIndexer javaCodeIndexer,
                                      IndexerService indexerService,
                                      WorkspaceProperties workspaceProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.indexRepository = indexRepository;
        this.javaCodeIndexer = javaCodeIndexer;
        this.indexerService = indexerService;
        this.workspaceProperties = workspaceProperties;
    }

    // ==================== Single File Operations (used by Incremental Parser) ====================

    /**
     * Index a single file for the given repository.
     * Only that file's data is replaced in the index.
     *
     * @param repositoryId the repository identifier
     * @param filePath     the absolute path to the file to index
     */
    public void indexSingleFile(String repositoryId, String filePath) {
        log.info("[INCREMENTAL-PARSER] Indexing single file: {} for repository: {}", filePath, repositoryId);

        Path path = Paths.get(filePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.warn("[INCREMENTAL-PARSER] File does not exist or is not a regular file: {}", filePath);
            return;
        }

        String fileName = path.getFileName().toString();
        String fileType = determineFileType(fileName);
        String relativePath = computeRelativePath(filePath);

        try {
            switch (fileType) {
                case "JAVA":
                    indexSingleJavaFile(repositoryId, filePath, fileName, relativePath);
                    break;
                case "BUILD":
                    indexSingleBuildFile(repositoryId, filePath, fileName, relativePath);
                    break;
                case "CONFIG":
                    indexSingleConfigFile(repositoryId, filePath, fileName, relativePath);
                    break;
                case "DOCUMENTATION":
                case "OTHER":
                default:
                    // For non-indexable files, just track them
                    trackFile(repositoryId, filePath, relativePath, fileName);
                    log.debug("[INCREMENTAL-PARSER] Tracked non-Java file: {}", filePath);
                    break;
            }

            // Update file tracking after indexing
            updateFileTracking(repositoryId, filePath, relativePath, fileName);

            log.info("[INCREMENTAL-PARSER] Successfully indexed file: {}", filePath);
        } catch (Exception e) {
            log.error("[INCREMENTAL-PARSER] Failed to index file {}: {}", filePath, e.getMessage(), e);
        }
    }

    /**
     * Remove a file and all its associated index entries from the database.
     *
     * @param repositoryId the repository identifier
     * @param filePath     the absolute path to the file to remove
     */
    public void removeFileFromIndex(String repositoryId, String filePath) {
        log.info("[INCREMENTAL-PARSER] Removing file from index: {} for repository: {}", filePath, repositoryId);

        try {
            // Delete from file_index and all dependent tables
            indexRepository.deleteFileIndexByFilePath(filePath);

            // Remove file tracking entry
            jdbcTemplate.update("DELETE FROM file_tracking WHERE repository_id = ? AND file_path = ?",
                    repositoryId, filePath);

            log.info("[INCREMENTAL-PARSER] Removed file from index: {}", filePath);
        } catch (Exception e) {
            log.error("[INCREMENTAL-PARSER] Failed to remove file {} from index: {}", filePath, e.getMessage());
        }
    }

    // ==================== Single File Type Indexers ====================

    private void indexSingleJavaFile(String repositoryId, String filePath, String fileName, String relativePath) {
        // First remove existing index for this file if it exists
        indexRepository.deleteFileIndexByFilePath(filePath);

        try {
            // Parse and extract using IndexerService
            Path path = Paths.get(filePath);
            String content = Files.readString(path);
            com.github.javaparser.JavaParser javaParser = new com.github.javaparser.JavaParser();
            com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(content).getResult()
                    .orElseThrow(() -> new IOException("Failed to parse: " + filePath));

            FileIndex fileIndex = new FileIndex(filePath, fileName);

            // Run extractors
            com.projectiq.indexerlocal.extractor.ClassExtractor classExtractor = new com.projectiq.indexerlocal.extractor.ClassExtractor();
            com.projectiq.indexerlocal.extractor.ImportExtractor importExtractor = new com.projectiq.indexerlocal.extractor.ImportExtractor();

            classExtractor.extract(cu, fileIndex);
            importExtractor.extract(cu, fileIndex);

            // Count fields and methods from classes
            long totalFields = fileIndex.getClasses().stream()
                    .mapToLong(c -> c.getFields() != null ? c.getFields().size() : 0)
                    .sum();
            long totalMethods = fileIndex.getClasses().stream()
                    .mapToLong(c -> c.getMethods() != null ? c.getMethods().size() : 0)
                    .sum();
            long totalAnnotations = fileIndex.getClasses().stream()
                    .mapToLong(c -> c.getAnnotations() != null ? c.getAnnotations().size() : 0)
                    .sum();

            fileIndex.setFieldCount(totalFields);
            fileIndex.setMethodCount(totalMethods);
            fileIndex.setAnnotationCount(totalAnnotations);

            // Save to database using IndexRepository's saveIndexResult
            IndexResult indexResult = new IndexResult(repositoryId, List.of(fileIndex));
            indexRepository.saveIndexResult(indexResult);

            log.debug("[INCREMENTAL-PARSER] Indexed Java file: {}", filePath);

            log.info("[INCREMENTAL-PARSER] Successfully indexed Java file: {}", filePath);
        } catch (IOException e) {
            log.error("[INCREMENTAL-PARSER] Failed to read/parse Java file {}: {}", filePath, e.getMessage());
        } catch (Exception e) {
            log.error("[INCREMENTAL-PARSER] Failed to index Java file {}: {}", filePath, e.getMessage(), e);
        }
    }

    private void indexSingleBuildFile(String repositoryId, String filePath, String fileName, String relativePath) {
        log.debug("[INCREMENTAL-PARSER] Build file change detected (tracking only): {}", filePath);
        // Build file changes are tracked but full dependency re-analysis
        // is handled by the existing BuildSystemService on the next full scan
        trackFile(repositoryId, filePath, relativePath, fileName);
    }

    private void indexSingleConfigFile(String repositoryId, String filePath, String fileName, String relativePath) {
        log.debug("[INCREMENTAL-PARSER] Config file change detected (tracking only): {}", filePath);
        // Config file changes are tracked but full config re-analysis
        // is handled by the existing ConfigurationService on the next full scan
        trackFile(repositoryId, filePath, relativePath, fileName);
    }

    // ==================== Main Incremental Indexing Entry Point ====================

    /**
     * Perform incremental indexing for a repository.
     * Returns statistics about the changes detected and files processed.
     */
    public IncrementalIndexingStatistics performIncrementalIndexing(String repositoryId) {
        long startTimeMs = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        // Initialize statistics
        IncrementalIndexingStatistics stats = new IncrementalIndexingStatistics();
        stats.setRepositoryId(repositoryId);
        stats.setStartTime(now);
        stats.setStatus("IN_PROGRESS");
        stats.setFilesAdded(0);
        stats.setFilesModified(0);
        stats.setFilesDeleted(0);
        stats.setFilesUnchanged(0);
        stats.setTotalFilesProcessed(0);

        try {
            log.info("[INCREMENTAL-INDEX] Starting incremental indexing for repository: {}", repositoryId);

            // Step 1: Validate repository exists and has existing index
            validateRepositoryForIncrementalIndexing(repositoryId);

            // Step 2: Get workspace path
            String workspacePath = getWorkspacePath(repositoryId);
            if (workspacePath == null || workspacePath.isEmpty()) {
                throw new IllegalArgumentException("Repository has no workspace path configured: " + repositoryId);
            }

            // Step 3: Load previous file tracking metadata
            Map<String, FileTrackingMetadata> previousMetadata = loadPreviousFileTracking(repositoryId);
            log.info("[INCREMENTAL-INDEX] Loaded {} previous file entries for repository: {}", 
                    previousMetadata.size(), repositoryId);

            // Step 4: Scan current file system and detect changes
            Set<String> currentFilePaths = scanJavaFiles(workspacePath, repositoryId);
            
            // Detect changes (this also updates stats)
            Map<String, FileTrackingMetadata> currentMetadata = detectChanges(repositoryId, previousMetadata, currentFilePaths);

            log.info("[INCREMENTAL-INDEX] Change detection complete for {}: {} added, {} modified, {} deleted, {} unchanged",
                    repositoryId, stats.getFilesAdded(), stats.getFilesModified(), 
                    stats.getFilesDeleted(), stats.getFilesUnchanged());

            // Step 5: Process changed files (index only new/modified)
            Set<String> filesToIndex = currentMetadata.values().stream()
                    .filter(m -> "NEW".equals(m.getIndexStatus()) || "MODIFIED".equals(m.getIndexStatus()))
                    .map(FileTrackingMetadata::getFilePath)
                    .collect(Collectors.toSet());

            log.info("[INCREMENTAL-INDEX] Files to re-index: {} for repository: {}", filesToIndex.size(), repositoryId);

            // Step 6: Remove deleted files from all indexed data
            Set<String> deletedFiles = currentMetadata.values().stream()
                    .filter(m -> "DELETED".equals(m.getIndexStatus()))
                    .map(FileTrackingMetadata::getFilePath)
                    .collect(Collectors.toSet());

            if (!deletedFiles.isEmpty()) {
                removeDeletedFilesFromIndex(repositoryId, deletedFiles);
                log.info("[INCREMENTAL-INDEX] Removed {} deleted files from index for repository: {}", 
                        deletedFiles.size(), repositoryId);
            }

            // Step 7: Index new and modified files
            if (!filesToIndex.isEmpty()) {
                indexChangedFiles(repositoryId, workspacePath, filesToIndex);
            }

            // Step 8: Update file tracking metadata
            saveFileTrackingMetadata(repositoryId, currentMetadata);

            // Step 9: Update indexing timestamp
            updateLastIncrementalIndexingTimestamp(repositoryId, now);

            // Calculate processing time
            long elapsedMs = System.currentTimeMillis() - startTimeMs;
            stats.setEndTime(LocalDateTime.now());
            stats.setProcessingTime(Duration.ofMillis(elapsedMs));
            stats.setTotalFilesProcessed(stats.getFilesAdded() + stats.getFilesModified());
            stats.setStatus("SUCCESS");

            log.info("[INCREMENTAL-INDEX] Completed incremental indexing for repository: {}. " +
                    "Processing Time: {}ms ({}s). Files Added: {}, Modified: {}, Deleted: {}, Unchanged: {}",
                    repositoryId, elapsedMs, elapsedMs / 1000,
                    stats.getFilesAdded(), stats.getFilesModified(), 
                    stats.getFilesDeleted(), stats.getFilesUnchanged());

        } catch (IllegalArgumentException e) {
            stats.setStatus("FAILED");
            stats.setErrorMessage(e.getMessage());
            log.error("[INCREMENTAL-INDEX] Validation error for repository {}: {}", repositoryId, e.getMessage());
            throw e;
        } catch (IOException e) {
            stats.setStatus("FAILED");
            stats.setErrorMessage(e.getMessage());
            log.error("[INCREMENTAL-INDEX] I/O error during incremental indexing for repository {}: {}", 
                    repositoryId, e.getMessage(), e);
            throw new RuntimeException("I/O error during incremental indexing: " + e.getMessage(), e);
        } catch (Exception e) {
            stats.setStatus("FAILED");
            stats.setErrorMessage(e.getMessage());
            log.error("[INCREMENTAL-INDEX] Error during incremental indexing for repository {}: {}", 
                    repositoryId, e.getMessage(), e);
            throw new RuntimeException("Error during incremental indexing: " + e.getMessage(), e);
        }

        return stats;
    }

    // ==================== Validation ====================

    private void validateRepositoryForIncrementalIndexing(String repositoryId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM repositories WHERE id = ?", Long.class, repositoryId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Repository not found: " + repositoryId);
        }

        List<FileIndex> existingFiles = indexRepository.findAllFilesByRepositoryId(repositoryId);
        if (existingFiles == null || existingFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "Repository must have an existing full index before performing incremental indexing: " + repositoryId);
        }

        log.info("[INCREMENTAL-INDEX] Validation passed for repository: {} ({} existing indexed files)",
                repositoryId, existingFiles.size());
    }

    private String getWorkspacePath(String repositoryId) {
        try {
            String sql = "SELECT workspace_path FROM repositories WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, repositoryId);
        } catch (Exception e) {
            log.error("[INCREMENTAL-INDEX] Failed to get workspace path for repository: {}", repositoryId, e);
            return null;
        }
    }

    // ==================== File Scanning ====================

    private Set<String> scanJavaFiles(String workspacePath, String repositoryId) throws IOException {
        Path basePath = Paths.get(workspacePath);
        if (!Files.isDirectory(basePath)) {
            log.warn("[INCREMENTAL-INDEX] Workspace path is not a directory: {}", workspacePath);
            return Collections.emptySet();
        }

        Set<String> filePaths = new HashSet<>();
        
        try (Stream<Path> paths = Files.walk(basePath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> filePaths.add(p.toAbsolutePath().toString()));
        }

        log.info("[INCREMENTAL-INDEX] Scanned workspace: {} - found {} Java files", workspacePath, filePaths.size());
        return filePaths;
    }

    // ==================== Change Detection ====================

    private Map<String, FileTrackingMetadata> detectChanges(String repositoryId,
                                                            Map<String, FileTrackingMetadata> previousMetadata,
                                                            Set<String> currentFilePaths) {
        Map<String, FileTrackingMetadata> currentMetadata = new LinkedHashMap<>();
        Set<String> previousPaths = previousMetadata.keySet();
        Set<String> untouchedPaths = new HashSet<>(previousPaths);

        for (String currentPath : currentFilePaths) {
            String fileName = Paths.get(currentPath).getFileName().toString();
            File file = new File(currentPath);
            
            FileTrackingMetadata metadata = new FileTrackingMetadata(
                    repositoryId, currentPath, "", fileName);

            long lastModifiedMs = file.lastModified();
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(lastModifiedMs),
                    ZoneId.systemDefault());
            metadata.setLastModified(lastModified);
            metadata.setFileSize(file.length());

            String currentChecksum = computeSimpleChecksum(currentPath);
            metadata.setChecksum(currentChecksum);

            if (previousPaths.contains(currentPath)) {
                untouchedPaths.remove(currentPath);
                FileTrackingMetadata previous = previousMetadata.get(currentPath);
                
                if (currentChecksum.equals(previous.getChecksum())) {
                    metadata.setIndexStatus("UNCHANGED");
                    metadata.setId(previous.getId());
                } else {
                    metadata.setIndexStatus("MODIFIED");
                    metadata.setId(previous.getId());
                }
            } else {
                metadata.setIndexStatus("NEW");
            }

            currentMetadata.put(currentPath, metadata);
        }

        // Detect deleted files
        for (String previousPath : untouchedPaths) {
            FileTrackingMetadata metadata = new FileTrackingMetadata(
                    repositoryId, previousPath, 
                    previousMetadata.get(previousPath).getRelativePath(),
                    previousMetadata.get(previousPath).getFileName());
            metadata.setIndexStatus("DELETED");
            metadata.setId(previousMetadata.get(previousPath).getId());
            currentMetadata.put(previousPath, metadata);
        }

        return currentMetadata;
    }

    // ==================== Helper Methods ====================

    private String computeRelativePath(String absolutePath) {
        String basePath = workspaceProperties.getRootDir();
        if (absolutePath.startsWith(basePath)) {
            return absolutePath.substring(basePath.length()).replace("\\", "/");
        }
        return absolutePath;
    }

    private String computeSimpleChecksum(String filePath) {
        try {
            File file = new File(filePath);
            long fileSize = file.length();
            
            StringBuilder content = new StringBuilder();
            try (FileReader reader = new FileReader(file)) {
                char[] buffer = new char[1024];
                int read = reader.read(buffer);
                while (read > 0) {
                    content.append(buffer, 0, read);
                    read = reader.read(buffer);
                }
            }

            String input = fileSize + ":" + content.toString();
            return Integer.toUnsignedString(Math.abs(input.hashCode()), 16);
        } catch (IOException e) {
            log.warn("[INCREMENTAL-INDEX] Failed to compute checksum for {}: {}", filePath, e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Determine file type category for a file name.
     */
    private String determineFileType(String fileName) {
        if (fileName == null) return "OTHER";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".java")) return "JAVA";
        for (String ext : BUILD_EXTENSIONS) {
            if (lower.endsWith(ext) && (lower.contains("pom") || lower.contains("build") || lower.contains("gradle"))) {
                return "BUILD";
            }
        }
        for (String ext : CONFIG_EXTENSIONS) {
            if (lower.endsWith(ext)) return "CONFIG";
        }
        for (String ext : DOC_EXTENSIONS) {
            if (lower.endsWith(ext)) return "DOCUMENTATION";
        }
        return "OTHER";
    }

    // ==================== Index Operations ====================

    private void removeDeletedFilesFromIndex(String repositoryId, Set<String> deletedFiles) {
        for (String filePath : deletedFiles) {
            indexRepository.deleteFileIndexByFilePath(filePath);
            log.debug("[INCREMENTAL-INDEX] Removed indexed data for deleted file: {}", filePath);
        }
    }

    private void indexChangedFiles(String repositoryId, String workspacePath, Set<String> filesToIndex) throws IOException {
        log.info("[INCREMENTAL-INDEX] Indexing {} changed files for repository: {}", 
                filesToIndex.size(), repositoryId);

        int indexedCount = 0;
        for (String filePath : filesToIndex) {
            log.info("[INCREMENTAL-INDEX] Processing changed file: {}", filePath);
            
            try {
                // Remove existing index and re-index
                indexRepository.deleteFileIndexByFilePath(filePath);
                indexSingleFile(repositoryId, filePath);
                indexedCount++;
                log.info("[INCREMENTAL-INDEX] Successfully indexed file: {}", filePath);
            } catch (Exception e) {
                log.error("[INCREMENTAL-INDEX] Failed to index file {}: {}", filePath, e.getMessage());
            }
        }

        log.info("[INCREMENTAL-INDEX] Completed indexing {} changed files for repository: {}", 
                indexedCount, repositoryId);
    }

    // ==================== Persistence ====================

    private Map<String, FileTrackingMetadata> loadPreviousFileTracking(String repositoryId) {
        Map<String, FileTrackingMetadata> metadataMap = new LinkedHashMap<>();
        
        try {
            String sql = "SELECT id, repository_id, file_path, relative_path, file_name, last_modified_ms, file_size, checksum FROM file_tracking WHERE repository_id = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, repositoryId);
            
            for (Map<String, Object> row : rows) {
                FileTrackingMetadata metadata = new FileTrackingMetadata();
                metadata.setId((Long) row.get("id"));
                metadata.setRepositoryId((String) row.get("repository_id"));
                metadata.setFilePath((String) row.get("file_path"));
                Object relativePath = row.get("relative_path");
                metadata.setRelativePath(relativePath != null ? (String) relativePath : "");
                metadata.setFileName((String) row.get("file_name"));
                
                Object lastModifiedMs = row.get("last_modified_ms");
                if (lastModifiedMs != null) {
                    long ms = ((Number) lastModifiedMs).longValue();
                    LocalDateTime lastModified = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(ms), ZoneId.systemDefault());
                    metadata.setLastModified(lastModified);
                }
                
                Object fileSize = row.get("file_size");
                if (fileSize != null) {
                    metadata.setFileSize(((Number) fileSize).longValue());
                }
                
                metadata.setChecksum((String) row.get("checksum"));
                metadataMap.put((String) row.get("file_path"), metadata);
            }
        } catch (Exception e) {
            log.warn("[INCREMENTAL-INDEX] Failed to load file tracking metadata: {}", e.getMessage());
        }
        
        return metadataMap;
    }

    private void saveFileTrackingMetadata(String repositoryId, Map<String, FileTrackingMetadata> metadataMap) {
        try {
            jdbcTemplate.update("DELETE FROM file_tracking WHERE repository_id = ?", repositoryId);

            String sql = "INSERT INTO file_tracking (repository_id, file_path, relative_path, file_name, last_modified_ms, file_size, checksum, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            for (Map.Entry<String, FileTrackingMetadata> entry : metadataMap.entrySet()) {
                String filePath = entry.getKey();
                FileTrackingMetadata metadata = entry.getValue();
                
                if ("DELETED".equals(metadata.getIndexStatus())) {
                    continue;
                }

                Long lastModifiedMs = null;
                if (metadata.getLastModified() != null) {
                    lastModifiedMs = java.time.Instant.from(
                            metadata.getLastModified().atZone(java.time.ZoneId.systemDefault())).toEpochMilli();
                }

                jdbcTemplate.update(sql,
                        repositoryId,
                        metadata.getFilePath(),
                        metadata.getRelativePath(),
                        metadata.getFileName(),
                        lastModifiedMs,
                        metadata.getFileSize(),
                        metadata.getChecksum(),
                        Timestamp.valueOf(LocalDateTime.now()));
            }

            log.info("[INCREMENTAL-INDEX] Saved {} file tracking entries for repository: {}", 
                    metadataMap.values().stream().filter(m -> !"DELETED".equals(m.getIndexStatus())).count(), repositoryId);
        } catch (Exception e) {
            log.error("[INCREMENTAL-INDEX] Failed to save file tracking metadata: {}", e.getMessage(), e);
        }
    }

    private void updateFileTracking(String repositoryId, String filePath, String relativePath, String fileName) {
        try {
            File file = new File(filePath);
            long lastModifiedMs = file.lastModified();
            String checksum = computeSimpleChecksum(filePath);

            // Remove old tracking entry and insert new one
            jdbcTemplate.update("DELETE FROM file_tracking WHERE repository_id = ? AND file_path = ?",
                    repositoryId, filePath);
            jdbcTemplate.update(
                    "INSERT INTO file_tracking (repository_id, file_path, relative_path, file_name, last_modified_ms, file_size, checksum, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    repositoryId, filePath, relativePath, fileName, lastModifiedMs, file.length(), checksum,
                    Timestamp.valueOf(LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("[INCREMENTAL-INDEX] Failed to update file tracking for {}: {}", filePath, e.getMessage());
        }
    }

    private void trackFile(String repositoryId, String filePath, String relativePath, String fileName) {
        updateFileTracking(repositoryId, filePath, relativePath, fileName);
    }

    private void updateLastIncrementalIndexingTimestamp(String repositoryId, LocalDateTime timestamp) {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM indexing_stats WHERE repository_id = ?", Long.class, repositoryId);

            if (count != null && count > 0) {
                jdbcTemplate.update(
                        "UPDATE indexing_stats SET last_incremental_index_at = ?, incremental_stats_json = ? WHERE repository_id = ?",
                        Timestamp.valueOf(timestamp),
                        constructStatsJson(repositoryId),
                        repositoryId);
            } else {
                jdbcTemplate.update(
                        "INSERT INTO indexing_stats (repository_id, last_incremental_index_at) VALUES (?, ?)",
                        repositoryId,
                        Timestamp.valueOf(timestamp));
            }

            log.info("[INCREMENTAL-INDEX] Updated last incremental indexing timestamp for repository: {}", repositoryId);
        } catch (Exception e) {
            log.warn("[INCREMENTAL-INDEX] Failed to update indexing timestamp: {}", e.getMessage());
        }
    }

    private String constructStatsJson(String repositoryId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT incremental_stats_json FROM indexing_stats WHERE repository_id = ?", repositoryId);
            if (!rows.isEmpty() && rows.get(0).get("incremental_stats_json") != null) {
                return (String) rows.get(0).get("incremental_stats_json");
            }
        } catch (Exception e) {
            // Ignore, return empty
        }
        return "{}";
    }

    /**
     * Get the last incremental indexing status for a repository.
     */
    public Map<String, Object> getLastIncrementalIndexingStatus(String repositoryId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM repositories WHERE id = ?", Long.class, repositoryId);
            if (count == null || count == 0) {
                result.put("error", "Repository not found: " + repositoryId);
                return result;
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT last_incremental_index_at, incremental_stats_json FROM indexing_stats WHERE repository_id = ?",
                    repositoryId);

            if (!rows.isEmpty()) {
                Map<String, Object> statsEntry = rows.get(0);
                result.put("repositoryId", repositoryId);
                
                Object lastIndexAt = statsEntry.get("last_incremental_index_at");
                if (lastIndexAt instanceof java.sql.Timestamp) {
                    result.put("lastIncrementalIndexAt", ((java.sql.Timestamp) lastIndexAt).toLocalDateTime());
                } else if (lastIndexAt instanceof java.util.Date) {
                    result.put("lastIncrementalIndexAt", ((java.util.Date) lastIndexAt).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                }
                
                String statsJson = (String) statsEntry.get("incremental_stats_json");
                if (statsJson != null && !statsJson.isEmpty()) {
                    result.put("statistics", parseStatsJson(statsJson));
                }
            } else {
                result.put("repositoryId", repositoryId);
                result.put("lastIncrementalIndexAt", null);
                result.put("statistics", null);
                result.put("message", "No incremental indexing has been performed for this repository");
            }

        } catch (Exception e) {
            result.put("error", "Failed to retrieve incremental indexing status: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> parseStatsJson(String json) {
        Map<String, Object> stats = new HashMap<>();
        if (json != null && !json.equals("{}")) {
            try {
                json = json.replaceAll("[{}\"]", "");
                String[] parts = json.split(",");
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length == 2) {
                        stats.put(kv[0].trim(), parseStatsValue(kv[1].trim()));
                    }
                }
            } catch (Exception e) {
                log.warn("[INCREMENTAL-INDEX] Failed to parse stats JSON: {}", json);
            }
        }
        return stats;
    }

    private Object parseStatsValue(String value) {
        try {
            if (value.matches("-?\\d+")) {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Not a number, return as string
        }
        return value;
    }
}