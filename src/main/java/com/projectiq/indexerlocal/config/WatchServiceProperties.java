package com.projectiq.indexerlocal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for the File Watch Service.
 */
@Component
@ConfigurationProperties(prefix = "indexer.watch")
public class WatchServiceProperties {

    /**
     * Patterns to ignore for file names (e.g., temporary files).
     */
    private List<String> ignoreFilePatterns = Arrays.asList(
            "*.tmp",
            "*.temp",
            "*.swp",
            "*.swo",
            "*~",
            ".*.swp"
    );

    /**
     * Directory names to ignore (e.g., .git, .idea).
     */
    private List<String> ignoredDirectories = Arrays.asList(
            ".git",
            ".idea",
            ".vscode",
            ".settings",
            ".classpath",
            "node_modules",
            "target",
            "build",
            "dist",
            "out",
            "bin",
            "obj",
            ".gradle",
            ".mvn",
            ".publish"
    );

    /**
     * Delay in milliseconds before processing events (debounce).
     */
    private long debounceDelay = 500;

    /**
     * Maximum queue size for watch events.
     */
    private int maxQueueSize = 10000;

    /**
     * Whether to enable the watch service.
     */
    private boolean enabled = true;

    public List<String> getIgnoreFilePatterns() {
        return ignoreFilePatterns;
    }

    public void setIgnoreFilePatterns(List<String> ignoreFilePatterns) {
        this.ignoreFilePatterns = ignoreFilePatterns;
    }

    public List<String> getIgnoredDirectories() {
        return ignoredDirectories;
    }

    public void setIgnoredDirectories(List<String> ignoredDirectories) {
        this.ignoredDirectories = ignoredDirectories;
    }

    public long getDebounceDelay() {
        return debounceDelay;
    }

    public void setDebounceDelay(long debounceDelay) {
        this.debounceDelay = debounceDelay;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}