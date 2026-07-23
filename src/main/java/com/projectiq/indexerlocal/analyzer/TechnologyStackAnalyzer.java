package com.projectiq.indexerlocal.analyzer;

import com.projectiq.indexerlocal.model.TechnologyStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects the technology stack of a repository by analyzing build files,
 * configuration files, directory structure, and file extensions.
 * Does NOT perform dependency analysis or source code indexing.
 */
@Component
public class TechnologyStackAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(TechnologyStackAnalyzer.class);

    // Language to file extension mapping
    private static final Map<String, Set<String>> LANGUAGE_EXTENSIONS = new HashMap<>();

    static {
        LANGUAGE_EXTENSIONS.put("Java", Set.of("java"));
        LANGUAGE_EXTENSIONS.put("Kotlin", Set.of("kt", "kts"));
        LANGUAGE_EXTENSIONS.put("Groovy", Set.of("groovy", "gradle", "gy"));
        LANGUAGE_EXTENSIONS.put("JavaScript", Set.of("js", "mjs", "cjs"));
        LANGUAGE_EXTENSIONS.put("TypeScript", Set.of("ts", "tsx"));
        LANGUAGE_EXTENSIONS.put("HTML", Set.of("html", "htm"));
        LANGUAGE_EXTENSIONS.put("CSS", Set.of("css", "scss", "sass", "less"));
        LANGUAGE_EXTENSIONS.put("SQL", Set.of("sql"));
        LANGUAGE_EXTENSIONS.put("XML", Set.of("xml"));
        LANGUAGE_EXTENSIONS.put("YAML", Set.of("yaml", "yml"));
        LANGUAGE_EXTENSIONS.put("Properties", Set.of("properties", "cfg", "ini", "conf"));
    }

    // Database patterns (dependency names to look for in pom.xml/build.gradle)
    private static final Map<String, List<String>> DATABASE_PATTERNS = new HashMap<>();

    static {
        DATABASE_PATTERNS.put("Oracle", Arrays.asList(
                "oracle", "ojdbc", "orai18n", "oracle.jdbc"
        ));
        DATABASE_PATTERNS.put("PostgreSQL", Arrays.asList(
                "postgresql", "pgjdbc"
        ));
        DATABASE_PATTERNS.put("MySQL", Arrays.asList(
                "mysql", "mysql-connector"
        ));
        DATABASE_PATTERNS.put("SQL Server", Arrays.asList(
                "mssqlserver", "sqljdbc", "microsoft.sqlserver"
        ));
        DATABASE_PATTERNS.put("H2", Arrays.asList(
                "h2", "h2database"
        ));
        DATABASE_PATTERNS.put("SQLite", Arrays.asList(
                "sqlite"
        ));
    }

    // Frontend technology patterns for package.json
    private static final Map<String, List<String>> FRONTEND_PATTERNS = new HashMap<>();

    static {
        FRONTEND_PATTERNS.put("Angular", Arrays.asList(
                "@angular/core", "@angular/common", "angular-cli"
        ));
        FRONTEND_PATTERNS.put("React", Arrays.asList(
                "react", "react-dom"
        ));
        FRONTEND_PATTERNS.put("Vue", Arrays.asList(
                "vue", "vue-router"
        ));
        FRONTEND_PATTERNS.put("Bootstrap", Arrays.asList(
                "bootstrap"
        ));
    }

    /**
     * Detect the complete technology stack for a repository workspace.
     */
    public TechnologyStack detectTechnologyStack(String repositoryId, Path workspacePath) {
        logger.info("Starting technology stack detection for repository: {}", repositoryId);
        logger.debug("Workspace path: {}", workspacePath);

        TechnologyStack techStack = new TechnologyStack();
        techStack.setRepositoryId(repositoryId);

        // Detect languages from file extensions and directory structure
        detectLanguages(techStack, workspacePath);

        // Detect frameworks from build files
        detectFrameworks(techStack, workspacePath);

        // Detect build tools (reuse BuildSystemAnalyzer results)
        detectBuildTools(techStack, workspacePath);

        // Detect databases from configuration and dependencies
        detectDatabases(techStack, workspacePath);

        // Detect testing frameworks
        detectTestingFrameworks(techStack, workspacePath);

        // Detect frontend technologies
        detectFrontendTechnologies(techStack, workspacePath);

        logger.info("Technology stack detection completed for repository: {}", repositoryId);
        logger.info("Detected languages: {}", techStack.getLanguages().stream()
                .map(TechnologyStack.LanguageInfo::getName).collect(Collectors.toList()));
        logger.info("Detected frameworks: {}", techStack.getFrameworks().stream()
                .map(TechnologyStack.FrameworkInfo::getName).collect(Collectors.toList()));
        logger.info("Detected build tools: {}", techStack.getBuildTools());
        logger.info("Detected databases: {}", techStack.getDatabases());
        logger.info("Detected testing frameworks: {}", techStack.getTestingFrameworks());
        logger.info("Detected frontend technologies: {}", techStack.getFrontendTechnologies());

        return techStack;
    }

    /**
     * Detect programming languages based on file extensions.
     */
    private void detectLanguages(TechnologyStack techStack, Path workspacePath) {
        logger.debug("Detecting languages from file extensions...");

        // Recursively find all files and collect extensions
        Set<String> fileExtensions = new HashSet<>();
        try {
            Files.walk(workspacePath)
                    .filter(Files::isRegularFile)
                    .filter(p -> !isIgnoredPath(p))
                    .forEach(p -> {
                        String ext = getFileExtension(p);
                        if (!ext.isEmpty()) {
                            fileExtensions.add(ext.toLowerCase());
                        }
                    });
        } catch (IOException e) {
            logger.warn("Failed to walk workspace directory: {}", workspacePath, e);
        }

        // Map extensions to languages
        for (Map.Entry<String, Set<String>> entry : LANGUAGE_EXTENSIONS.entrySet()) {
            String language = entry.getKey();
            Set<String> extensions = entry.getValue();

            boolean found = extensions.stream().anyMatch(ext -> fileExtensions.contains(ext.toLowerCase()));
            if (found) {
                // Higher confidence when multiple files match
                long matchingFiles = countMatchingExtensions(fileExtensions, extensions);
                double confidence = matchingFiles > 10 ? 1.0 :
                                    matchingFiles > 5 ? 0.9 :
                                    matchingFiles > 0 ? 0.7 : 0.5;
                techStack.addLanguage(language, confidence);
                logger.debug("Found language: {} with {} matching files", language, matchingFiles);
            }
        }
    }

    /**
     * Detect frameworks from build file content.
     */
    private void detectFrameworks(TechnologyStack techStack, Path workspacePath) {
        logger.debug("Detecting frameworks from build files...");

        // Check pom.xml for Spring frameworks
        Path pomXml = workspacePath.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            String pomContent = readFileContent(pomXml);
            if (pomContent != null) {
                // Detect Spring Boot
                if (pomContent.contains("spring-boot-starter-parent") ||
                    pomContent.contains("org.springframework.boot")) {
                    techStack.addFramework("Spring Boot", 0.95);
                    logger.debug("Detected Spring Boot from pom.xml");
                }

                // Detect Spring MVC
                if (pomContent.contains("spring-webmvc") || pomContent.contains("spring-web")) {
                    techStack.addFramework("Spring MVC", 0.9);
                    logger.debug("Detected Spring MVC from pom.xml");
                }

                // Detect Spring Batch
                if (pomContent.contains("spring-batch")) {
                    techStack.addFramework("Spring Batch", 0.9);
                    logger.debug("Detected Spring Batch from pom.xml");
                }

                // Detect Spring Security
                if (pomContent.contains("spring-security") || pomContent.contains("spring-boot-starter-security")) {
                    techStack.addFramework("Spring Security", 0.9);
                    logger.debug("Detected Spring Security from pom.xml");
                }

                // Detect Spring Data JPA
                if (pomContent.contains("spring-data-jpa") || pomContent.contains("spring-boot-starter-data-jpa")) {
                    techStack.addFramework("Spring Data JPA", 0.9);
                    logger.debug("Detected Spring Data JPA from pom.xml");
                }

                // Detect Hibernate
                if (pomContent.contains("hibernate") || pomContent.contains("jakarta.persistence") || pomContent.contains("javax.persistence")) {
                    techStack.addFramework("Hibernate", 0.85);
                    logger.debug("Detected Hibernate from pom.xml");
                }

                // Detect JUnit
                if (pomContent.toLowerCase().contains("junit")) {
                    techStack.addFramework("JUnit", 0.9);
                    logger.debug("Detected JUnit from pom.xml");
                }

                // Detect Mockito
                if (pomContent.toLowerCase().contains("mockito")) {
                    techStack.addFramework("Mockito", 0.9);
                    logger.debug("Detected Mockito from pom.xml");
                }

                // Detect Lombok
                if (pomContent.toLowerCase().contains("lombok")) {
                    techStack.addFramework("Lombok", 0.9);
                    logger.debug("Detected Lombok from pom.xml");
                }

                // Detect MapStruct
                if (pomContent.toLowerCase().contains("mapstruct")) {
                    techStack.addFramework("MapStruct", 0.9);
                    logger.debug("Detected MapStruct from pom.xml");
                }
            }
        }

        // Check build.gradle / build.gradle.kts for Spring frameworks
        Path buildGradle = workspacePath.resolve("build.gradle");
        Path buildGradleKts = workspacePath.resolve("build.gradle.kts");

        if (Files.exists(buildGradle) || Files.exists(buildGradleKts)) {
            Path gradleFile = Files.exists(buildGradle) ? buildGradle : buildGradleKts;
            String gradleContent = readFileContent(gradleFile);
            if (gradleContent != null) {
                // Detect Spring Boot
                if (gradleContent.contains("org.springframework.boot") ||
                    gradleContent.contains("id 'org.springframework.boot'") ||
                    gradleContent.contains("id \"org.springframework.boot\"")) {
                    techStack.addFramework("Spring Boot", 0.95);
                    logger.debug("Detected Spring Boot from Gradle");
                }

                // Check for other Spring dependencies
                if (gradleContent.toLowerCase().contains("spring-data") || gradleContent.toLowerCase().contains("spring-jpa")) {
                    techStack.addFramework("Spring Data JPA", 0.85);
                }

                if (gradleContent.toLowerCase().contains("spring-security")) {
                    techStack.addFramework("Spring Security", 0.9);
                }

                // Detect Hibernate
                if (gradleContent.toLowerCase().contains("hibernate") || gradleContent.contains("jakarta.persistence")) {
                    techStack.addFramework("Hibernate", 0.85);
                }

                // Detect JUnit, Mockito, Lombok
                if (gradleContent.toLowerCase().contains("junit")) {
                    techStack.addFramework("JUnit", 0.9);
                }

                if (gradleContent.toLowerCase().contains("mockito")) {
                    techStack.addFramework("Mockito", 0.9);
                }

                if (gradleContent.toLowerCase().contains("lombok")) {
                    techStack.addFramework("Lombok", 0.9);
                }
            }
        }

        // Check for Spring MVC and Spring Framework from Spring Boot context
        if (!techStack.getFrameworks().stream().anyMatch(f -> f.getName().equals("Spring MVC"))) {
            if (techStack.getFrameworks().stream().anyMatch(f -> f.getName().equals("Spring Boot")) ||
                techStack.getFrameworks().stream().anyMatch(f -> f.getName().equals("Spring Framework"))) {
                techStack.addFramework("Spring MVC", 0.7);
            }
        }

        if (!techStack.getFrameworks().stream().anyMatch(f -> f.getName().equals("Spring Framework"))) {
            if (techStack.getFrameworks().stream().anyMatch(f -> f.getName().equals("Spring Boot"))) {
                techStack.addFramework("Spring Framework", 0.8);
            }
        }

        // Check for Jakarta EE
        if (!techStack.getFrameworks().stream().anyMatch(f -> f.getName().equals("Jakarta EE"))) {
            if (techStack.getFrameworks().stream().anyMatch(f -> f.getName().equals("Hibernate")) ||
                pomXml != null && Files.exists(pomXml) && readFileContent(pomXml) != null &&
                (readFileContent(pomXml).contains("jakarta") || readFileContent(pomXml).contains("javax.annotation"))) {
                techStack.addFramework("Jakarta EE", 0.7);
            }
        }
    }

    /**
     * Detect build tools from available files.
     */
    private void detectBuildTools(TechnologyStack techStack, Path workspacePath) {
        logger.debug("Detecting build tools...");

        if (Files.exists(workspacePath.resolve("pom.xml"))) {
            techStack.addBuildTool("Maven");
            logger.debug("Detected Maven from pom.xml");
        }

        if (Files.exists(workspacePath.resolve("build.gradle.kts"))) {
            techStack.addBuildTool("Gradle Kotlin DSL");
            logger.debug("Detected Gradle Kotlin DSL from build.gradle.kts");
        } else if (Files.exists(workspacePath.resolve("build.gradle"))) {
            techStack.addBuildTool("Gradle");
            logger.debug("Detected Gradle from build.gradle");
        }
    }

    /**
     * Detect databases from configuration and dependency files.
     */
    private void detectDatabases(TechnologyStack techStack, Path workspacePath) {
        logger.debug("Detecting databases...");

        // Check pom.xml for database dependencies
        Path pomXml = workspacePath.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            String pomContent = readFileContent(pomXml);
            if (pomContent != null) {
                for (Map.Entry<String, List<String>> entry : DATABASE_PATTERNS.entrySet()) {
                    for (String pattern : entry.getValue()) {
                        if (pomContent.toLowerCase().contains(pattern.toLowerCase())) {
                            techStack.addDatabase(entry.getKey());
                            logger.debug("Detected database: {} from pom.xml", entry.getKey());
                            break;
                        }
                    }
                }
            }
        }

        // Check application.properties / application.yml for database configuration
        List<Path> configFiles = Arrays.asList(
                workspacePath.resolve("src/main/resources/application.properties"),
                workspacePath.resolve("src/main/resources/application.yml"),
                workspacePath.resolve("src/main/resources/application.yaml")
        );

        for (Path configPath : configFiles) {
            if (Files.exists(configPath)) {
                String configContent = readFileContent(configPath);
                if (configContent != null) {
                    detectDatabasesFromConfig(techStack, configContent);
                }
            }
        }

        // Also check test resources
        List<Path> testConfigFiles = Arrays.asList(
                workspacePath.resolve("src/test/resources/application.properties"),
                workspacePath.resolve("src/test/resources/application.yml"),
                workspacePath.resolve("src/test/resources/application.yaml")
        );

        for (Path configPath : testConfigFiles) {
            if (Files.exists(configPath)) {
                String configContent = readFileContent(configPath);
                if (configContent != null) {
                    detectDatabasesFromConfig(techStack, configContent);
                }
            }
        }
    }

    /**
     * Detect databases from configuration file content.
     */
    private void detectDatabasesFromConfig(TechnologyStack techStack, String configContent) {
        // URL-based detection
        if (configContent.toLowerCase().contains("jdbc:oracle:")) {
            techStack.addDatabase("Oracle");
        }
        if (configContent.toLowerCase().contains("jdbc:postgresql:")) {
            techStack.addDatabase("PostgreSQL");
        }
        if (configContent.toLowerCase().contains("jdbc:mysql:")) {
            techStack.addDatabase("MySQL");
        }
        if (configContent.toLowerCase().contains("jdbc:mssql:") || configContent.toLowerCase().contains("sqlserver:")) {
            techStack.addDatabase("SQL Server");
        }
        if (configContent.toLowerCase().contains("jdbc:h2:")) {
            techStack.addDatabase("H2");
        }
        if (configContent.toLowerCase().contains("jdbc:sqlite:")) {
            techStack.addDatabase("SQLite");
        }

        // Driver class detection
        if (configContent.contains("oracle.jdbc") || configContent.contains("OracleDriver")) {
            techStack.addDatabase("Oracle");
        }
        if (configContent.contains("org.postgresql.Driver")) {
            techStack.addDatabase("PostgreSQL");
        }
        if (configContent.contains("com.mysql") && configContent.contains("Driver")) {
            techStack.addDatabase("MySQL");
        }
        if (configContent.contains("org.h2.Driver")) {
            techStack.addDatabase("H2");
        }
    }

    /**
     * Detect testing frameworks from build files and configuration.
     */
    private void detectTestingFrameworks(TechnologyStack techStack, Path workspacePath) {
        logger.debug("Detecting testing frameworks...");

        // Test framework detection is already handled in detectFrameworks for frameworks
        // Map detected test frameworks to testing category
        Set<String> detectedTestFrameworks = new HashSet<>();
        for (TechnologyStack.FrameworkInfo framework : techStack.getFrameworks()) {
            if (framework.getName().equals("JUnit") || framework.getName().equals("Mockito") || framework.getName().equals("TestNG")) {
                detectedTestFrameworks.add(framework.getName());
            }
        }

        // Check for test directory structure as additional signal
        Path testDir = workspacePath.resolve("src/test");
        if (Files.exists(testDir)) {
            try {
                long javaTestCount = Files.walk(testDir)
                        .filter(p -> p.toString().endsWith(".java"))
                        .count();
                if (javaTestCount > 0 && !detectedTestFrameworks.contains("JUnit")) {
                    // Likely using JUnit if there are test files but no explicit dependency found
                    detectedTestFrameworks.add("JUnit");
                    techStack.addTestingFramework("JUnit");
                    logger.debug("Inferred JUnit from test directory structure");
                }
            } catch (IOException e) {
                logger.warn("Failed to walk test directory: {}", testDir, e);
            }
        }

        // Check for TestNG
        Path pomXml = workspacePath.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            String pomContent = readFileContent(pomXml);
            if (pomContent != null && pomContent.toLowerCase().contains("testng")) {
                techStack.addTestingFramework("TestNG");
                logger.debug("Detected TestNG from pom.xml");
            }
        }

        // Add any missing test frameworks from framework detection
        for (String framework : detectedTestFrameworks) {
            if (!techStack.getTestingFrameworks().contains(framework)) {
                techStack.addTestingFramework(framework);
            }
        }
    }

    /**
     * Detect frontend technologies from package.json and directory structure.
     */
    private void detectFrontendTechnologies(TechnologyStack techStack, Path workspacePath) {
        logger.debug("Detecting frontend technologies...");

        // Check for package.json
        Path packageJson = workspacePath.resolve("package.json");
        if (Files.exists(packageJson)) {
            String packageContent = readFileContent(packageJson);
            if (packageContent != null) {
                for (Map.Entry<String, List<String>> entry : FRONTEND_PATTERNS.entrySet()) {
                    for (String pattern : entry.getValue()) {
                        if (packageContent.contains(pattern)) {
                            techStack.addFrontendTechnology(entry.getKey());
                            logger.debug("Detected frontend technology: {} from package.json", entry.getKey());
                            break;
                        }
                    }
                }
            }
        }

        // Check for frontend directory structure
        List<Path> frontendDirs = Arrays.asList(
                workspacePath.resolve("src/main/webapp"),
                workspacePath.resolve("webapp"),
                workspacePath.resolve("public"),
                workspacePath.resolve("static")
        );

        for (Path dir : frontendDirs) {
            if (Files.exists(dir)) {
                // Check for common frontend file patterns
                try {
                    long hasHtml = Files.walk(dir)
                            .filter(p -> p.toString().endsWith(".html"))
                            .count();
                    if (hasHtml > 0) {
                        logger.debug("Found HTML files in {}, likely has frontend", dir);
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        // Check for Angular/React/Vue specific directories
        Path nodeModules = workspacePath.resolve("node_modules");
        if (Files.exists(nodeModules)) {
            if (Files.exists(nodeModules.resolve("@angular"))) {
                techStack.addFrontendTechnology("Angular");
            } else if (Files.exists(nodeModules.resolve("react"))) {
                techStack.addFrontendTechnology("React");
            } else if (Files.exists(nodeModules.resolve("vue"))) {
                techStack.addFrontendTechnology("Vue");
            }

            if (Files.exists(nodeModules.resolve("bootstrap"))) {
                techStack.addFrontendTechnology("Bootstrap");
            }
        }
    }

    // ==================== Helper Methods ====================

    private String readFileContent(Path filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            logger.warn("Failed to read file: {}", filePath, e);
            return null;
        }
    }

    private String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    private boolean isIgnoredPath(Path path) {
        String pathStr = path.toString();
        return pathStr.contains(".git") ||
               pathStr.contains("node_modules") ||
               pathStr.contains(".idea") ||
               pathStr.contains(".vscode") ||
               pathStr.contains("target") ||
               pathStr.contains("build") ||
               pathStr.contains(".gradle") ||
               pathStr.contains(".m2");
    }

    private long countMatchingExtensions(Set<String> fileExtensions, Set<String> targetExtensions) {
        return fileExtensions.stream()
                .filter(fileExt -> targetExtensions.contains(fileExt.toLowerCase()))
                .count();
    }
}