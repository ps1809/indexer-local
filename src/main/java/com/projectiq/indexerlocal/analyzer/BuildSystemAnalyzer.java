package com.projectiq.indexerlocal.analyzer;

import com.projectiq.indexerlocal.model.BuildMetadata;
import com.projectiq.indexerlocal.model.BuildSystemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes project build systems (Maven, Gradle) to detect and extract metadata.
 * Only performs build system detection and metadata extraction - no dependency or source code analysis.
 */
@Component
public class BuildSystemAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(BuildSystemAnalyzer.class);

    /**
     * Detect the build system type based on available build files.
     */
    public BuildSystemType detectBuildSystem(Path workspacePath) {
        Path pomXml = workspacePath.resolve("pom.xml");
        Path buildGradle = workspacePath.resolve("build.gradle");
        Path buildGradleKts = workspacePath.resolve("build.gradle.kts");

        if (Files.exists(pomXml)) {
            logger.info("Found Maven build file: pom.xml");
            return BuildSystemType.MAVEN;
        }

        if (Files.exists(buildGradleKts)) {
            logger.info("Found Gradle Kotlin DSL build file: build.gradle.kts");
            return BuildSystemType.GRADLE_KOTLIN_DSL;
        }

        if (Files.exists(buildGradle)) {
            logger.info("Found Gradle build file: build.gradle");
            return BuildSystemType.GRADLE;
        }

        logger.info("No recognized build system detected");
        return BuildSystemType.UNKNOWN;
    }

    /**
     * Detect wrapper presence in the workspace.
     */
    public WrapperInfo detectWrappers(Path workspacePath) {
        WrapperInfo wrapperInfo = new WrapperInfo();

        // Check Maven Wrapper
        Path mavenWrapperScript = workspacePath.resolve("mvnw");
        Path mavenWrapperBat = workspacePath.resolve("mvnw.cmd");
        Path mavenWrapperDir = workspacePath.resolve("mvnw.bat");
        Path mavenWrapperConfig = workspacePath.resolve("maven-wrapper.properties");

        if (Files.exists(mavenWrapperScript) || Files.exists(mavenWrapperBat) || Files.exists(mavenWrapperDir)) {
            wrapperInfo.setMavenWrapperPresent(true);
            logger.info("Maven Wrapper detected");
        }

        if (Files.exists(mavenWrapperConfig)) {
            wrapperInfo.setMavenWrapperPropertiesPresent(true);
            logger.info("Maven Wrapper configuration detected");
        }

        // Check Gradle Wrapper
        Path gradleWrapperScript = workspacePath.resolve("gradlew");
        Path gradleWrapperBat = workspacePath.resolve("gradlew.bat");
        Path gradleWrapperDir = workspacePath.resolve("gradle");

        if (Files.exists(gradleWrapperScript) || Files.exists(gradleWrapperBat)) {
            wrapperInfo.setGradleWrapperPresent(true);
            logger.info("Gradle Wrapper detected");
        }

        if (Files.exists(gradleWrapperDir) && Files.isDirectory(gradleWrapperDir)) {
            Path gradleWrapperJar = gradleWrapperDir.resolve("wrapper").resolve("gradle-wrapper.jar");
            if (Files.exists(gradleWrapperJar)) {
                wrapperInfo.setGradleWrapperJarPresent(true);
                logger.info("Gradle Wrapper JAR detected");
            }
        }

        return wrapperInfo;
    }

    /**
     * Extract Maven metadata from pom.xml.
     */
    public BuildMetadata extractMavenMetadata(Path workspacePath) {
        Path pomXml = workspacePath.resolve("pom.xml");
        if (!Files.exists(pomXml)) {
            logger.warn("pom.xml not found at: {}", pomXml);
            return null;
        }

        logger.info("Extracting Maven metadata from: {}", pomXml);
        BuildMetadata metadata = new BuildMetadata();
        metadata.setBuildSystemType(BuildSystemType.MAVEN);
        metadata.setBuildFileName("pom.xml");

        try (BufferedReader reader = new BufferedReader(new FileReader(pomXml.toFile()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // Remove comments for parsing
                if (!line.trim().startsWith("<!")) {
                    content.append(line).append("\n");
                }
            }

            String pomContent = content.toString();

            // Extract groupId
            String groupId = extractTag(pomContent, "groupId");
            if (groupId == null) {
                // Try to get from parent reference
                groupId = extractProperty(pomContent, "groupId");
            }
            metadata.setGroupId(groupId);

            // Extract artifactId
            String artifactId = extractTag(pomContent, "artifactId");
            metadata.setArtifactId(artifactId);

            // Extract version
            String version = extractTag(pomContent, "version");
            if (version == null) {
                version = extractProperty(pomContent, "version");
            }
            if (version != null && version.startsWith("${") && version.endsWith("}")) {
                // Resolve property reference
                String propertyName = version.substring(2, version.length() - 1);
                version = extractProperty(pomContent, propertyName);
                if (version == null) {
                    version = "${" + propertyName + "}"; // Keep as is if not found
                }
            }
            metadata.setVersion(version);

            // Extract packaging
            String packaging = extractTag(pomContent, "packaging");
            metadata.setPackaging(packaging != null ? packaging : "jar");

            // Extract parent info
            String parentGroupId = extractParentTag(pomContent, "groupId");
            String parentArtifactId = extractParentTag(pomContent, "artifactId");
            String parentVersion = extractParentTag(pomContent, "version");

            if (parentGroupId != null || parentArtifactId != null || parentVersion != null) {
                metadata.setParentGroupId(parentGroupId);
                metadata.setParentArtifactId(parentArtifactId);
                metadata.setParentVersion(parentVersion);
                logger.info("Parent project detected: {}:{}:{}", parentGroupId, parentArtifactId, parentVersion);
            }

            // Extract modules
            List<String> moduleList = extractModules(pomContent);
            if (!moduleList.isEmpty()) {
                metadata.setModules(moduleList);
                metadata.setProjectType("Multi Module");
                logger.info("Detected {} child modules", moduleList.size());
                for (String module : moduleList) {
                    logger.info("  Module: {}", module);
                }
            } else {
                metadata.setProjectType("Single Module");
            }

            // Extract Java version (from properties or maven.compiler.source/target)
            String javaVersion = extractProperty(pomContent, "java.version");
            if (javaVersion == null) {
                javaVersion = extractProperty(pomContent, "maven.compiler.source");
            }
            if (javaVersion == null) {
                // Check for source/target directly in build section
                String sourcePattern = "<source>(.*?)</source>";
                Matcher sourceMatcher = Pattern.compile(sourcePattern).matcher(pomContent);
                if (sourceMatcher.find()) {
                    javaVersion = sourceMatcher.group(1);
                }
            }
            metadata.setJavaVersion(javaVersion);

            // Extract Spring Boot version
            String springBootVersion = extractProperty(pomContent, "spring-boot.version");
            if (springBootVersion == null) {
                // Check in dependencyManagement
                springBootVersion = extractDependencyVersion(pomContent, "spring-boot-starter-parent");
                if (springBootVersion == null) {
                    springBootVersion = extractDependencyVersion(pomContent, "spring-boot-dependencies");
                }
            }
            if (springBootVersion != null) {
                metadata.setSpringBootVersion(springBootVersion);
                logger.info("Spring Boot version: {}", springBootVersion);
            }

        } catch (IOException e) {
            logger.error("Failed to read pom.xml: {}", pomXml, e);
        }

        logger.info("Maven metadata extraction completed for repository");
        return metadata;
    }

    /**
     * Extract Gradle metadata from build.gradle or build.gradle.kts.
     */
    public BuildMetadata extractGradleMetadata(Path workspacePath) {
        Path buildGradle = workspacePath.resolve("build.gradle");
        Path buildGradleKts = workspacePath.resolve("build.gradle.kts");
        Path targetFile = Files.exists(buildGradle) ? buildGradle : buildGradleKts;

        if (!Files.exists(targetFile)) {
            logger.warn("Gradle build file not found at: {}", targetFile);
            return null;
        }

        logger.info("Extracting Gradle metadata from: {}", targetFile);
        BuildMetadata metadata = new BuildMetadata();
        metadata.setBuildSystemType(buildGradleKts.equals(targetFile) ? BuildSystemType.GRADLE_KOTLIN_DSL : BuildSystemType.GRADLE);
        metadata.setBuildFileName(targetFile.getFileName().toString());

        try (BufferedReader reader = new BufferedReader(new FileReader(targetFile.toFile()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            String gradleContent = content.toString();

            // Extract group (plugins block for Gradle Kotlin DSL)
            String group = extractGradleProperty(gradleContent, "group");
            metadata.setGradleGroup(group);

            // Extract version (plugins block for Gradle Kotlin DSL)
            String version = extractGradleProperty(gradleContent, "version");
            metadata.setVersion(version);

            // Extract project name from file name or projects {} block
            String projectName = workspacePath.getFileName() != null ? workspacePath.getFileName().toString() : "unknown";
            metadata.setProjectName(projectName);

            // Extract Java version
            String javaVersion = extractGradleProperty(gradleContent, "javaVersion");
            if (javaVersion == null) {
                javaVersion = extractGradleSourceCompatibility(gradleContent);
            }
            if (javaVersion == null) {
                javaVersion = extractGradleTargetCompatibility(gradleContent);
            }
            metadata.setJavaVersion(javaVersion);

            // Check for multi-project setup
            String includes = extractGradleProperty(gradleContent, "include");
            if (includes != null) {
                List<String> modules = parseCommaSeparatedList(includes);
                if (!modules.isEmpty()) {
                    metadata.setModules(modules);
                    metadata.setProjectType("Multi Module");
                    logger.info("Detected {} included projects", modules.size());
                }
            }

            // Check for plugins
            boolean hasSpringBoot = gradleContent.contains("org.springframework.boot") ||
                    gradleContent.contains("id 'org.springframework.boot'");
            if (hasSpringBoot) {
                String bootVersion = extractGradleProperty(gradleContent, "bootVersion");
                if (bootVersion == null) {
                    bootVersion = extractGradleProperty(gradleContent, "springBootVersion");
                }
                // Check ext {} block
                if (bootVersion == null) {
                    bootVersion = extractExtBlockValue(gradleContent, "bootVersion");
                }
                if (bootVersion == null) {
                    bootVersion = extractExtBlockValue(gradleContent, "springBootVersion");
                }
                if (bootVersion != null) {
                    metadata.setSpringBootVersion(bootVersion);
                    logger.info("Spring Boot version: {}", bootVersion);
                }
            }

        } catch (IOException e) {
            logger.error("Failed to read Gradle build file: {}", targetFile, e);
        }

        // Check for settings.gradle for multi-project
        Path settingsGradle = workspacePath.resolve("settings.gradle");
        Path settingsGradleKts = workspacePath.resolve("settings.gradle.kts");
        if (Files.exists(settingsGradle) || Files.exists(settingsGradleKts)) {
            Path settingsFile = Files.exists(settingsGradle) ? settingsGradle : settingsGradleKts;
            try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile.toFile()))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }

                String settingsContent = content.toString();

                // Extract included projects from settings.gradle
                List<String> includedProjects = extractIncludedProjects(settingsContent);
                if (!includedProjects.isEmpty() && metadata.getProjectType().equals("Single Module")) {
                    metadata.setModules(includedProjects);
                    metadata.setProjectType("Multi Module");
                    logger.info("Detected {} modules from settings.gradle", includedProjects.size());
                }
            } catch (IOException e) {
                logger.warn("Failed to read settings file: {}", settingsFile, e);
            }
        }

        if (metadata.getProjectType() == null) {
            metadata.setProjectType("Single Module");
        }

        logger.info("Gradle metadata extraction completed for repository");
        return metadata;
    }

    /**
     * Extract all modules from a Maven pom.xml including nested child module pom.xml files.
     */
    public List<com.projectiq.indexerlocal.model.BuildMetadata.ModuleInfo> extractDetailedModules(Path workspacePath, List<String> moduleNames) {
        List<com.projectiq.indexerlocal.model.BuildMetadata.ModuleInfo> detailedModules = new ArrayList<>();

        if (moduleNames == null || moduleNames.isEmpty()) {
            return detailedModules;
        }

        for (String moduleName : moduleNames) {
            com.projectiq.indexerlocal.model.BuildMetadata.ModuleInfo moduleInfo =
                    new com.projectiq.indexerlocal.model.BuildMetadata.ModuleInfo(moduleName, " :" + moduleName);

            // Check if the module has its own pom.xml
            Path modulePom = workspacePath.resolve(moduleName).resolve("pom.xml");
            if (Files.exists(modulePom)) {
                try (BufferedReader reader = new BufferedReader(new FileReader(modulePom.toFile()))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().startsWith("<!")) {
                            content.append(line).append("\n");
                        }
                    }

                    String moduleContent = content.toString();
                    String moduleGroupId = extractTag(moduleContent, "groupId");
                    String moduleArtifactId = extractTag(moduleContent, "artifactId");
                    String moduleVersion = extractTag(moduleContent, "version");

                    if (moduleArtifactId != null) {
                        moduleInfo.setArtifactId(moduleArtifactId);
                    }
                    if (moduleGroupId != null) {
                        moduleInfo.setGroupId(moduleGroupId);
                    }
                    if (moduleVersion != null) {
                        moduleInfo.setVersion(moduleVersion);
                    }

                    // Recursively check for nested modules
                    List<String> nestedModules = extractModules(moduleContent);
                    if (!nestedModules.isEmpty()) {
                        logger.info("Found nested modules in {}: {}", moduleName, nestedModules);
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read module pom.xml: {}", modulePom, e);
                }
            }

            detailedModules.add(moduleInfo);
        }

        return detailedModules;
    }

    // ==================== Helper Methods ====================

    private String extractTag(String content, String tagName) {
        String pattern = "<" + tagName + "\\s*>(.*?)</" + tagName + ">";
        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractParentTag(String content, String tagName) {
        String pattern = "<parent>.*?<" + tagName + "\\s*>(.*?)</" + tagName + ">";
        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractProperty(String content, String propertyName) {
        // First check in <properties> block
        String propertiesPattern = "<properties>.*?<" + propertyName + "\\s*>(.*?)</" + propertyName + ">";
        Matcher propertiesMatcher = Pattern.compile(propertiesPattern, Pattern.DOTALL).matcher(content);
        if (propertiesMatcher.find()) {
            return propertiesMatcher.group(1).trim();
        }

        // Then check in <property> elements (e.g., maven.compiler.source)
        String propPattern = "<" + propertyName + ">\\s*(.*?)\\s*</" + propertyName + ">";
        Matcher propMatcher = Pattern.compile(propPattern).matcher(content);
        if (propMatcher.find()) {
            return propMatcher.group(1).trim();
        }

        // Check for <name>propertyName</name> style properties
        String namePattern = "<" + propertyName + ">\\s*(.*?)\\s*</" + propertyName + ">";
        Matcher nameMatcher = Pattern.compile(namePattern).matcher(content);
        if (nameMatcher.find()) {
            return nameMatcher.group(1).trim();
        }

        return null;
    }

    private List<String> extractModules(String content) {
        List<String> modules = new ArrayList<>();

        // Look for <modules><module>...</module></modules> pattern
        String modulePattern = "<modules>.*?<module>(.*?)</module>";
        Matcher matcher = Pattern.compile(modulePattern, Pattern.DOTALL).matcher(content);

        if (matcher.find()) {
            String modulesSection = matcher.group(1);
            String[] moduleArray = modulesSection.split("\n");
            for (String module : moduleArray) {
                String trimmed = module.trim().replaceAll("<[^>]*>", "").trim();
                if (!trimmed.isEmpty() && !"pom".equals(trimmed)) {
                    modules.add(trimmed);
                }
            }
        }

        return modules;
    }

    private String extractGradleProperty(String content, String propertyName) {
        // Match: groupName = 'value' or group = "value" (Groovy DSL)
        String pattern1 = propertyName + "\\s*=\\s*['\"](.*?)['\"]";
        Matcher matcher1 = Pattern.compile(pattern1).matcher(content);
        if (matcher1.find()) {
            return matcher1.group(1);
        }

        // Match: groupName = value (without quotes, for variable references)
        String pattern2 = propertyName + "\\s*=\\s*([a-zA-Z0-9_\\.]+)";
        Matcher matcher2 = Pattern.compile(pattern2).matcher(content);
        if (matcher2.find()) {
            return matcher2.group(1);
        }

        // Kotlin DSL: val groupName by extra { set("value") }
        String pattern3 = propertyName + "\\s*by\\s*extra\\s*\\{\\s*set\\s*\\(\\s*['\"](.*?)['\"]\\s*\\)";
        Matcher matcher3 = Pattern.compile(pattern3).matcher(content);
        if (matcher3.find()) {
            return matcher3.group(1);
        }

        return null;
    }

    private String extractGradleSourceCompatibility(String content) {
        String pattern = "sourceCompatibility\\s*=\\s*['\"]?(.*?)['\"]?";
        Matcher matcher = Pattern.compile(pattern).matcher(content);
        if (matcher.find()) {
            String value = matcher.group(1);
            // Convert Java version format (e.g., "1.8" -> "8", "11" -> "11")
            if (value.startsWith("1.")) {
                return value.substring(2);
            }
            return value;
        }
        return null;
    }

    private String extractGradleTargetCompatibility(String content) {
        String pattern = "targetCompatibility\\s*=\\s*['\"]?(.*?)['\"]?";
        Matcher matcher = Pattern.compile(pattern).matcher(content);
        if (matcher.find()) {
            String value = matcher.group(1);
            if (value.startsWith("1.")) {
                return value.substring(2);
            }
            return value;
        }
        return null;
    }

    private String extractExtBlockValue(String content, String propertyName) {
        // Look for ext { propertyName = 'value' } block
        String pattern = "ext\\s*\\{.*?" + propertyName + "\\s*=\\s*['\"](.*?)['\"]";
        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Look for ext.propertyName = 'value'
        pattern = "ext\\." + propertyName + "\\s*=\\s*['\"](.*?)['\"]";
        matcher = Pattern.compile(pattern).matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private List<String> parseCommaSeparatedList(String input) {
        List<String> result = new ArrayList<>();
        if (input == null) {
            return result;
        }

        String cleaned = input.replaceAll("[\\[\\]' \t]", "");
        String[] parts = cleaned.split(",");
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }

    private List<String> extractIncludedProjects(String settingsContent) {
        List<String> projects = new ArrayList<>();

        // settings.gradle: include ':module1', ':module2'
        String includePattern = "include\\s*\\(\\s*['\"](:[^'\"]+)['\"]";
        Matcher matcher = Pattern.compile(includePattern).matcher(settingsContent);
        while (matcher.find()) {
            String project = matcher.group(1);
            if (project.startsWith(":")) {
                project = project.substring(1); // Remove leading colon
            }
            projects.add(project);
        }

        // settings.gradle.kts: include(":module1", ":module2")
        includePattern = "include\\(\"(:[^\"]+)\"";
        matcher = Pattern.compile(includePattern).matcher(settingsContent);
        while (matcher.find()) {
            String project = matcher.group(1);
            if (project.startsWith(":")) {
                project = project.substring(1);
            }
            projects.add(project);
        }

        // rootProject.name pattern for determining root module name
        return projects;
    }

    private String extractDependencyVersion(String content, String dependencyName) {
        // Look for spring-boot-starter-parent as parent
        String parentPattern = "<parent>.*?<artifactId>" + Pattern.quote(dependencyName) + "</artifactId>";
        Matcher parentMatcher = Pattern.compile(parentPattern, Pattern.DOTALL).matcher(content);
        if (parentMatcher.find()) {
            // Extract version from the same parent block
            String versionPattern = "<version>(.*?)</version>";
            Matcher versionMatcher = Pattern.compile(versionPattern).matcher(
                    content.substring(parentMatcher.start(), Math.min(parentMatcher.end() + 500, content.length())));
            if (versionMatcher.find()) {
                return versionMatcher.group(1).trim();
            }
        }
        return null;
    }

    /**
     * Wrapper detection result.
     */
    public static class WrapperInfo {
        private boolean mavenWrapperPresent;
        private boolean mavenWrapperPropertiesPresent;
        private boolean gradleWrapperPresent;
        private boolean gradleWrapperJarPresent;

        public boolean isMavenWrapperPresent() {
            return mavenWrapperPresent;
        }

        public void setMavenWrapperPresent(boolean mavenWrapperPresent) {
            this.mavenWrapperPresent = mavenWrapperPresent;
        }

        public boolean isMavenWrapperPropertiesPresent() {
            return mavenWrapperPropertiesPresent;
        }

        public void setMavenWrapperPropertiesPresent(boolean mavenWrapperPropertiesPresent) {
            this.mavenWrapperPropertiesPresent = mavenWrapperPropertiesPresent;
        }

        public boolean isGradleWrapperPresent() {
            return gradleWrapperPresent;
        }

        public void setGradleWrapperPresent(boolean gradleWrapperPresent) {
            this.gradleWrapperPresent = gradleWrapperPresent;
        }

        public boolean isGradleWrapperJarPresent() {
            return gradleWrapperJarPresent;
        }

        public void setGradleWrapperJarPresent(boolean gradleWrapperJarPresent) {
            this.gradleWrapperJarPresent = gradleWrapperJarPresent;
        }
    }

    /**
     * Perform complete build analysis for a repository workspace.
     */
    public BuildMetadata analyzeBuildSystem(Path workspacePath) {
        logger.info("Starting build system analysis for workspace: {}", workspacePath);

        // Detect build system type
        BuildSystemType buildSystemType = detectBuildSystem(workspacePath);
        if (buildSystemType == BuildSystemType.UNKNOWN) {
            logger.info("No supported build system found, setting to Unknown");
            BuildMetadata metadata = new BuildMetadata();
            metadata.setBuildSystemType(BuildSystemType.UNKNOWN);
            metadata.setProjectType("Single Module");
            return metadata;
        }

        // Extract metadata based on detected type
        BuildMetadata metadata;
        switch (buildSystemType) {
            case MAVEN:
                metadata = extractMavenMetadata(workspacePath);
                break;
            case GRADLE:
            case GRADLE_KOTLIN_DSL:
                metadata = extractGradleMetadata(workspacePath);
                break;
            default:
                metadata = new BuildMetadata();
                metadata.setBuildSystemType(BuildSystemType.UNKNOWN);
                metadata.setProjectType("Single Module");
        }

        // Detect wrappers
        WrapperInfo wrapperInfo = detectWrappers(workspacePath);
        metadata.setMavenWrapperPresent(wrapperInfo.isMavenWrapperPresent());
        metadata.setGradleWrapperPresent(wrapperInfo.isGradleWrapperPresent());

        // Extract detailed module information if applicable
        if (!metadata.getModules().isEmpty() && workspacePath != null) {
            List<com.projectiq.indexerlocal.model.BuildMetadata.ModuleInfo> detailedModules =
                    extractDetailedModules(workspacePath, metadata.getModules());
            metadata.setChildModules(detailedModules);
        }

        logger.info("Build system analysis completed. Type: {}, Project Type: {}",
                buildSystemType, metadata.getProjectType());

        return metadata;
    }
}