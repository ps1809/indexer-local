package com.projectiq.indexerlocal.analyzer;

import com.projectiq.indexerlocal.model.BuildMetadata;
import com.projectiq.indexerlocal.model.BuildSystemType;
import com.projectiq.indexerlocal.model.Dependency;
import com.projectiq.indexerlocal.model.DependencyStatistics;
import com.projectiq.indexerlocal.model.DependencyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analyzes project build files to discover, classify, and extract dependency information.
 * Only parses build files (pom.xml, build.gradle, build.gradle.kts) - does not download
 * dependencies or resolve transitive dependencies.
 */
@Component
public class DependencyAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalyzer.class);

    /**
     * Analyze all dependencies for a repository workspace.
     */
    public List<Dependency> analyzeDependencies(String repositoryId, Path workspacePath, BuildMetadata buildMetadata) {
        logger.info("Starting dependency analysis for repository: {}, build system: {}",
                repositoryId, buildMetadata.getBuildSystemType());

        List<Dependency> dependencies = new ArrayList<>();

        switch (buildMetadata.getBuildSystemType()) {
            case MAVEN:
                dependencies = extractMavenDependencies(repositoryId, workspacePath, buildMetadata);
                break;
            case GRADLE:
            case GRADLE_KOTLIN_DSL:
                dependencies = extractGradleDependencies(repositoryId, workspacePath, buildMetadata);
                break;
            default:
                logger.warn("Unsupported build system type: {}", buildMetadata.getBuildSystemType());
                break;
        }

        logger.info("Dependency analysis completed for repository: {}. Found {} dependencies.",
                repositoryId, dependencies.size());

        return dependencies;
    }

    /**
     * Calculate dependency statistics from a list of dependencies.
     */
    public DependencyStatistics calculateStatistics(String repositoryId, List<Dependency> dependencies, BuildMetadata buildMetadata) {
        logger.info("Calculating dependency statistics for repository: {}", repositoryId);

        DependencyStatistics stats = new DependencyStatistics();
        stats.setRepositoryId(repositoryId);
        stats.setTotalDependencies(dependencies.size());

        // Count by scope/type
        Map<DependencyType, Integer> byScope = new HashMap<>();
        for (Dependency dep : dependencies) {
            if (dep.getType() != null) {
                byScope.merge(dep.getType(), 1, Integer::sum);
            }
        }
        stats.setDependenciesByScope(byScope);

        // Count by classifier/type (Maven) or configuration (Gradle)
        Map<String, Integer> byType = new HashMap<>();
        for (Dependency dep : dependencies) {
            String typeKey;
            if (buildMetadata.getBuildSystemType() == BuildSystemType.MAVEN && dep.getTypeClassifier() != null) {
                typeKey = dep.getTypeClassifier();
            } else if (dep.getConfiguration() != null) {
                typeKey = dep.getConfiguration();
            } else {
                typeKey = "unknown";
            }
            byType.merge(typeKey, 1, Integer::sum);
        }
        stats.setDependenciesByType(byType);

        // Detect duplicates (same groupId:artifactId with different versions)
        Map<String, List<Dependency>> depMap = new LinkedHashMap<>();
        for (Dependency dep : dependencies) {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            depMap.computeIfAbsent(key, k -> new ArrayList<>()).add(dep);
        }
        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<Dependency>> entry : depMap.entrySet()) {
            Set<String> versions = entry.getValue().stream()
                    .map(Dependency::getVersion)
                    .filter(v -> v != null)
                    .collect(Collectors.toSet());
            if (versions.size() > 1) {
                duplicates.add(entry.getKey());
                logger.info("Duplicate dependency detected: {} with versions: {}", entry.getKey(), versions);
            }
        }
        stats.setDuplicateDependencies(duplicates);

        // Count missing versions
        long missingVersions = dependencies.stream()
                .filter(dep -> dep.getVersion() == null || dep.getVersion().isEmpty())
                .count();
        stats.setMissingVersionsCount((int) missingVersions);

        // Count snapshot dependencies
        long snapshots = dependencies.stream()
                .filter(dep -> dep.getVersion() != null && dep.getVersion().contains("-SNAPSHOT"))
                .count();
        stats.setSnapshotDependenciesCount((int) snapshots);

        // Count internal vs external
        String projectGroupId = buildMetadata.getGroupId();
        int internalCount = 0;
        int externalCount = 0;
        if (projectGroupId != null && !projectGroupId.isEmpty()) {
            for (Dependency dep : dependencies) {
                if (dep.getGroupId() != null && dep.getGroupId().startsWith(projectGroupId)) {
                    internalCount++;
                } else {
                    externalCount++;
                }
            }
        } else {
            // If we can't determine, count all as external
            externalCount = dependencies.size();
        }

        stats.setInternalDependenciesCount(internalCount);
        stats.setExternalDependenciesCount(externalCount);

        logger.info("Dependency statistics calculated for repository: {}. Total: {}, Internal: {}, External: {}",
                repositoryId, stats.getTotalDependencies(), internalCount, externalCount);

        return stats;
    }

    // ==================== Maven Dependency Extraction ====================

    private List<Dependency> extractMavenDependencies(String repositoryId, Path workspacePath, BuildMetadata buildMetadata) {
        List<Dependency> dependencies = new ArrayList<>();

        // Parse root pom.xml
        Path pomXml = workspacePath.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            logger.info("Extracting Maven dependencies from: {}", pomXml);
            String pomContent = readFileContent(pomXml);
            if (pomContent != null) {
                // Remove comments
                String cleanContent = removeXmlComments(pomContent);

                // Extract dependencies from <dependencies> section
                List<Dependency> deps = parseMavenDependenciesFromXml(cleanContent, repositoryId, buildMetadata);
                dependencies.addAll(deps);

                // Extract BOM imports from dependencyManagement
                List<Dependency> bomDeps = extractMavenBomImports(cleanContent, repositoryId, buildMetadata);
                dependencies.addAll(bomDeps);

                // Extract property names for version resolution
                List<String> propertyNames = extractPropertyNamesFromProperties(cleanContent);
            }
        }

        // Parse parent pom.xml
        if (buildMetadata.getParentArtifactId() != null) {
            logger.info("Parent project detected: {}:{}:{}",
                    buildMetadata.getParentGroupId(),
                    buildMetadata.getParentArtifactId(),
                    buildMetadata.getParentVersion());
        }

        // Parse child module poms in multi-module projects
        if (buildMetadata.getProjectType() != null && "Multi Module".equals(buildMetadata.getProjectType())) {
            for (String module : buildMetadata.getModules()) {
                Path modulePom = workspacePath.resolve(module).resolve("pom.xml");
                if (Files.exists(modulePom)) {
                    logger.info("Extracting Maven dependencies from module: {}", module);
                    String moduleContent = readFileContent(modulePom);
                    if (moduleContent != null) {
                        String cleanModuleContent = removeXmlComments(moduleContent);
                        List<Dependency> moduleDeps = parseMavenDependenciesFromXml(cleanModuleContent, repositoryId, buildMetadata);
                        dependencies.addAll(moduleDeps);
                    }
                }
            }
        }

        return dependencies;
    }

    private List<Dependency> parseMavenDependenciesFromXml(String pomContent, String repositoryId, BuildMetadata buildMetadata) {
        List<Dependency> dependencies = new ArrayList<>();

        // Remove dependencyManagement sections to avoid double-counting with BOM imports
        String pomWithoutMgmt = removeDependencyManagementFromPom(pomContent);

        // Extract the <dependencies>...</dependencies> block
        Pattern depsPattern = Pattern.compile("<dependencies>(.*?)</dependencies>", Pattern.DOTALL);
        Matcher matcher = depsPattern.matcher(pomWithoutMgmt);

        if (!matcher.find()) {
            return dependencies;
        }

        String depsBlock = matcher.group(1);

        // Extract individual dependencies
        Matcher depInnerMatcher = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL).matcher(depsBlock);

        while (depInnerMatcher.find()) {
            String depBlock = depInnerMatcher.group(1);
            Dependency dep = parseSingleMavenDependency(depBlock, repositoryId);
            if (dep != null) {
                dependencies.add(dep);
            }
        }

        return dependencies;
    }

    private List<Dependency> extractMavenBomImports(String pomContent, String repositoryId, BuildMetadata buildMetadata) {
        List<Dependency> dependencies = new ArrayList<>();

        // Find dependencyManagement section at the root level (not inside parent)
        Pattern mgmtPattern = Pattern.compile("<dependencyManagement>(.*?)</dependencyManagement>", Pattern.DOTALL);
        Matcher mgmtMatcher = mgmtPattern.matcher(pomContent);

        if (!mgmtMatcher.find()) {
            return dependencies;
        }

        String mgmtSection = mgmtMatcher.group(1);

        // Check if BOM is in parent section
        boolean isParent = pomContent.indexOf("<parent>") < pomContent.indexOf("<dependencyManagement>");
        if (isParent) {
            Pattern parentPattern = Pattern.compile("<parent>(.*?)</parent>", Pattern.DOTALL);
            Matcher parentMatcher = parentPattern.matcher(pomContent);
            if (parentMatcher.find()) {
                String parentSection = parentMatcher.group(1);
                if (Pattern.compile("<artifactId>.*?spring-boot-dependencies.*?</artifactId>").matcher(parentSection).find()
                        || Pattern.compile("<artifactId>.*?bom</artifactId>").matcher(parentSection).find()) {
                    logger.info("BOM import detected in parent project");
                }
            }
        }

        // Extract <dependency> blocks from dependencyManagement for BOM imports
        Matcher depMatcher = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL).matcher(mgmtSection);
        while (depMatcher.find()) {
            String depBlock = depMatcher.group(1);
            String groupId = extractXmlTag(depBlock, "groupId");
            String artifactId = extractXmlTag(depBlock, "artifactId");
            String version = extractXmlTag(depBlock, "version");

            // Skip if groupId is same as parent (imported via BOM)
            if (artifactId != null && !artifactId.isEmpty()) {
                Dependency dep = new Dependency(groupId, artifactId, version, DependencyType.COMPILE);
                dep.setRepositoryId(repositoryId);
                dep.setTypeClassifier("bom-import");
                dependencies.add(dep);
            }
        }

        return dependencies;
    }

    private String removeDependencyManagementFromPom(String pomContent) {
        // Remove only the first (root-level) <dependencyManagement> section
        Pattern parentPattern = Pattern.compile("<parent>(.*?)</parent>", Pattern.DOTALL);
        Matcher parentMatcher = parentPattern.matcher(pomContent);
        int afterParent = 0;
        if (parentMatcher.find()) {
            afterParent = parentMatcher.end();
        }

        // Find dependencyManagement after the parent section
        String contentAfterParent = pomContent.substring(afterParent);
        Pattern mgmtPattern = Pattern.compile("<dependencyManagement>[^<]*>(.*?)</dependencyManagement>", Pattern.DOTALL);
        Matcher mgmtMatcher = mgmtPattern.matcher(contentAfterParent);

        if (mgmtMatcher.find()) {
            String before = pomContent.substring(0, afterParent + mgmtMatcher.start());
            String after = pomContent.substring(afterParent + mgmtMatcher.end());
            return before + after;
        }

        return pomContent;
    }

    private Dependency parseSingleMavenDependency(String depBlock, String repositoryId) {
        String groupId = extractXmlTag(depBlock, "groupId");
        String artifactId = extractXmlTag(depBlock, "artifactId");
        String version = extractXmlTag(depBlock, "version");
        String scope = extractXmlTag(depBlock, "scope");
        String optionalStr = extractXmlTag(depBlock, "optional");
        String type = extractXmlTag(depBlock, "type");
        String classifier = extractXmlTag(depBlock, "classifier");

        if (artifactId == null || artifactId.isEmpty()) {
            return null;
        }

        Dependency dep = new Dependency(groupId, artifactId, version, mavenScopeToDependencyType(scope));
        dep.setRepositoryId(repositoryId);
        dep.setOptional("true".equalsIgnoreCase(optionalStr));
        dep.setTypeClassifier(type != null ? type : "jar");
        dep.setClassifier(classifier);

        return dep;
    }

    private DependencyType mavenScopeToDependencyType(String scope) {
        if (scope == null || scope.isEmpty()) {
            return DependencyType.COMPILE;
        }
        switch (scope.toLowerCase()) {
            case "compile":
                return DependencyType.COMPILE;
            case "runtime":
                return DependencyType.RUNTIME;
            case "test":
                return DependencyType.TEST;
            case "provided":
                return DependencyType.PROVIDED;
            case "system":
                return DependencyType.PROVIDED;
            default:
                return DependencyType.COMPILE;
        }
    }

    // ==================== Gradle Dependency Extraction ====================

    private List<Dependency> extractGradleDependencies(String repositoryId, Path workspacePath, BuildMetadata buildMetadata) {
        List<Dependency> dependencies = new ArrayList<>();

        // Determine the build file
        Path buildFile;
        boolean isKotlinDsl = buildMetadata.getBuildSystemType() == BuildSystemType.GRADLE_KOTLIN_DSL;
        if (isKotlinDsl) {
            buildFile = workspacePath.resolve("build.gradle.kts");
        } else {
            buildFile = workspacePath.resolve("build.gradle");
        }

        if (!Files.exists(buildFile)) {
            logger.warn("Gradle build file not found: {}", buildFile);
            return dependencies;
        }

        logger.info("Extracting Gradle dependencies from: {}", buildFile);
        String buildContent = readFileContent(buildFile);
        if (buildContent == null) {
            return dependencies;
        }

        // Remove comments
        String cleanContent = removeGradleComments(buildContent);

        // Extract dependencies from dependencies {} block
        List<Dependency> deps = parseGradleDependenciesFromBlock(cleanContent, repositoryId, buildMetadata, workspacePath);
        dependencies.addAll(deps);

        return dependencies;
    }

    private List<Dependency> parseGradleDependenciesFromBlock(String content, String repositoryId, BuildMetadata buildMetadata, Path workspacePath) {
        List<Dependency> dependencies = new ArrayList<>();

        // Find the dependencies {} block
        Pattern depsPattern = Pattern.compile("dependencies\\s*\\{((?:[^{}]|\\{[^}]*\\})*)\\}", Pattern.DOTALL);
        Matcher matcher = depsPattern.matcher(content);

        if (!matcher.find()) {
            logger.info("No dependencies block found in Gradle build file");
            return dependencies;
        }

        String depsSection = matcher.group(1);

        // Parse each configuration
        // Patterns for Groovy DSL: configuration 'artifact:version:type:classifier'
        // Patterns for Kotlin DSL: configuration("artifact:version:type:classifier")
        Pattern groovyDepPattern = Pattern.compile("(\\w+)\\s+['\"]([^'\"]+)['\"]");
        Matcher groovyMatcher = groovyDepPattern.matcher(depsSection);

        while (groovyMatcher.find()) {
            String configuration = groovyMatcher.group(1);
            String coords = groovyMatcher.group(2);
            Dependency dep = parseGradleDependency(coords, configuration, repositoryId, buildMetadata, workspacePath);
            if (dep != null) {
                dependencies.add(dep);
            }
        }

        return dependencies;
    }

    private Dependency parseGradleDependency(String coords, String configuration, String repositoryId, BuildMetadata buildMetadata, Path workspacePath) {
        Dependency dep = new Dependency();
        dep.setRepositoryId(repositoryId);
        dep.setConfiguration(configuration);

        // Parse coordinates: groupId:artifactId:version<classifier>@<type> or just artifactId:version
        String[] parts = coords.split(":");
        if (parts.length < 2) {
            logger.warn("Invalid dependency coordinates: {}", coords);
            return null;
        }

        dep.setGroupId(parts.length >= 1 ? parts[0] : null);
        dep.setArtifactId(parts.length >= 2 ? parts[1] : null);

        if (dep.getArtifactId() == null || dep.getArtifactId().isEmpty()) {
            return null;
        }

        // Version might contain @classifier@type suffix
        if (parts.length >= 3) {
            String versionPart = parts[2];
            if (versionPart.contains("@")) {
                String[] verType = versionPart.split("@");
                dep.setVersion(verType[0]);
            } else {
                dep.setVersion(versionPart);
            }

            if (parts.length >= 4) {
                if (parts.length >= 5 && parts[4].endsWith("@")) {
                    dep.setClassifier(parts[3]);
                    dep.setTypeClassifier(parts[4].replace("@", ""));
                } else {
                    dep.setTypeClassifier(parts[3]);
                }
            }
        }

        // Resolve version from extBlocks or variables if it starts with a variable reference
        String version = dep.getVersion();
        if (version != null && version.contains("$")) {
            String resolvedVersion = extractGradlePropertyFromContent(readFileContent(workspacePath.resolve(
                    buildMetadata.getBuildFileName() != null ? buildMetadata.getBuildFileName() : "build.gradle")), version);
            if (resolvedVersion != null) {
                dep.setVersion(resolvedVersion);
            }
        }

        // Classify based on configuration
        dep.setType(gradleConfigToDependencyType(configuration));

        // Determine internal vs external
        String projectGroupId = buildMetadata.getGradleGroup();
        if (projectGroupId != null && !projectGroupId.isEmpty()) {
            dep.setInternal(dep.getGroupId() != null && dep.getGroupId().startsWith(projectGroupId));
        }

        return dep;
    }

    private DependencyType gradleConfigToDependencyType(String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return DependencyType.COMPILE;
        }
        switch (configuration.toLowerCase()) {
            case "implementation":
            case "api":
                return DependencyType.COMPILE;
            case "compileOnly":
                return DependencyType.PROVIDED;
            case "runtimeOnly":
                return DependencyType.RUNTIME;
            case "testImplementation":
            case "testCompileOnly":
            case "testRuntimeOnly":
            case "unittest":
                return DependencyType.TEST;
            case "annotationProcessor":
            case "jpaMetamodelGenerator":
                return DependencyType.ANNOTATION_PROCESSOR;
            case "developmentOnly":
                return DependencyType.DEVELOPMENT_ONLY;
            default:
                return DependencyType.COMPILE;
        }
    }

    // ==================== Helper Methods ====================

    private String readFileContent(Path path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            logger.error("Failed to read file: {}", path, e);
            return null;
        }
    }

    private String extractXmlTag(String content, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + "\\s*>(.*?)</" + tagName + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String removeXmlComments(String content) {
        return content.replaceAll("\\Q<!--\\E.*?\\Q-->\\E", "");
    }

    private String removeGradleComments(String content) {
        // Remove single-line comments
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            // Remove // comments but not inside strings
            int commentIdx = -1;
            boolean inString = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                    inString = !inString;
                } else if (!inString && i < line.length() - 1 && c == '/' && line.charAt(i + 1) == '/') {
                    commentIdx = i;
                    break;
                }
            }
            if (commentIdx >= 0) {
                result.append(line.substring(0, commentIdx)).append("\n");
            } else {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    /**
     * Extract version properties from pom.xml <properties> section.
     */
    private List<String> extractPropertyNamesFromProperties(String pomContent) {
        List<String> propertyNames = new ArrayList<>();
        Pattern propsPattern = Pattern.compile("<properties>(.*?)</properties>", Pattern.DOTALL);
        Matcher matcher = propsPattern.matcher(pomContent);
        if (matcher.find()) {
            String propsSection = matcher.group(1);
            // Extract all property names
            Pattern namePattern = Pattern.compile("<([a-zA-Z0-9._-]+)\\s*>");
            Matcher nameMatcher = namePattern.matcher(propsSection);
            while (nameMatcher.find()) {
                propertyNames.add(nameMatcher.group(1));
            }
        }
        return propertyNames;
    }

    /**
     * Resolve a Maven property reference to its value from pom.xml content.
     */
    public String resolveMavenProperty(String pomContent, String propertyName) {
        if (propertyName == null || !propertyName.contains("${")) {
            return propertyName;
        }

        // Try properties section first
        Pattern propsPattern = Pattern.compile("<properties>(.*?)</properties>", Pattern.DOTALL);
        Matcher propsMatcher = propsPattern.matcher(pomContent);
        if (propsMatcher.find()) {
            String propsSection = propsMatcher.group(1);
            Pattern propPattern = Pattern.compile("<" + Pattern.quote(propertyName) + "\\s*>(.*?)</" + Pattern.quote(propertyName) + ">");
            Matcher propMatcher = propPattern.matcher(propsSection);
            if (propMatcher.find()) {
                return propMatcher.group(1).trim();
            }
        }

        // Try as direct property name
        Pattern directPropPattern = Pattern.compile("<" + Pattern.quote(propertyName) + ">\\s*(.*?)\\s*</" + Pattern.quote(propertyName) + ">");
        Matcher directMatcher = directPropPattern.matcher(pomContent);
        if (directMatcher.find()) {
            return directMatcher.group(1).trim();
        }

        return null;
    }

    private String extractGradlePropertyFromContent(String content, String variableName) {
        if (content == null || variableName == null) {
            return null;
        }

        // Remove the $ prefix and {} if present
        String varName = variableName.replaceAll("^\\$\\{?", "")  .replaceAll("\\}$", "");

        // Try to find: variableName = 'value' or variableName = "value"
        Pattern pattern = Pattern.compile(varName + "\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Try: ext { variableName = 'value' }
        pattern = Pattern.compile("ext\\s*\\{.*?" + varName + "\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.DOTALL);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}