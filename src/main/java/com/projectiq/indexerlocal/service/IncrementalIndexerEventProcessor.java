package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.event.EventType;
import com.projectiq.indexerlocal.model.event.WatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

/**
 * Processes individual filesystem watch events for incremental indexing.
 * Determines the event type and delegates to the appropriate handler.
 */
@Component
public class IncrementalIndexerEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(IncrementalIndexerEventProcessor.class);

    private static final Set<String> JAVA_EXT = Set.of(".java");
    private static final Set<String> BUILD_EXT = Set.of(".xml", ".gradle", ".kts", ".properties", ".yml", ".yaml");
    private static final Set<String> CONFIG_EXT = Set.of(".properties", ".yml", ".yaml", ".json", ".xml", ".conf", ".cfg", ".ini");
    private static final Set<String> DOC_EXT = Set.of(".md", ".adoc", ".rst", ".txt", ".html", ".pdf");

    private final IncrementalIndexingService incrementalIndexingService;

    public IncrementalIndexerEventProcessor(IncrementalIndexingService incrementalIndexingService) {
        this.incrementalIndexingService = incrementalIndexingService;
    }

    /**
     * Process a single watch event.
     *
     * @param event the watch event to process
     * @return true if the event was processed successfully, false otherwise
     */
    public boolean processEvent(WatchEvent event) {
        if (event == null) {
            return false;
        }

        String repositoryId = event.getRepositoryId();
        Path absolutePath = event.getAbsoluteFilePath();
        EventType eventType = event.getEventType();

        if (repositoryId == null || absolutePath == null || eventType == null) {
            log.warn("[INCREMENTAL-PARSER] Incomplete event: repositoryId={}, path={}, type={}",
                    repositoryId, absolutePath, eventType);
            return false;
        }

        // Check if file exists (for create/modify events) or was deleted
        boolean fileExists = Files.exists(absolutePath) && Files.isRegularFile(absolutePath);

        try {
            switch (eventType) {
                case CREATED:
                    if (fileExists) {
                        return handleFileCreated(repositoryId, absolutePath);
                    } else {
                        log.warn("[INCREMENTAL-PARSER] Created event for non-existent file: {}", absolutePath);
                        return false;
                    }

                case MODIFIED:
                    if (fileExists) {
                        return handleFileModified(repositoryId, absolutePath);
                    } else {
                        log.warn("[INCREMENTAL-PARSER] Modified event for non-existent file: {}", absolutePath);
                        return false;
                    }

                case DELETED:
                    return handleFileDeleted(repositoryId, absolutePath, absolutePath.toString());

                case RENAMED:
                    return handleFileRenamed(repositoryId, absolutePath);

                default:
                    log.debug("[INCREMENTAL-PARSER] Unhandled event type: {} for {}", eventType, absolutePath);
                    return false;
            }
        } catch (Exception e) {
            log.error("[INCREMENTAL-PARSER] Error processing {} event for {}: {}",
                    eventType, absolutePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handle a file creation event.
     */
    boolean handleFileCreated(String repositoryId, Path absolutePath) {
        String fileName = absolutePath.getFileName().toString();
        log.info("[INCREMENTAL-PARSER] File created: {} in repository {}", fileName, repositoryId);

        try {
            String filePath = absolutePath.toAbsolutePath().normalize().toString();
            incrementalIndexingService.indexSingleFile(repositoryId, filePath);
            return true;
        } catch (Exception e) {
            log.error("[INCREMENTAL-PARSER] Failed to index created file {}: {}", absolutePath, e.getMessage());
            return false;
        }
    }

    /**
     * Handle a file modification event.
     */
    boolean handleFileModified(String repositoryId, Path absolutePath) {
        String fileName = absolutePath.getFileName().toString();
        log.info("[INCREMENTAL-PARSER] File modified: {} in repository {}", fileName, repositoryId);

        try {
            // Remove existing index for this file and re-index
            String filePath = absolutePath.toAbsolutePath().normalize().toString();
            incrementalIndexingService.removeFileFromIndex(repositoryId, filePath);
            incrementalIndexingService.indexSingleFile(repositoryId, filePath);
            return true;
        } catch (Exception e) {
            log.error("[INCREMENTAL-PARSER] Failed to re-index modified file {}: {}", absolutePath, e.getMessage());
            return false;
        }
    }

    /**
     * Handle a file deletion event.
     */
    boolean handleFileDeleted(String repositoryId, Path absolutePath, String originalPath) {
        String fileName = absolutePath.getFileName().toString();
        log.info("[INCREMENTAL-PARSER] File deleted: {} from repository {}", fileName, repositoryId);

        try {
            String filePath = absolutePath.toAbsolutePath().normalize().toString();
            incrementalIndexingService.removeFileFromIndex(repositoryId, filePath);
            return true;
        } catch (Exception e) {
            log.error("[INCREMENTAL-PARSER] Failed to remove deleted file {} from index: {}", absolutePath, e.getMessage());
            return false;
        }
    }

    /**
     * Handle a file rename event.
     */
    boolean handleFileRenamed(String repositoryId, Path absolutePath) {
        String fileName = absolutePath.getFileName().toString();
        log.info("[INCREMENTAL-PARSER] File renamed/moved in repository {}: {}", repositoryId, fileName);

        // For rename, we receive the new path. The old path is not directly available from the event.
        // We try to find the file by its new path - if it exists, it's a rename-in (new name),
        // otherwise we treat it as a deletion of the old entry.
        try {
            String filePath = absolutePath.toAbsolutePath().normalize().toString();

            if (Files.exists(absolutePath)) {
                // Check if there's an existing index entry with a different path
                // Since we can't get the old name from the event, we re-index the new file
                // and the old entry will be orphaned until the next full scan
                log.info("[INCREMENTAL-PARSER] Re-indexing renamed file at new location: {}", filePath);
                incrementalIndexingService.indexSingleFile(repositoryId, filePath);
            } else {
                log.info("[INCREMENTAL-PARSER] Renamed file no longer exists at old location: {}", filePath);
                incrementalIndexingService.removeFileFromIndex(repositoryId, filePath);
            }
            return true;
        } catch (Exception e) {
            log.error("[INCREMENTAL-PARSER] Failed to handle rename for {}: {}", absolutePath, e.getMessage());
            return false;
        }
    }

    /**
     * Determine the file type category for the given file path.
     */
    public String determineFileType(String filePath) {
        if (filePath == null) return "UNKNOWN";

        String lower = filePath.toLowerCase();
        for (String ext : JAVA_EXT) {
            if (lower.endsWith(ext)) return "JAVA";
        }
        for (String ext : BUILD_EXT) {
            if (lower.endsWith(ext)) return "BUILD";
        }
        for (String ext : CONFIG_EXT) {
            if (lower.endsWith(ext)) return "CONFIG";
        }
        for (String ext : DOC_EXT) {
            if (lower.endsWith(ext)) return "DOCUMENTATION";
        }
        return "OTHER";
    }
}