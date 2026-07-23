package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;

/**
 * Represents a single dependency found in a project's build file.
 */
public class Dependency {

    private String repositoryId;
    private String groupId;
    private String artifactId;
    private String version;
    private DependencyType type;
    private boolean optional;
    private String typeClassifier;  // Maven dependency type (jar, war, etc.)
    private String classifier;       // Maven classifier
    private String configuration;    // Gradle configuration (compileOnly, implementation, etc.)
    private boolean internal;        // Internal vs external dependency
    private LocalDateTime analyzedAt;

    public Dependency() {
        this.analyzedAt = LocalDateTime.now();
    }

    public Dependency(String groupId, String artifactId, String version, DependencyType type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.optional = false;
        this.analyzedAt = LocalDateTime.now();
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
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

    public DependencyType getType() {
        return type;
    }

    public void setType(DependencyType type) {
        this.type = type;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getTypeClassifier() {
        return typeClassifier;
    }

    public void setTypeClassifier(String typeClassifier) {
        this.typeClassifier = typeClassifier;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", type=" + type +
                ", configuration='" + configuration + '\'' +
                '}';
    }

    /**
     * Generate a unique key for this dependency to detect duplicates.
     */
    public String getUniqueKey() {
        return groupId + ":" + artifactId;
    }
}