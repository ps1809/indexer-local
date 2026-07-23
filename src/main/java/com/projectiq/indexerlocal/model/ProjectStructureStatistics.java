package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents statistics calculated from project structure analysis.
 */
public class ProjectStructureStatistics {

    private String repositoryId;
    private Integer totalDirectories;
    private Integer totalFiles;
    private Long totalSize;
    private Map<String, Integer> fileCountByExtension;
    private Map<DirectoryClassification, Integer> directoryCountByClassification;
    private Map<FileClassification, Integer> fileCountByClassification;
    private Integer deepestDirectoryLevel;
    private Long largestFileSize;
    private String largestFileName;
    private String largestFilePath;
    private LocalDateTime analyzedAt;

    // Getters and Setters

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Integer getTotalDirectories() {
        return totalDirectories;
    }

    public void setTotalDirectories(Integer totalDirectories) {
        this.totalDirectories = totalDirectories;
    }

    public Integer getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(Integer totalFiles) {
        this.totalFiles = totalFiles;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public Map<String, Integer> getFileCountByExtension() {
        return fileCountByExtension;
    }

    public void setFileCountByExtension(Map<String, Integer> fileCountByExtension) {
        this.fileCountByExtension = fileCountByExtension;
    }

    public Map<DirectoryClassification, Integer> getDirectoryCountByClassification() {
        return directoryCountByClassification;
    }

    public void setDirectoryCountByClassification(Map<DirectoryClassification, Integer> directoryCountByClassification) {
        this.directoryCountByClassification = directoryCountByClassification;
    }

    public Map<FileClassification, Integer> getFileCountByClassification() {
        return fileCountByClassification;
    }

    public void setFileCountByClassification(Map<FileClassification, Integer> fileCountByClassification) {
        this.fileCountByClassification = fileCountByClassification;
    }

    public Integer getDeepestDirectoryLevel() {
        return deepestDirectoryLevel;
    }

    public void setDeepestDirectoryLevel(Integer deepestDirectoryLevel) {
        this.deepestDirectoryLevel = deepestDirectoryLevel;
    }

    public Long getLargestFileSize() {
        return largestFileSize;
    }

    public void setLargestFileSize(Long largestFileSize) {
        this.largestFileSize = largestFileSize;
    }

    public String getLargestFileName() {
        return largestFileName;
    }

    public void setLargestFileName(String largestFileName) {
        this.largestFileName = largestFileName;
    }

    public String getLargestFilePath() {
        return largestFilePath;
    }

    public void setLargestFilePath(String largestFilePath) {
        this.largestFilePath = largestFilePath;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}