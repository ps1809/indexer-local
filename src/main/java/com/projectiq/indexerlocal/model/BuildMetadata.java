package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents build system metadata for a repository.
 * Stores detected build system type, extracted metadata, and module information.
 */
public class BuildMetadata {

    private String repositoryId;
    private BuildSystemType buildSystemType;
    private String buildFileName;

    // Maven specific fields
    private String groupId;
    private String artifactId;
    private String version;
    private String packaging;
    private String parentGroupId;
    private String parentArtifactId;
    private String parentVersion;
    private List<String> modules = new ArrayList<>();

    // Gradle specific fields
    private String projectName;
    private String gradleGroup;
    private String javaVersion;

    // Common fields
    private boolean mavenWrapperPresent;
    private boolean gradleWrapperPresent;
    private String projectType; // "Single Module" or "Multi Module"
    private List<ModuleInfo> childModules = new ArrayList<>();
    private LocalDateTime analyzedAt;

    // Extension metadata for future use
    private Map<String, Object> additionalMetadata = new HashMap<>();

    public BuildMetadata() {
        this.analyzedAt = LocalDateTime.now();
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public BuildSystemType getBuildSystemType() {
        return buildSystemType;
    }

    public void setBuildSystemType(BuildSystemType buildSystemType) {
        this.buildSystemType = buildSystemType;
    }

    public String getBuildFileName() {
        return buildFileName;
    }

    public void setBuildFileName(String buildFileName) {
        this.buildFileName = buildFileName;
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

    public String getParentGroupId() {
        return parentGroupId;
    }

    public void setParentGroupId(String parentGroupId) {
        this.parentGroupId = parentGroupId;
    }

    public String getParentArtifactId() {
        return parentArtifactId;
    }

    public void setParentArtifactId(String parentArtifactId) {
        this.parentArtifactId = parentArtifactId;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    public void setParentVersion(String parentVersion) {
        this.parentVersion = parentVersion;
    }

    public List<String> getModules() {
        return modules;
    }

    public void setModules(List<String> modules) {
        this.modules = modules;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getGradleGroup() {
        return gradleGroup;
    }

    public void setGradleGroup(String gradleGroup) {
        this.gradleGroup = gradleGroup;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public boolean isMavenWrapperPresent() {
        return mavenWrapperPresent;
    }

    public void setMavenWrapperPresent(boolean mavenWrapperPresent) {
        this.mavenWrapperPresent = mavenWrapperPresent;
    }

    public boolean isGradleWrapperPresent() {
        return gradleWrapperPresent;
    }

    public void setGradleWrapperPresent(boolean gradleWrapperPresent) {
        this.gradleWrapperPresent = gradleWrapperPresent;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public List<ModuleInfo> getChildModules() {
        return childModules;
    }

    public void setChildModules(List<ModuleInfo> childModules) {
        this.childModules = childModules;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public Map<String, Object> getAdditionalMetadata() {
        return additionalMetadata;
    }

    public void setAdditionalMetadata(Map<String, Object> additionalMetadata) {
        this.additionalMetadata = additionalMetadata;
    }

    /**
     * Get the effective Spring Boot version if configured.
     */
    public String getSpringBootVersion() {
        Object springBootVersion = additionalMetadata.get("springBootVersion");
        return springBootVersion != null ? springBootVersion.toString() : null;
    }

    /**
     * Set the effective Spring Boot version.
     */
    public void setSpringBootVersion(String springBootVersion) {
        additionalMetadata.put("springBootVersion", springBootVersion);
    }

    @Override
    public String toString() {
        return "BuildMetadata{" +
                "buildSystemType=" + buildSystemType +
                ", buildFileName='" + buildFileName + '\'' +
                ", projectType='" + projectType + '\'' +
                '}';
    }

    /**
     * Inner class representing a child module in multi-module projects.
     */
    public static class ModuleInfo {
        private String name;
        private String path;
        private String groupId;
        private String artifactId;
        private String version;

        public ModuleInfo() {
        }

        public ModuleInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
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

        @Override
        public String toString() {
            return "ModuleInfo{" +
                    "name='" + name + '\'' +
                    ", path='" + path + '\'' +
                    '}';
        }
    }
}