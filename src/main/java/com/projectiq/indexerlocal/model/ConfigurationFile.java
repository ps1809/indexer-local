package com.projectiq.indexerlocal.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Represents a discovered project configuration file with its metadata.
 */
public class ConfigurationFile {

    private String repositoryId;
    private Path filePath;
    private String fileName;
    private long fileSize;
    private ConfigurationType configurationType;
    private String fileFormat;  // properties, xml, yml, yaml, json, sh, etc.
    private String environmentProfile;  // local, dev, prod, test, etc.
    private LocalDateTime lastModified;
    private LocalDateTime analyzedAt;

    public ConfigurationFile() {
        this.analyzedAt = LocalDateTime.now();
    }

    public ConfigurationFile(String repositoryId, Path filePath) {
        this.repositoryId = repositoryId;
        this.filePath = filePath;
        this.fileName = filePath.getFileName().toString();
        this.lastModified = LocalDateTime.now();
        this.analyzedAt = LocalDateTime.now();
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public ConfigurationType getConfigurationType() {
        return configurationType;
    }

    public void setConfigurationType(ConfigurationType configurationType) {
        this.configurationType = configurationType;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getEnvironmentProfile() {
        return environmentProfile;
    }

    public void setEnvironmentProfile(String environmentProfile) {
        this.environmentProfile = environmentProfile;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    @Override
    public String toString() {
        return "ConfigurationFile{" +
                "fileName='" + fileName + '\'' +
                ", configurationType=" + configurationType +
                ", fileFormat='" + fileFormat + '\'' +
                ", environmentProfile='" + environmentProfile + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}