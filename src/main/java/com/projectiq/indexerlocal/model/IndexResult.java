package com.projectiq.indexerlocal.model;

import java.util.List;

/**
 * Represents the complete result of indexing a Spring Boot project.
 */
public class IndexResult {
    
    private String projectPath;
    private long totalFiles;
    private long totalClasses;
    private long totalMethods;
    private long totalFields;
    private long totalAnnotations;
    private List<FileIndex> fileIndexes;
    
    public IndexResult() {
    }
    
    public IndexResult(String projectPath, List<FileIndex> fileIndexes) {
        this.projectPath = projectPath;
        this.fileIndexes = fileIndexes;
        this.totalFiles = fileIndexes.size();
        this.totalClasses = fileIndexes.stream().mapToLong(FileIndex::getClassCount).sum();
        this.totalMethods = fileIndexes.stream().mapToLong(FileIndex::getMethodCount).sum();
        this.totalFields = fileIndexes.stream().mapToLong(FileIndex::getFieldCount).sum();
        this.totalAnnotations = fileIndexes.stream().mapToLong(FileIndex::getAnnotationCount).sum();
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public long getTotalMethods() {
        return totalMethods;
    }

    public void setTotalMethods(long totalMethods) {
        this.totalMethods = totalMethods;
    }

    public long getTotalFields() {
        return totalFields;
    }

    public void setTotalFields(long totalFields) {
        this.totalFields = totalFields;
    }

    public long getTotalAnnotations() {
        return totalAnnotations;
    }

    public void setTotalAnnotations(long totalAnnotations) {
        this.totalAnnotations = totalAnnotations;
    }

    public List<FileIndex> getFileIndexes() {
        return fileIndexes;
    }

    public void setFileIndexes(List<FileIndex> fileIndexes) {
        this.fileIndexes = fileIndexes;
    }
}