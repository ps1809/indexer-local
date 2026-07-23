package com.projectiq.indexerlocal.analyzer;

import com.projectiq.indexerlocal.model.ConfigurationFile;
import com.projectiq.indexerlocal.model.ConfigurationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Discovers, analyzes, classifies, and extracts metadata from project configuration files.
 * Does NOT parse Java source code, validate configuration values, or analyze security.
 */
@Component
public class ConfigurationAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationAnalyzer.class);

    // Spring configuration files
    private static final List<String> SPRING_CONFIG_FILES = List.of(
            "application.properties", "application.yml", "application.yaml",
            "application-local.properties", "application-dev.properties", "application-prod.properties",
            "bootstrap.properties", "bootstrap.yml"
    );

    // Spring standard files that should be checked for presence
    private static final List<String> STANDARD_SPRING_FILES = List.of(
            "application.properties", "application.yml"
    );

    // Logging configuration files
    private static final List<String> LOGGING_CONFIG_FILES = List.of(
            "logback.xml", "logback-spring.xml", "log4j2.xml"
    );

    // Build configuration files
    private static final List<String> BUILD_CONFIG_FILES = List.of(
            "pom.xml", "build.gradle", "build.gradle.kts", "gradle.properties"
    );

    // Docker configuration files
    private static final List<String> DOCKER_CONFIG_FILES = List.of(
            "Dockerfile", "docker-compose.yml", "docker-compose.yaml", ".dockerignore"
    );

    // Kubernetes configuration files
    private static final Set<String> KUBERNETES_CONFIG_FILES = Set.of(
            "deployment.yaml", "deployment.yml", "service.yaml",
            "ingress.yaml", "configmap.yaml", "secret.yaml"
    );

    // CI/CD configuration files
    private static final List<String> CICD_CONFIG_FILES = List.of(
            "azure-pipelines.yml", "Jenkinsfile", ".gitlab-ci.yml"
    );

    // Environment files
    private static final List<String> ENV_CONFIG_FILES = List.of(
            ".env", ".env.local", ".env.dev", ".env.prod"
    );

    /**
     * Discover and classify all configuration files in a workspace.
     */
    public List<ConfigurationFile> analyzeConfigurations(String repositoryId, Path workspacePath) {
        logger.info("Starting configuration analysis for repository: {}, workspace: {}", repositoryId, workspacePath);

        List<ConfigurationFile> configurationFiles = new ArrayList<>();

        try {
            // Walk the file tree and find configuration files
            Files.walkFileTree(workspacePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                    ConfigurationFile config = classifyConfigurationFile(repositoryId, filePath);
                    if (config != null) {
                        configurationFiles.add(config);
                        logger.info("Discovered configuration file: {} with type: {}", 
                                config.getFileName(), config.getConfigurationType());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path filePath, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Failed to walk file tree for repository: {}", repositoryId, e);
        }

        logger.info("Configuration analysis completed for repository: {}. Found {} configuration files.", 
                repositoryId, configurationFiles.size());

        return configurationFiles;
    }

    /**
     * Calculate configuration statistics from a list of configuration files.
     */
    public Map<String, Object> calculateStatistics(String repositoryId, List<ConfigurationFile> configurationFiles) {
        logger.info("Calculating configuration statistics for repository: {}", repositoryId);

        Map<String, Object> stats = new LinkedHashMap<>();
        
        // Total configuration files
        stats.put("totalConfigurationFiles", configurationFiles.size());

        // Configuration files by type
        Map<String, Integer> filesByType = new LinkedHashMap<>();
        for (ConfigurationType type : ConfigurationType.values()) {
            long count = configurationFiles.stream()
                    .filter(config -> config.getConfigurationType() == type)
                    .count();
            if (count > 0) {
                filesByType.put(type.name(), (int) count);
            }
        }
        stats.put("configurationFilesByType", filesByType);

        // Environment-specific configuration count
        long envSpecificCount = configurationFiles.stream()
                .filter(config -> config.getEnvironmentProfile() != null && !config.getEnvironmentProfile().isEmpty())
                .count();
        stats.put("environmentSpecificConfigurationCount", envSpecificCount);

        // Missing standard Spring configuration files
        Set<String> foundFileNames = configurationFiles.stream()
                .map(ConfigurationFile::getFileName)
                .collect(Collectors.toSet());
        
        List<String> missingStandardFiles = STANDARD_SPRING_FILES.stream()
                .filter(fileName -> !foundFileNames.contains(fileName))
                .collect(Collectors.toList());
        
        stats.put("missingStandardSpringConfigCount", missingStandardFiles.size());
        stats.put("missingStandardSpringConfigs", missingStandardFiles);

        // File format distribution
        Map<String, Integer> filesByFormat = new LinkedHashMap<>();
        for (ConfigurationFile config : configurationFiles) {
            String format = config.getFileFormat() != null ? config.getFileFormat() : "unknown";
            filesByFormat.merge(format, 1, Integer::sum);
        }
        stats.put("filesByFormat", filesByFormat);

        // File size statistics
        long totalSize = configurationFiles.stream()
                .mapToLong(ConfigurationFile::getFileSize)
                .sum();
        stats.put("totalConfigurationFileSizeBytes", totalSize);

        LocalDateTime analyzedAt = LocalDateTime.now(ZoneId.systemDefault());
        stats.put("analyzedAt", analyzedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        logger.info("Configuration statistics calculated for repository: {}. Total files: {}, Types: {}",
                repositoryId, configurationFiles.size(), filesByType.keySet());

        return stats;
    }

    /**
     * Classify a single file as a configuration file and determine its type.
     */
    private ConfigurationFile classifyConfigurationFile(String repositoryId, Path filePath) {
        String fileName = filePath.getFileName().toString();
        ConfigurationType configType = determineConfigurationType(fileName);

        // Skip unknown configuration files
        if (configType == ConfigurationType.UNKNOWN_CONFIGURATION) {
            return null;
        }

        ConfigurationFile config = new ConfigurationFile(repositoryId, filePath);
        config.setConfigurationType(configType);
        config.setFileFormat(determineFileFormat(fileName));
        config.setEnvironmentProfile(determineEnvironmentProfile(fileName, filePath));

        // Get file size
        try {
            config.setFileSize(Files.size(filePath));
        } catch (IOException e) {
            logger.warn("Failed to get file size for: {}", filePath, e);
        }

        // Get last modified timestamp
        try {
            LocalDateTime lastModified = Files.getLastModifiedTime(filePath).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            config.setLastModified(lastModified);
        } catch (IOException e) {
            logger.warn("Failed to get last modified time for: {}", filePath, e);
        }

        return config;
    }

    /**
     * Determine the configuration type based on file name.
     */
    private ConfigurationType determineConfigurationType(String fileName) {
        // Check Spring configuration files
        if (SPRING_CONFIG_FILES.contains(fileName)) {
            return ConfigurationType.APPLICATION_CONFIGURATION;
        }

        // Check logging configuration files
        if (LOGGING_CONFIG_FILES.contains(fileName)) {
            return ConfigurationType.LOGGING_CONFIGURATION;
        }

        // Check build configuration files
        if (BUILD_CONFIG_FILES.contains(fileName)) {
            return ConfigurationType.BUILD_CONFIGURATION;
        }

        // Check Docker configuration files
        if (DOCKER_CONFIG_FILES.contains(fileName)) {
            return ConfigurationType.CONTAINER_CONFIGURATION;
        }

        // Check Kubernetes configuration files
        if (KUBERNETES_CONFIG_FILES.contains(fileName)) {
            return ConfigurationType.KUBERNETES_CONFIGURATION;
        }

        // Check CI/CD configuration files
        if (CICD_CONFIG_FILES.contains(fileName)) {
            return ConfigurationType.CICD_CONFIGURATION;
        }

        // Check environment files
        if (ENV_CONFIG_FILES.contains(fileName)) {
            return ConfigurationType.ENVIRONMENT_CONFIGURATION;
        }

        // Check for CI/CD workflow files (.github/workflows/*.yml or *.yaml)
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            // Check if path contains .github/workflows
            return ConfigurationType.CICD_CONFIGURATION;
        }

        // Check Docker-related YAML files by name patterns
        if (fileName.equalsIgnoreCase("docker-compose.yml") || fileName.equalsIgnoreCase("docker-compose.yaml") 
                || fileName.equals("Dockerfile") || fileName.equals(".dockerignore")) {
            return ConfigurationType.CONTAINER_CONFIGURATION;
        }

        // Kubernetes files often contain "k8s" or are in kubernetes directories
        // For now, check common k8s file name patterns
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            // Check deployment/service/ingress/configmap/secret YAML files
            String baseName = fileName.replaceAll("\\.(yaml|yml)$", "");
            if (baseName.startsWith("deployment") || baseName.startsWith("service") 
                    || baseName.startsWith("ingress") || baseName.startsWith("configmap")
                    || baseName.startsWith("secret")) {
                return ConfigurationType.KUBERNETES_CONFIGURATION;
            }
        }

        return ConfigurationType.UNKNOWN_CONFIGURATION;
    }

    /**
     * Determine the file format based on file name/extension.
     */
    private String determineFileFormat(String fileName) {
        if (fileName.endsWith(".properties")) {
            return "properties";
        } else if (fileName.endsWith(".xml")) {
            return "xml";
        } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return "yaml";
        } else if (fileName.equals("Dockerfile")) {
            return "dockerfile";
        } else if (fileName.equals("Jenkinsfile")) {
            return "groovy";
        } else if (fileName.equals(".env") || fileName.startsWith(".env.")) {
            return "env";
        } else if (fileName.endsWith(".gradle.kts")) {
            return "kotlin-dsl";
        } else if (fileName.equals("gradle.properties")) {
            return "properties";
        } else if (fileName.equals(".dockerignore")) {
            return "ignore-list";
        }
        return "unknown";
    }

    /**
     * Determine the environment profile from the file name.
     */
    private String determineEnvironmentProfile(String fileName, Path filePath) {
        // Check file name for environment indicators
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.contains("-local") || lowerFileName.contains(".local")) {
            return "local";
        } else if (lowerFileName.contains("-dev") || lowerFileName.contains(".dev")) {
            return "dev";
        } else if (lowerFileName.contains("-prod") || lowerFileName.contains(".prod")) {
            return "prod";
        } else if (lowerFileName.contains("-test") || lowerFileName.contains(".test")) {
            return "test";
        } else if (lowerFileName.contains("-staging") || lowerFileName.contains(".staging")) {
            return "staging";
        } else if (fileName.equals(".env") || fileName.equals("application.properties") 
                || fileName.equals("application.yml") || fileName.equals("application.yaml")) {
            return "base";
        }

        // Check parent directory names for environment indicators
        Path parent = filePath.getParent();
        while (parent != null) {
            String parentName = parent.getFileName().toString().toLowerCase();
            if (parentName.contains("local") || parentName.contains("dev") || parentName.contains("prod")) {
                return parentName;
            }
            if (parentName.equals("src") || parentName.equals("main") || parentName.equals("resources")) {
                break;
            }
            parent = parent.getParent();
        }

        return null;
    }
}