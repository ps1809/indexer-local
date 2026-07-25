package com.projectiq.indexerlocal.model.buildsearch;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Represents a build search result entry returned by the Build Search Engine.
 * Contains build system metadata for a repository or module.
 */
@Schema(description = "Build search result entry")
public class BuildSearchResult {

    @Schema(description = "Repository ID")
    private String repositoryId;

    @Schema(description = "Build system type (MAVEN, GRADLE, etc.)")
    private String buildSystem;

    @Schema(description = "Build file path (e.g., pom.xml, build.gradle)")
    private String buildFilePath;

    @Schema(description = "Module name")
    private String moduleName;

    @Schema(description = "Parent module name, if applicable")
    private String parentModule;

    @Schema(description = "Group ID (Maven) or project group")
    private String groupId;

    @Schema(description = "Artifact ID (Maven) or project name")
    private String artifactId;

    @Schema(description = "Project version")
    private String version;

    @Schema(description = "Packaging type (jar, war, pom, etc.)")
    private String packaging;

    @Schema(description = "Project type (Single Module, Multi Module)")
    private String projectType;

    @Schema(description = "List of plugin identifiers")
    private List<String> plugins;

    @Schema(description = "List of dependency coordinates (groupId:artifactId:version)")
    private List<String> dependencies;

    @Schema(description = "List of build profile names")
    private List<String> profiles;

    @Schema(description = "Java version configured")
    private String javaVersion;

    @Schema(description = "Spring Boot version if applicable")
    private String springBootVersion;

    public BuildSearchResult() {
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getBuildSystem() {
        return buildSystem;
    }

    public void setBuildSystem(String buildSystem) {
        this.buildSystem = buildSystem;
    }

    public String getBuildFilePath() {
        return buildFilePath;
    }

    public void setBuildFilePath(String buildFilePath) {
        this.buildFilePath = buildFilePath;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getParentModule() {
        return parentModule;
    }

    public void setParentModule(String parentModule) {
        this.parentModule = parentModule;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<String> plugins) {
        this.plugins = plugins;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getSpringBootVersion() {
        return springBootVersion;
    }

    public void setSpringBootVersion(String springBootVersion) {
        this.springBootVersion = springBootVersion;
    }
}