package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;

/**
 * Represents the repository summary and statistics.
 * Aggregates metadata from all persisted analyzer outputs.
 */
public class RepositorySummary {

    // Code statistics
    private long totalJavaFiles;
    private long totalPackages;
    private long totalClasses;
    private long totalInterfaces;
    private long totalEnums;
    private long totalRecords;
    private long totalMethods;
    private long totalConstructors;
    private long totalFields;
    private long totalAnnotations;

    // Spring statistics
    private long totalComponents;
    private long totalServices;
    private long totalControllers;
    private long totalRestControllers;
    private long totalRepositories;
    private long totalConfigurationClasses;
    private long totalBeans;

    // REST API statistics
    private long totalRestApiEndpoints;
    private long totalGetEndpoints;
    private long totalPostEndpoints;
    private long totalPutEndpoints;
    private long totalDeleteEndpoints;
    private long totalPatchEndpoints;

    // Dependency statistics
    private long totalDependencies;
    private long compileDependencies;
    private long runtimeDependencies;
    private long testDependencies;

    // Configuration statistics
    private long totalConfigurationFiles;
    private long springConfigurations;
    private long dockerConfigurations;
    private long kubernetesConfigurations;
    private long ciCdConfigurations;

    // Database statistics
    private long databasesDetected;
    private long datasources;
    private long sqlFiles;
    private long migrationScripts;

    // Project health
    private long parsingErrors;
    private long unsupportedFiles;
    private long buildFilesFound;
    private long configFilesFound;

    // Repository information
    private String repositoryId;
    private String repositoryName;
    private LocalDateTime registrationDate;
    private LocalDateTime lastIndexedDate;
    private String currentStatus;

    // Project summary
    private String buildSystem;
    private String projectType;
    private String technologyStack;
    private String detectedDatabases;
    private String springFrameworkUsage;

    // Legacy field
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

    // ==================== Code Statistics Getters/Setters ====================

    public long getTotalJavaFiles() { return totalJavaFiles; }
    public void setTotalJavaFiles(long totalJavaFiles) { this.totalJavaFiles = totalJavaFiles; }

    public long getTotalPackages() { return totalPackages; }
    public void setTotalPackages(long totalPackages) { this.totalPackages = totalPackages; }

    public long getTotalClasses() { return totalClasses; }
    public void setTotalClasses(long totalClasses) { this.totalClasses = totalClasses; }

    public long getTotalInterfaces() { return totalInterfaces; }
    public void setTotalInterfaces(long totalInterfaces) { this.totalInterfaces = totalInterfaces; }

    public long getTotalEnums() { return totalEnums; }
    public void setTotalEnums(long totalEnums) { this.totalEnums = totalEnums; }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getTotalMethods() { return totalMethods; }
    public void setTotalMethods(long totalMethods) { this.totalMethods = totalMethods; }

    public long getTotalConstructors() { return totalConstructors; }
    public void setTotalConstructors(long totalConstructors) { this.totalConstructors = totalConstructors; }

    public long getTotalFields() { return totalFields; }
    public void setTotalFields(long totalFields) { this.totalFields = totalFields; }

    public long getTotalAnnotations() { return totalAnnotations; }
    public void setTotalAnnotations(long totalAnnotations) { this.totalAnnotations = totalAnnotations; }

    // ==================== Spring Statistics Getters/Setters ====================

    public long getTotalComponents() { return totalComponents; }
    public void setTotalComponents(long totalComponents) { this.totalComponents = totalComponents; }

    public long getTotalServices() { return totalServices; }
    public void setTotalServices(long totalServices) { this.totalServices = totalServices; }

    public long getTotalControllers() { return totalControllers; }
    public void setTotalControllers(long totalControllers) { this.totalControllers = totalControllers; }

    public long getTotalRestControllers() { return totalRestControllers; }
    public void setTotalRestControllers(long totalRestControllers) { this.totalRestControllers = totalRestControllers; }

    public long getTotalRepositories() { return totalRepositories; }
    public void setTotalRepositories(long totalRepositories) { this.totalRepositories = totalRepositories; }

    public long getTotalConfigurationClasses() { return totalConfigurationClasses; }
    public void setTotalConfigurationClasses(long totalConfigurationClasses) { this.totalConfigurationClasses = totalConfigurationClasses; }

    public long getTotalBeans() { return totalBeans; }
    public void setTotalBeans(long totalBeans) { this.totalBeans = totalBeans; }

    // ==================== REST API Statistics Getters/Setters ====================

    public long getTotalRestApiEndpoints() { return totalRestApiEndpoints; }
    public void setTotalRestApiEndpoints(long totalRestApiEndpoints) { this.totalRestApiEndpoints = totalRestApiEndpoints; }

    public long getTotalGetEndpoints() { return totalGetEndpoints; }
    public void setTotalGetEndpoints(long totalGetEndpoints) { this.totalGetEndpoints = totalGetEndpoints; }

    public long getTotalPostEndpoints() { return totalPostEndpoints; }
    public void setTotalPostEndpoints(long totalPostEndpoints) { this.totalPostEndpoints = totalPostEndpoints; }

