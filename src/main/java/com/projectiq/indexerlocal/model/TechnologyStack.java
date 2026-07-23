package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the detected technology stack for a repository.
 * Stores detected languages, frameworks, build tools, databases, testing frameworks, and frontend technologies.
 */
public class TechnologyStack {

    private String repositoryId;
    private List<LanguageInfo> languages = new ArrayList<>();
    private List<FrameworkInfo> frameworks = new ArrayList<>();
    private List<String> buildTools = new ArrayList<>();
    private List<String> databases = new ArrayList<>();
    private List<String> testingFrameworks = new ArrayList<>();
    private List<String> frontendTechnologies = new ArrayList<>();
    private LocalDateTime detectedAt;

    public TechnologyStack() {
        this.detectedAt = LocalDateTime.now();
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public List<LanguageInfo> getLanguages() {
        return languages;
    }

    public void setLanguages(List<LanguageInfo> languages) {
        this.languages = languages;
    }

    public void addLanguage(String language, double confidence) {
        this.languages.add(new LanguageInfo(language, confidence));
    }

    public List<FrameworkInfo> getFrameworks() {
        return frameworks;
    }

    public void setFrameworks(List<FrameworkInfo> frameworks) {
        this.frameworks = frameworks;
    }

    public void addFramework(String framework, double confidence) {
        this.frameworks.add(new FrameworkInfo(framework, confidence));
    }

    public List<String> getBuildTools() {
        return buildTools;
    }

    public void setBuildTools(List<String> buildTools) {
        this.buildTools = buildTools;
    }

    public void addBuildTool(String buildTool) {
        if (!this.buildTools.contains(buildTool)) {
            this.buildTools.add(buildTool);
        }
    }

    public List<String> getDatabases() {
        return databases;
    }

    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }

    public void addDatabase(String database) {
        if (!this.databases.contains(database)) {
            this.databases.add(database);
        }
    }

    public List<String> getTestingFrameworks() {
        return testingFrameworks;
    }

    public void setTestingFrameworks(List<String> testingFrameworks) {
        this.testingFrameworks = testingFrameworks;
    }

    public void addTestingFramework(String framework) {
        if (!this.testingFrameworks.contains(framework)) {
            this.testingFrameworks.add(framework);
        }
    }

    public List<String> getFrontendTechnologies() {
        return frontendTechnologies;
    }

    public void setFrontendTechnologies(List<String> frontendTechnologies) {
        this.frontendTechnologies = frontendTechnologies;
    }

    public void addFrontendTechnology(String technology) {
        if (!this.frontendTechnologies.contains(technology)) {
            this.frontendTechnologies.add(technology);
        }
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    @Override
    public String toString() {
        return "TechnologyStack{" +
                "languages=" + languages +
                ", frameworks=" + frameworks +
                ", buildTools=" + buildTools +
                ", databases=" + databases +
                ", testingFrameworks=" + testingFrameworks +
                ", frontendTechnologies=" + frontendTechnologies +
                '}';
    }

    /**
     * Inner class representing a detected language with confidence level.
     */
    public static class LanguageInfo {
        private String name;
        private double confidence;

        public LanguageInfo() {
        }

        public LanguageInfo(String name, double confidence) {
            this.name = name;
            this.confidence = confidence;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return "LanguageInfo{" +
                    "name='" + name + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }

    /**
     * Inner class representing a detected framework with confidence level.
     */
    public static class FrameworkInfo {
        private String name;
        private double confidence;

        public FrameworkInfo() {
        }

        public FrameworkInfo(String name, double confidence) {
            this.name = name;
            this.confidence = confidence;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return "FrameworkInfo{" +
                    "name='" + name + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }
}