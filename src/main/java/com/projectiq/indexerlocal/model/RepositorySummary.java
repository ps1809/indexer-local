package com.projectiq.indexerlocal.model;

/**
 * Represents the repository summary statistics.
 */
public class RepositorySummary {

    private long totalJavaFiles;
    private long totalPackages;
    private long totalClasses;
    private long totalInterfaces;
    private long totalEnums;
    private long totalRecords;
    private long totalMethods;
    private long totalFields;
    private long totalSpringComponents;

    public RepositorySummary() {
    }

    public RepositorySummary(long totalJavaFiles, long totalPackages, long totalClasses,
                             long totalInterfaces, long totalEnums, long totalRecords,
                             long totalMethods, long totalFields, long totalSpringComponents) {
        this.totalJavaFiles = totalJavaFiles;
        this.totalPackages = totalPackages;
        this.totalClasses = totalClasses;
        this.totalInterfaces = totalInterfaces;
        this.totalEnums = totalEnums;
        this.totalRecords = totalRecords;
        this.totalMethods = totalMethods;
        this.totalFields = totalFields;
        this.totalSpringComponents = totalSpringComponents;
    }

    public long getTotalJavaFiles() {
        return totalJavaFiles;
    }

    public void setTotalJavaFiles(long totalJavaFiles) {
        this.totalJavaFiles = totalJavaFiles;
    }

    public long getTotalPackages() {
        return totalPackages;
    }

    public void setTotalPackages(long totalPackages) {
        this.totalPackages = totalPackages;
    }

    public long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public long getTotalInterfaces() {
        return totalInterfaces;
    }

    public void setTotalInterfaces(long totalInterfaces) {
        this.totalInterfaces = totalInterfaces;
    }

    public long getTotalEnums() {
        return totalEnums;
    }

    public void setTotalEnums(long totalEnums) {
        this.totalEnums = totalEnums;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
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

    public long getTotalSpringComponents() {
        return totalSpringComponents;
    }

    public void setTotalSpringComponents(long totalSpringComponents) {
        this.totalSpringComponents = totalSpringComponents;
    }
}