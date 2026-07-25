package com.projectiq.indexerlocal.model.event;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Represents a single file system event detected by the Watch Service,
 * carrying necessary metadata for incremental indexing.
 */
public record IndexEvent(
        String repositoryId,
        Path absoluteFilePath,
        String relativePath,
        EventType type,
        Instant timestamp) {

    /**
     * Helper constructor to create an IndexEvent with a null or empty
     * repository ID and path when the context is not fully available.
     * @param type The event type.
     * @return A default IndexEvent instance.
     */
    public static IndexEvent empty(EventType type) {
        // Using current time as fallback timestamp, but typically this will be set accurately during watch processing.
        return new IndexEvent(
                null,
                null,
                null,
                type,
                Instant.now()
        );
    }

    /**
     * Checks if the event should be processed based on filtering rules (e.g., temporary/hidden files).
     * @param path The physical path of the file associated with the event.
     * @return True if the event should be kept, false otherwise.
     */
    public boolean isProcessable(Path path) {
        if (path == null || !java.nio.file.Files.exists(path)) {
            // Cannot process events for non-existent paths (e.g., deleted files are fine, but this check can be stricter)
            return true; 
        }

        String fileName = path.getFileName().toString();
        String parentName = path.getParent().toString();

        // Ignore temporary files or known ignored directories/patterns
        boolean isTemporaryFile = fileName.startsWith(".") && !fileName.equals(".");
        boolean isHiddenDirectory = java.nio.file.Files.isDirectory(path) && fileName.startsWith(".");
        boolean inGitDir = parentName.contains(java.nio.file.Paths.get(".git").toString());
        boolean inIdeaDir = parentName.contains(java.nio.file.Paths.get(".idea").toString());
        boolean inVscodeDir = parentName.contains(java.nio.file.Paths.get(".vscode").toString());

        // Ignore git, idea, vscode directories/files
        if (inGitDir || inIdeaDir || inVscodeDir) {
            return false;
        }
        
        // Note: Directory check for build/target is complex to perform universally, 
        // we rely on explicit configuration or path matching for exclusion if needed.

        return !isTemporaryFile && !isHiddenDirectory;
    }
}