    public long getTotalPutEndpoints() { return totalPutEndpoints; }
    public void setTotalPutEndpoints(long totalPutEndpoints) { this.totalPutEndpoints = totalPutEndpoints; }

    public long getTotalDeleteEndpoints() { return totalDeleteEndpoints; }
    public void setTotalDeleteEndpoints(long totalDeleteEndpoints) { this.totalDeleteEndpoints = totalDeleteEndpoints; }

    public long getTotalPatchEndpoints() { return totalPatchEndpoints; }
    public void setTotalPatchEndpoints(long totalPatchEndpoints) { this.totalPatchEndpoints = totalPatchEndpoints; }

    // ==================== Dependency Statistics Getters/Setters ====================

    public long getTotalDependencies() { return totalDependencies; }
    public void setTotalDependencies(long totalDependencies) { this.totalDependencies = totalDependencies; }

    public long getCompileDependencies() { return compileDependencies; }
    public void setCompileDependencies(long compileDependencies) { this.compileDependencies = compileDependencies; }

    public long getRuntimeDependencies() { return runtimeDependencies; }
    public void setRuntimeDependencies(long runtimeDependencies) { this.runtimeDependencies = runtimeDependencies; }

    public long getTestDependencies() { return testDependencies; }
    public void setTestDependencies(long testDependencies) { this.testDependencies = testDependencies; }

    // ==================== Configuration Statistics Getters/Setters ====================

    public long getTotalConfigurationFiles() { return totalConfigurationFiles; }
    public void setTotalConfigurationFiles(long totalConfigurationFiles) { this.totalConfigurationFiles = totalConfigurationFiles; }

    public long getSpringConfigurations() { return springConfigurations; }
    public void setSpringConfigurations(long springConfigurations) { this.springConfigurations = springConfigurations; }

    public long getDockerConfigurations() { return dockerConfigurations; }
    public void setDockerConfigurations(long dockerConfigurations) { this.dockerConfigurations = dockerConfigurations; }

    public long getKubernetesConfigurations() { return kubernetesConfigurations; }
    public void setKubernetesConfigurations(long kubernetesConfigurations) { this.kubernetesConfigurations = kubernetesConfigurations; }

    public long getCiCdConfigurations() { return ciCdConfigurations; }
    public void setCiCdConfigurations(long ciCdConfigurations) { this.ciCdConfigurations = ciCdConfigurations; }

    // ==================== Database Statistics Getters/Setters ====================

    public long getDatabasesDetected() { return databasesDetected; }
    public void setDatabasesDetected(long databasesDetected) { this.databasesDetected = databasesDetected; }

    public long getDatasources() { return datasources; }
    public void setDatasources(long datasources) { this.datasources = datasources; }

    public long getSqlFiles() { return sqlFiles; }
    public void setSqlFiles(long sqlFiles) { this.sqlFiles = sqlFiles; }

    public long getMigrationScripts() { return migrationScripts; }
    public void setMigrationScripts(long migrationScripts) { this.migrationScripts = migrationScripts; }

    // ==================== Project Health Getters/Setters ====================

    public long getParsingErrors() { return parsingErrors; }
    public void setParsingErrors(long parsingErrors) { this.parsingErrors = parsingErrors; }

    public long getUnsupportedFiles() { return unsupportedFiles; }
    public void setUnsupportedFiles(long unsupportedFiles) { this.unsupportedFiles = unsupportedFiles; }

    public long getBuildFilesFound() { return buildFilesFound; }
    public void setBuildFilesFound(long buildFilesFound) { this.buildFilesFound = buildFilesFound; }

    public long getConfigFilesFound() { return configFilesFound; }
    public void setConfigFilesFound(long configFilesFound) { this.configFilesFound = configFilesFound; }

    // ==================== Repository Information Getters/Setters ====================

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public LocalDateTime getLastIndexedDate() { return lastIndexedDate; }
    public void setLastIndexedDate(LocalDateTime lastIndexedDate) { this.lastIndexedDate = lastIndexedDate; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    // ==================== Project Summary Getters/Setters ====================

    public String getBuildSystem() { return buildSystem; }
    public void setBuildSystem(String buildSystem) { this.buildSystem = buildSystem; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public String getTechnologyStack() { return technologyStack; }
    public void setTechnologyStack(String technologyStack) { this.technologyStack = technologyStack; }

    public String getDetectedDatabases() { return detectedDatabases; }
    public void setDetectedDatabases(String detectedDatabases) { this.detectedDatabases = detectedDatabases; }

    public String getSpringFrameworkUsage() { return springFrameworkUsage; }
    public void setSpringFrameworkUsage(String springFrameworkUsage) { this.springFrameworkUsage = springFrameworkUsage; }

    // ==================== Legacy Getter/Setters ====================

    public long getTotalSpringComponents() { return totalSpringComponents; }
    public void setTotalSpringComponents(long totalSpringComponents) { this.totalSpringComponents = totalSpringComponents; }
}