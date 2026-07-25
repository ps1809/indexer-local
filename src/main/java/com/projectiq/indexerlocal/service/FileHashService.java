package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.FileHashRecord;
import com.projectiq.indexerlocal.model.HashProcessingStatistics;
import com.projectiq.indexerlocal.repository.FileHashRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Intelligent Hash Engine that computes and maintains SHA-256 hashes for indexed files.
 * Enables change detection by comparing current hashes against previously persisted hashes.
 * <p>
 * Responsibilities:
 * - Compute SHA-256 hashes for files
 * - Compare current hashes against previous hashes
 * - Detect new, modified, deleted, and unchanged files
 * - Persist updated hashes to the database
 * - Provide processing statistics
 */
@Service
public class FileHashService {

    private static final Logger log = LoggerFactory.getLogger(FileHashService.class);

    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final long ESTIMATED_PARSE_TIME_PER_FILE_MS = 50; // Estimated time saved per skipped file

    private final FileHashRepository fileHashRepository;

    public FileHashService(FileHashRepository fileHashRepository) {
        this.fileHashRepository = fileHashRepository;
    }

    // ==================== Public API ====================

    /**
     * Compute SHA-256 hash for a single file.
     *
     * @param filePath the absolute path to the file
     * @return the SHA-256 hex string, or null if computation fails
     */
    public String computeHash(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                log.warn("[FILE-HASH] File does not exist or is not a regular file: {}", filePath);
                return null;
            }
            return computeSha256(path);
        } catch (IOException e) {
            log.error("[FILE-HASH] Failed to compute hash for {}: {}", filePath, e.getMessage());
            return null;
        }
    }

    /**
     * Compute SHA-256 hash for a single file and return the result with metadata.
     *
     * @param filePath the absolute path to the file
     * @return the FileHashRecord with computed hash, or empty if computation fails
     */
    public Optional<FileHashRecord> computeHashWithMetadata(String repositoryId, String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                log.warn("[FILE-HASH] File does not exist or is not a regular file: {}", filePath);
                return Optional.empty();
            }

            File file = path.toFile();
            String hash = computeSha256(path);
            if (hash == null) {
                return Optional.empty();
            }

            FileHashRecord record = new FileHashRecord(repositoryId, filePath);
            record.setSha256Hash(hash);
            record.setFileSize(file.length());
            record.setLastModified(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(file.lastModified()),
                    ZoneId.systemDefault()));
            record.setLastIndexedAt(LocalDateTime.now());
            record.setProcessingStatus("INDEXED");

            return Optional.of(record);
        } catch (IOException e) {
            log.error("[FILE-HASH] Failed to compute hash for {}: {}", filePath, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Detect changes for a set of current file paths against previously stored hashes.
     * Returns a map of file path to change type: NEW, MODIFIED, UNCHANGED, DELETED.
     *
     * @param repositoryId    the repository identifier
     * @param currentFilePaths the set of current file paths on disk
     * @return map of file path to change type
     */
    public Map<String, String> detectChanges(String repositoryId, Set<String> currentFilePaths) {
        Map<String, String> changes = new LinkedHashMap<>();
        Map<String, FileHashRecord> previousHashes = loadPreviousHashes(repositoryId);

        Set<String> previousPaths = previousHashes.keySet();
        Set<String> untouchedPaths = previousPaths.stream()
                .filter(p -> !currentFilePaths.contains(p))
                .collect(Collectors.toSet());

        // Classify current files
        for (String filePath : currentFilePaths) {
            if (previousPaths.contains(filePath)) {
                FileHashRecord previous = previousHashes.get(filePath);
                File file = new File(filePath);
                long currentSize = file.length();
                LocalDateTime currentModified = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(file.lastModified()),
                        ZoneId.systemDefault());

                // Quick check: if size and last modified are the same, likely unchanged
                if (!previous.isPotentiallyModified(currentModified, currentSize)) {
                    changes.put(filePath, "UNCHANGED");
                } else {
                    // Need to compute hash to confirm
                    changes.put(filePath, "PENDING_HASH_CHECK");
                }
            } else {
                changes.put(filePath, "NEW");
            }
        }

        // Mark deleted files
        for (String deletedPath : untouchedPaths) {
            changes.put(deletedPath, "DELETED");
        }

        return changes;
    }

    /**
     * Process a set of files for a repository: compute hashes, detect changes,
     * persist updated hashes, and return statistics.
     *
     * @param repositoryId    the repository identifier
     * @param currentFilePaths the set of current file paths on disk
     * @return processing statistics
     */
    public HashProcessingStatistics processRepository(String repositoryId, Set<String> currentFilePaths) {
        log.info("[FILE-HASH] Starting hash processing for repository: {} with {} files",
                repositoryId, currentFilePaths.size());

        LocalDateTime startTime = LocalDateTime.now();
        HashProcessingStatistics stats = new HashProcessingStatistics();
        stats.setRepositoryId(repositoryId);
        stats.setStartTime(startTime);
        stats.setStatus("IN_PROGRESS");

        try {
            // Load previous hashes
            Map<String, FileHashRecord> previousHashes = loadPreviousHashes(repositoryId);
            log.info("[FILE-HASH] Loaded {} previous hash records for repository: {}",
                    previousHashes.size(), repositoryId);

            // Detect changes
            Map<String, String> changes = detectChanges(repositoryId, currentFilePaths);

            // Process files
            List<FileHashRecord> recordsToPersist = new ArrayList<>();
            long timeSavedMs = 0;

            for (Map.Entry<String, String> entry : changes.entrySet()) {
                String filePath = entry.getKey();
                String changeType = entry.getValue();

                switch (changeType) {
                    case "NEW":
                        processNewFile(repositoryId, filePath, recordsToPersist, stats);
                        break;

                    case "PENDING_HASH_CHECK":
                        processPendingHashCheck(repositoryId, filePath, previousHashes, recordsToPersist, stats);
                        break;

                    case "UNCHANGED":
                        processUnchangedFile(filePath, previousHashes, recordsToPersist, stats);
                        timeSavedMs += ESTIMATED_PARSE_TIME_PER_FILE_MS;
                        break;

                    case "DELETED":
                        processDeletedFile(repositoryId, filePath, recordsToPersist, stats);
                        break;

                    default:
                        log.warn("[FILE-HASH] Unknown change type: {} for file: {}", changeType, filePath);
                        break;
                }
            }

            // Persist updated hashes
            stats.addTimeSavedMs(timeSavedMs);
            persistHashes(repositoryId, recordsToPersist);

            // Finalize statistics
            LocalDateTime endTime = LocalDateTime.now();
            stats.setEndTime(endTime);
            stats.setProcessingDuration(Duration.between(startTime, endTime));
            stats.setStatus("SUCCESS");

            log.info("[FILE-HASH] Completed hash processing for repository: {}. " +
                            "Processed: {}, Skipped: {}, Changed: {}, Hash Calculations: {}, Time Saved: {}ms",
                    repositoryId, stats.getFilesProcessed(), stats.getFilesSkipped(),
                    stats.getFilesChanged(), stats.getHashCalculations(), stats.getTimeSavedMs());

        } catch (Exception e) {
            stats.setStatus("FAILED");
            stats.setErrorMessage(e.getMessage());
            log.error("[FILE-HASH] Error processing repository {}: {}", repositoryId, e.getMessage(), e);
        }

        return stats;
    }

    /**
     * Validate a single file by computing its hash and comparing with stored hash.
     *
     * @param repositoryId the repository identifier
     * @param filePath     the absolute path to the file
     * @return change type: UNCHANGED, MODIFIED, NEW, or ERROR
     */
    public String validateSingleFile(String repositoryId, String filePath) {
        log.debug("[FILE-HASH] Validating single file: {} for repository: {}", filePath, repositoryId);

        Path path = Paths.get(filePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.warn("[FILE-HASH] File does not exist: {}", filePath);
            return "DELETED";
        }

        try {
            Optional<FileHashRecord> previousOpt = fileHashRepository.findByRepositoryIdAndFilePath(repositoryId, filePath);

            if (previousOpt.isEmpty()) {
                return "NEW";
            }

            FileHashRecord previous = previousOpt.get();
            String currentHash = computeHash(filePath);

            if (currentHash == null) {
                return "ERROR";
            }

            if (previous.hasHashChanged(currentHash)) {
                return "MODIFIED";
            }

            return "UNCHANGED";
        } catch (Exception e) {
            log.error("[FILE-HASH] Error validating file {}: {}", filePath, e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Validate all files in a module (directory) and return change detection results.
     *
     * @param repositoryId the repository identifier
     * @param modulePath   the directory path of the module
     * @return processing statistics for the module
     */
    public HashProcessingStatistics validateModule(String repositoryId, String modulePath) {
        log.info("[FILE-HASH] Validating module: {} for repository: {}", modulePath, repositoryId);

        try {
            Set<String> moduleFiles = Files.walk(Paths.get(modulePath))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(p -> p.toAbsolutePath().toString())
                    .collect(Collectors.toSet());

            return processRepository(repositoryId, moduleFiles);
        } catch (IOException e) {
            log.error("[FILE-HASH] Error walking module path {}: {}", modulePath, e.getMessage());
            HashProcessingStatistics stats = new HashProcessingStatistics();
            stats.setRepositoryId(repositoryId);
            stats.setStatus("FAILED");
            stats.setErrorMessage("Error walking module path: " + e.getMessage());
            return stats;
        }
    }

    /**
     * Validate the entire repository and return change detection results.
     *
     * @param repositoryId the repository identifier
     * @param workspacePath the workspace path of the repository
     * @return processing statistics for the full repository
     */
    public HashProcessingStatistics validateRepository(String repositoryId, String workspacePath) {
        log.info("[FILE-HASH] Validating entire repository: {} at path: {}", repositoryId, workspacePath);

        try {
            Set<String> allFiles = Files.walk(Paths.get(workspacePath))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(p -> p.toAbsolutePath().toString())
                    .collect(Collectors.toSet());

            return processRepository(repositoryId, allFiles);
        } catch (IOException e) {
            log.error("[FILE-HASH] Error walking repository path {}: {}", workspacePath, e.getMessage());
            HashProcessingStatistics stats = new HashProcessingStatistics();
            stats.setRepositoryId(repositoryId);
            stats.setStatus("FAILED");
            stats.setErrorMessage("Error walking repository path: " + e.getMessage());
            return stats;
        }
    }

    /**
     * Get the stored hash record for a specific file.
     *
     * @param repositoryId the repository identifier
     * @param filePath     the absolute path to the file
     * @return the stored hash record, or empty if not found
     */
    public Optional<FileHashRecord> getStoredHash(String repositoryId, String filePath) {
        return fileHashRepository.findByRepositoryIdAndFilePath(repositoryId, filePath);
    }

    /**
     * Get all stored hash records for a repository.
     *
     * @param repositoryId the repository identifier
     * @return list of hash records
     */
    public List<FileHashRecord> getStoredHashes(String repositoryId) {
        return fileHashRepository.findByRepositoryId(repositoryId);
    }

    /**
     * Delete hash records for a specific file.
     *
     * @param repositoryId the repository identifier
     * @param filePath     the absolute path to the file
     */
    public void deleteHash(String repositoryId, String filePath) {
        fileHashRepository.deleteByRepositoryIdAndFilePath(repositoryId, filePath);
    }

    /**
     * Delete all hash records for a repository.
     *
     * @param repositoryId the repository identifier
     */
    public void deleteAllHashes(String repositoryId) {
        fileHashRepository.deleteByRepositoryId(repositoryId);
    }

    // ==================== Private Methods ====================

    /**
     * Compute SHA-256 hash for a file.
     */
    private String computeSha256(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] buffer = new byte[8192];
            int bytesRead;

            try (InputStream is = new FileInputStream(filePath.toFile())) {
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("[FILE-HASH] SHA-256 algorithm not available: {}", e.getMessage());
            throw new RuntimeException("SHA-256 algorithm not available", e);
        } catch (SecurityException e) {
            log.error("[FILE-HASH] Security exception computing hash for {}: {}", filePath, e.getMessage());
            throw new IOException("Security exception computing hash", e);
        }
    }

    /**
     * Load all previous hash records for a repository into a map keyed by file path.
     */
    private Map<String, FileHashRecord> loadPreviousHashes(String repositoryId) {
        List<FileHashRecord> records = fileHashRepository.findByRepositoryId(repositoryId);
        Map<String, FileHashRecord> hashMap = new LinkedHashMap<>();
        for (FileHashRecord record : records) {
            hashMap.put(record.getFilePath(), record);
        }
        return hashMap;
    }

    /**
     * Process a new file: compute hash and add to persistence list.
     */
    private void processNewFile(String repositoryId, String filePath,
                                List<FileHashRecord> recordsToPersist,
                                HashProcessingStatistics stats) {
        log.debug("[FILE-HASH] New file detected: {}", filePath);
        Optional<FileHashRecord> recordOpt = computeHashWithMetadata(repositoryId, filePath);
        if (recordOpt.isPresent()) {
            FileHashRecord record = recordOpt.get();
            record.setProcessingStatus("INDEXED");
            recordsToPersist.add(record);
            stats.incrementFilesProcessed();
            stats.incrementFilesChanged();
            stats.incrementHashCalculations();
        } else {
            log.warn("[FILE-HASH] Failed to compute hash for new file: {}", filePath);
        }
    }

    /**
     * Process a file that needs hash comparison to determine if it changed.
     */
    private void processPendingHashCheck(String repositoryId, String filePath,
                                         Map<String, FileHashRecord> previousHashes,
                                         List<FileHashRecord> recordsToPersist,
                                         HashProcessingStatistics stats) {
        FileHashRecord previous = previousHashes.get(filePath);
        String currentHash = computeHash(filePath);

        if (currentHash == null) {
            log.warn("[FILE-HASH] Failed to compute hash for file: {}", filePath);
            return;
        }

        stats.incrementHashCalculations();

        if (previous.hasHashChanged(currentHash)) {
            log.debug("[FILE-HASH] Modified file detected: {}", filePath);
            File file = new File(filePath);
            previous.setSha256Hash(currentHash);
            previous.setFileSize(file.length());
            previous.setLastModified(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(file.lastModified()),
                    ZoneId.systemDefault()));
            previous.setLastIndexedAt(LocalDateTime.now());
            previous.setProcessingStatus("INDEXED");
            recordsToPersist.add(previous);
            stats.incrementFilesProcessed();
            stats.incrementFilesChanged();
        } else {
            log.trace("[FILE-HASH] Unchanged file confirmed: {}", filePath);
            // Update last indexed timestamp but keep the same hash
            previous.setLastIndexedAt(LocalDateTime.now());
            previous.setProcessingStatus("INDEXED");
            recordsToPersist.add(previous);
            stats.incrementFilesSkipped();
        }
    }

    /**
     * Process an unchanged file: keep existing hash, update timestamp.
     */
    private void processUnchangedFile(String filePath,
                                      Map<String, FileHashRecord> previousHashes,
                                      List<FileHashRecord> recordsToPersist,
                                      HashProcessingStatistics stats) {
        FileHashRecord previous = previousHashes.get(filePath);
        if (previous != null) {
            previous.setLastIndexedAt(LocalDateTime.now());
            previous.setProcessingStatus("INDEXED");
            recordsToPersist.add(previous);
        }
        stats.incrementFilesSkipped();
    }

    /**
     * Process a deleted file: remove from persistence.
     */
    private void processDeletedFile(String repositoryId, String filePath,
                                    List<FileHashRecord> recordsToPersist,
                                    HashProcessingStatistics stats) {
        log.debug("[FILE-HASH] Deleted file detected: {}", filePath);
        fileHashRepository.deleteByRepositoryIdAndFilePath(repositoryId, filePath);
        stats.incrementFilesChanged();
    }

    /**
     * Persist all updated hash records to the database.
     */
    private void persistHashes(String repositoryId, List<FileHashRecord> records) {
        if (records.isEmpty()) {
            log.debug("[FILE-HASH] No hash records to persist for repository: {}", repositoryId);
            return;
        }

        log.info("[FILE-HASH] Persisting {} hash records for repository: {}", records.size(), repositoryId);
        fileHashRepository.saveAll(records);
    }
}