package com.projectiq.indexerlocal.model;


/**
 * Represents indexing statistics for a Java repository.
 */
/**
 * MVP POJO for Java indexing statistics.
 * Not persisted to DB — returned via API only.
 */
public class JavaIndexingStatistics {

    private long id = 1;
    
    private String repositoryId;
    
    private Long totalJavaFiles;
    private Long totalPackages;
    private Long totalClasses;
    private Long totalInterfaces;
    private Long totalEnums;
    private Long totalRecords;
    private Long totalMethods;
    private Long totalConstructors;
    private Long totalFields;
    private Long totalAnnotations;
    
    private Long indexingDurationMs;
    
    public JavaIndexingStatistics() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Long getTotalJavaFiles() {
        return totalJavaFiles;
    }

    public void setTotalJavaFiles(Long totalJavaFiles) {
        this.totalJavaFiles = totalJavaFiles;
    }

    public Long getTotalPackages() {
        return totalPackages;
    }

    public void setTotalPackages(Long totalPackages) {
        this.totalPackages = totalPackages;
    }

    public Long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(Long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public Long getTotalInterfaces() {
        return totalInterfaces;
    }

    public void setTotalInterfaces(Long totalInterfaces) {
        this.totalInterfaces = totalInterfaces;
    }

    public Long getTotalEnums() {
        return totalEnums;
    }

    public void setTotalEnums(Long totalEnums) {
        this.totalEnums = totalEnums;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Long getTotalMethods() {
        return totalMethods;
    }

    public void setTotalMethods(Long totalMethods) {
        this.totalMethods = totalMethods;
    }

    public Long getTotalConstructors() {
        return totalConstructors;
    }

    public void setTotalConstructors(Long totalConstructors) {
        this.totalConstructors = totalConstructors;
    }

    public Long getTotalFields() {
        return totalFields;
    }

    public void setTotalFields(Long totalFields) {
        this.totalFields = totalFields;
    }

    public Long getTotalAnnotations() {
        return totalAnnotations;
    }

    public void setTotalAnnotations(Long totalAnnotations) {
        this.totalAnnotations = totalAnnotations;
    }

    public Long getIndexingDurationMs() {
        return indexingDurationMs;
    }

    public void setIndexingDurationMs(Long indexingDurationMs) {
        this.indexingDurationMs = indexingDurationMs;
    }
}