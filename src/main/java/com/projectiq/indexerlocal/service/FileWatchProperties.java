package com.projectiq.indexerlocal.service;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility for filtering watched paths based on configurable exclusion rules.
 */
public class FileWatchProperties {

    private final List<String> ignoredDirectories;
    private final List<Pattern> ignoredDirectoryPatterns;
    private final List<String> ignoredFilePatterns;
    private final int maxQueueSize;
    private final long debounceDelay;
    private final boolean enabled;

    public FileWatchProperties(List<String> ignoredDirectories,
                               List<String> ignoredFilePatterns,
                               int maxQueueSize,
                               long debounceDelay,
                               boolean enabled) {
        this.ignoredDirectories = ignoredDirectories != null ? ignoredDirectories : getDefaultIgnoredDirectories();
        this.ignoredDirectoryPatterns = this.ignoredDirectories.stream()
                .map(dir -> Pattern.compile("^" + Pattern.quote(dir) + "$"))
                .collect(Collectors.toList());
        this.ignoredFilePatterns = ignoredFilePatterns != null ? ignoredFilePatterns : getDefaultIgnoredFilePatterns();
        this.maxQueueSize = maxQueueSize;
        this.debounceDelay = debounceDelay;
        this.enabled = enabled;
    }

    private static List<String> getDefaultIgnoredDirectories() {
        return Arrays.asList(
                ".git", ".idea", ".vscode", ".settings", ".classpath",
                "node_modules", "target", "build", "dist", "out", "bin", "obj",
                ".gradle", ".mvn", ".publish"
        );
    }

    private static List<String> getDefaultIgnoredFilePatterns() {
        return Arrays.asList(
                "*.tmp", "*.temp", "*.swp", "*.swo", "*~", ".*.swp"
        );
    }

    /**
     * Checks if the given directory should be ignored.
     */
    public boolean isIgnoredDirectory(String dirName) {
        return ignoredDirectoryPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(dirName).matches());
    }

    /**
     * Checks if the given file path should be ignored.
     */
    public boolean shouldIgnorePath(Path path) {
        if (path == null) {
            return true;
        }

        // Check each component of the path
        for (int i = 0; i < path.getNameCount(); i++) {
            String name = path.getName(i).toString();
            if (isIgnoredDirectory(name)) {
                return true;
            }
        }

        // Check file name patterns
        String fileName = path.getFileName().toString();
        return matchesIgnoredFilePatterns(fileName);
    }

    /**
     * Checks if the given directory path contains any ignored directories.
     */
    public boolean isInIgnoredDirectory(Path dirPath) {
        if (dirPath == null) {
            return true;
        }
        for (int i = 0; i < dirPath.getNameCount(); i++) {
            String name = dirPath.getName(i).toString();
            if (isIgnoredDirectory(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesIgnoredFilePatterns(String fileName) {
        for (String pattern : ignoredFilePatterns) {
            if (matchesGlob(pattern, fileName)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGlob(String globPattern, String fileName) {
        // Simple glob matching: convert glob to regex
        String regex = globPattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        return fileName.matches(regex);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getDebounceDelay() {
        return debounceDelay;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public List<String> getIgnoredDirectories() {
        return ignoredDirectories;
    }
}