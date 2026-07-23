package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for generating repository documentation from persisted metadata.
 * Generates human-readable Markdown documentation without parsing source code.
 */
@Service
public class DocumentationService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RepositoryRepository repositoryRepository;
    private final DocumentationRepository documentationRepository;
    private final JdbcTemplate jdbcTemplate;

    public DocumentationService(RepositoryRepository repositoryRepository,
                                 DocumentationRepository documentationRepository,
                                 JdbcTemplate jdbcTemplate) {
        this.repositoryRepository = repositoryRepository;
        this.documentationRepository = documentationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Generate documentation for a repository from persisted metadata.
     */
    @Transactional
    public RepositoryDocumentation generateDocumentation(String repositoryId) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting documentation generation for repository: {}", repositoryId);

        // Validate repository exists and is indexed
        Repository repository = validateRepositoryExistsAndIndexed(repositoryId);

        // Generate Markdown content from persisted metadata
        String markdownContent = generateMarkdownContent(repository);

        // Create documentation entity
        RepositoryDocumentation documentation = new RepositoryDocumentation();
        documentation.setRepositoryId(repositoryId);
        documentation.setMarkdownContent(markdownContent);
        documentation.setGeneratedAt(LocalDateTime.now());
        documentation.setContentSize((long) markdownContent.length());
        documentation.setGenerationStatus("SUCCESS");

        // Persist documentation
        documentationRepository.save(documentation);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Documentation generation completed for repository: {}. Duration: {}ms, Size: {} bytes",
                repositoryId, duration, markdownContent.length());

        return documentation;
    }

    /**
     * Retrieve existing documentation for a repository.
     */
    public RepositoryDocumentation getDocumentation(String repositoryId) {
        validateRepositoryExistsAndIndexed(repositoryId);
        
        RepositoryDocumentation documentation = documentationRepository.findByRepositoryId(repositoryId);
        if (documentation == null) {
            throw new IllegalStateException("Documentation not found for repository: " + repositoryId + 
                    ". Please generate documentation first.");
        }
        return documentation;
    }

    /**
     * Check if documentation exists for a repository.
     */
    public boolean documentationExists(String repositoryId) {
        return documentationRepository.existsByRepositoryId(repositoryId);
    }

    /**
     * Delete documentation for a repository.
     */
    public void deleteDocumentation(String repositoryId) {
        documentationRepository.deleteByRepositoryId(repositoryId);
        logger.info("Documentation deleted for repository: {}", repositoryId);
    }

    // ==================== Private Methods ====================

    private Repository validateRepositoryExistsAndIndexed(String repositoryId) {
        Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + repositoryId);
        }
        if (repository.getStatus() != RepositoryStatus.INDEXED) {
            throw new IllegalStateException("Repository is not indexed. Current status: " + 
                    (repository.getStatus() != null ? repository.getStatus().name() : "NULL"));
        }
        return repository;
    }

    private String generateMarkdownContent(Repository repository) {
        StringBuilder md = new StringBuilder();
        String repositoryId = repository.getRepositoryId();

        // Title and Header
        md.append("# ").append(repository.getRepositoryName()).append("\n\n");
        md.append("*Auto-generated documentation from indexed metadata*\n\n");
        md.append("---\n\n");

        // Table of Contents
        generateTableOfContents(md);

        // Project Overview
        generateProjectOverview(md, repository);

        // Technology Summary
        generateTechnologySummary(md, repositoryId);

        // Project Structure
        generateProjectStructure(md, repositoryId);

        // Build Information
        generateBuildInformation(md, repositoryId);

        // Configuration Summary
        generateConfigurationSummary(md, repositoryId);

        // Database Summary
        generateDatabaseSummary(md, repositoryId);

        // Spring Summary
        generateSpringSummary(md, repositoryId);

        // REST API Summary
        generateRestApiSummary(md, repositoryId);

        // Dependency Summary
        generateDependencySummary(md, repositoryId);

        // Indexing Summary
        generateIndexingSummary(md, repositoryId);

        // Setup Guide
        generateSetupGuide(md, repository, repositoryId);

        // Footer
        md.append("---\n\n");
        md.append("*Generated on ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("*\n");

        return md.toString();
    }

    private void generateTableOfContents(StringBuilder md) {
        md.append("## Table of Contents\n\n");
        md.append("1. [Project Overview](#project-overview)\n");
        md.append("2. [Technology Summary](#technology-summary)\n");
        md.append("3. [Project Structure](#project-structure)\n");
        md.append("4. [Build Information](#build-information)\n");
        md.append("5. [Configuration Summary](#configuration-summary)\n");
        md.append("6. [Database Summary](#database-summary)\n");
        md.append("7. [Spring Summary](#spring-summary)\n");
        md.append("8. [REST API Summary](#rest-api-summary)\n");
        md.append("9. [Dependency Summary](#dependency-summary)\n");
        md.append("10. [Indexing Summary](#indexing-summary)\n");
        md.append("11. [Setup Guide](#setup-guide)\n\n");
        md.append("---\n\n");
    }

    private void generateProjectOverview(StringBuilder md, Repository repository) {
        md.append("## Project Overview\n\n");
        md.append("| Property | Value |\n");
        md.append("|----------|-------|\n");
        md.append("| **Repository Name** | ").append(repository.getRepositoryName()).append(" |\n");
        md.append("| **Repository ID** | ").append(repository.getRepositoryId()).append(" |\n");
        md.append("| **Repository Location** | ").append(repository.getOriginalPath()).append(" |\n");
        md.append("| **Build System** | ").append(repository.getBuildSystem() != null ? repository.getBuildSystem() : "Unknown").append(" |\n");
        md.append("| **Technology Stack** | ").append(repository.getTechnologyStack() != null ? repository.getTechnologyStack() : "Unknown").append(" |\n");
        md.append("| **Status** | ").append(repository.getStatus() != null ? repository.getStatus().name() : "Unknown").append(" |\n");
        
        // Get additional info from build metadata
        String projectType = querySingleValue("SELECT project_type FROM build_metadata WHERE repository_id = ?", repository.getRepositoryId());
        String primaryLanguage = querySingleValue("SELECT languages FROM technology_stack WHERE repository_id = ?", repository.getRepositoryId());
        
        if (projectType != null && !projectType.isEmpty()) {
            md.append("| **Project Type** | ").append(projectType).append(" |\n");
        }
        if (primaryLanguage != null && !primaryLanguage.isEmpty() && !"[]".equals(primaryLanguage)) {
            md.append("| **Primary Language** | ").append(extractPrimaryLanguage(primaryLanguage)).append(" |\n");
        }
        
        md.append("\n");
    }

    private void generateTechnologySummary(StringBuilder md, String repositoryId) {
        md.append("## Technology Summary\n\n");

        // Languages
        String languages = querySingleValue("SELECT languages FROM technology_stack WHERE repository_id = ?", repositoryId);
        md.append("### Languages\n\n");
        if (languages != null && !languages.isEmpty() && !"[]".equals(languages)) {
            List<String> langList = parseJsonArray(languages);
            for (String lang : langList) {
                md.append("- ").append(lang).append("\n");
            }
        } else {
            md.append("- Java (detected)\n");
        }
        md.append("\n");

        // Frameworks
        String frameworks = querySingleValue("SELECT frameworks FROM technology_stack WHERE repository_id = ?", repositoryId);
        md.append("### Frameworks\n\n");
        if (frameworks != null && !frameworks.isEmpty() && !"[]".equals(frameworks)) {
            List<String> fwList = parseJsonArray(frameworks);
            for (String fw : fwList) {
                md.append("- ").append(fw).append("\n");
            }
        } else {
            md.append("- None detected\n");
        }
        md.append("\n");

        // Build Tools
        String buildTools = querySingleValue("SELECT build_tools FROM technology_stack WHERE repository_id = ?", repositoryId);
        md.append("### Build Tools\n\n");
        if (buildTools != null && !buildTools.isEmpty() && !"[]".equals(buildTools)) {
            List<String> btList = parseJsonArray(buildTools);
            for (String bt : btList) {
                md.append("- ").append(bt).append("\n");
            }
        } else {
            md.append("- None detected\n");
        }
        md.append("\n");

        // Databases
        String databases = querySingleValue("SELECT databases FROM technology_stack WHERE repository_id = ?", repositoryId);
        md.append("### Databases\n\n");
        if (databases != null && !databases.isEmpty() && !"[]".equals(databases)) {
            List<String> dbList = parseJsonArray(databases);
            for (String db : dbList) {
                md.append("- ").append(db).append("\n");
            }
        } else {
            md.append("- None detected\n");
        }
        md.append("\n");

        // Testing Frameworks
        String testingFrameworks = querySingleValue("SELECT testing_frameworks FROM technology_stack WHERE repository_id = ?", repositoryId);
        md.append("### Testing Frameworks\n\n");
        if (testingFrameworks != null && !testingFrameworks.isEmpty() && !"[]".equals(testingFrameworks)) {
            List<String> tfList = parseJsonArray(testingFrameworks);
            for (String tf : tfList) {
                md.append("- ").append(tf).append("\n");
            }
        } else {
            md.append("- None detected\n");
        }
        md.append("\n");
    }

    private void generateProjectStructure(StringBuilder md, String repositoryId) {
        md.append("## Project Structure\n\n");

        // Module Summary
        String childModules = querySingleValue("SELECT child_modules FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("### Modules\n\n");
        if (childModules != null && !childModules.isEmpty() && !"[]".equals(childModules)) {
            List<String> modules = parseJsonArray(childModules);
            for (String module : modules) {
                md.append("- ").append(module).append("\n");
            }
        } else {
            md.append("- Single module project\n");
        }
        md.append("\n");

        // Package Summary
        Long packageCount = queryLong("SELECT COUNT(DISTINCT package_name) FROM file_index WHERE repository_id = ?", repositoryId);
        md.append("### Packages\n\n");
        md.append("- **Total Packages**: ").append(packageCount != null ? packageCount : 0).append("\n\n");

        // Source Directories
        md.append("### Source Directories\n\n");
        List<String> sourceDirs = queryList("SELECT DISTINCT file_path FROM file_index WHERE repository_id = ? AND file_path LIKE '%src/main%'", repositoryId);
        if (!sourceDirs.isEmpty()) {
            Set<String> uniqueDirs = new LinkedHashSet<>();
            for (String path : sourceDirs) {
                String dir = extractDirectory(path);
                if (dir.contains("src/main")) {
                    uniqueDirs.add(dir);
                }
            }
            for (String dir : uniqueDirs) {
                md.append("- `").append(dir).append("`\n");
            }
        } else {
            md.append("- Standard Maven/Gradle structure\n");
        }
        md.append("\n");

        // Test Directories
        md.append("### Test Directories\n\n");
        List<String> testDirs = queryList("SELECT DISTINCT file_path FROM file_index WHERE repository_id = ? AND file_path LIKE '%src/test%'", repositoryId);
        if (!testDirs.isEmpty()) {
            Set<String> uniqueDirs = new LinkedHashSet<>();
            for (String path : testDirs) {
                String dir = extractDirectory(path);
                uniqueDirs.add(dir);
            }
            for (String dir : uniqueDirs) {
                md.append("- `").append(dir).append("`\n");
            }
        } else {
            md.append("- No test directories detected\n");
        }
        md.append("\n");
    }

    private void generateBuildInformation(StringBuilder md, String repositoryId) {
        md.append("## Build Information\n\n");

        md.append("| Property | Value |\n");
        md.append("|----------|-------|\n");

        // Build System Type
        String buildSystemType = querySingleValue("SELECT build_system_type FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("| **Build System** | ").append(buildSystemType != null ? buildSystemType : "Unknown").append(" |\n");

        // Java Version
        String javaVersion = querySingleValue("SELECT java_version FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("| **Java Version** | ").append(javaVersion != null && !javaVersion.isEmpty() ? javaVersion : "Not specified").append(" |\n");

        // Project Group ID
        String groupId = querySingleValue("SELECT project_group_id FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("| **Group ID** | ").append(groupId != null && !groupId.isEmpty() ? groupId : "N/A").append(" |\n");

        // Project Artifact ID
        String artifactId = querySingleValue("SELECT project_artifact_id FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("| **Artifact ID** | ").append(artifactId != null && !artifactId.isEmpty() ? artifactId : "N/A").append(" |\n");

        // Project Version
        String version = querySingleValue("SELECT project_version FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("| **Version** | ").append(version != null && !version.isEmpty() ? version : "N/A").append(" |\n");

        // Packaging
        String packaging = querySingleValue("SELECT packaging FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("| **Packaging** | ").append(packaging != null && !packaging.isEmpty() ? packaging : "jar").append(" |\n");

        // Spring Boot Version
        String springBootVersion = querySingleValue("SELECT spring_boot_version FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("| **Spring Boot Version** | ").append(springBootVersion != null && !springBootVersion.isEmpty() ? springBootVersion : "N/A").append(" |\n");

        // Maven Wrapper
        String mavenWrapper = querySingleValue("SELECT maven_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("| **Maven Wrapper** | ").append("1".equals(mavenWrapper) ? "Yes" : "No").append(" |\n");

        // Gradle Wrapper
        String gradleWrapper = querySingleValue("SELECT gradle_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("| **Gradle Wrapper** | ").append("1".equals(gradleWrapper) ? "Yes" : "No").append(" |\n");

        md.append("\n");

        // Multi-module Information
        String projectType = querySingleValue("SELECT project_type FROM build_metadata WHERE repository_id = ?", repositoryId);
        md.append("### Multi-Module Information\n\n");
        if (projectType != null && projectType.toLowerCase().contains("multi")) {
            md.append("- **Project Type**: Multi-module\n");
            String modules = querySingleValue("SELECT modules FROM build_metadata WHERE repository_id = ?", repositoryId);
            if (modules != null && !modules.isEmpty()) {
                md.append("- **Modules**: ").append(modules).append("\n");
            }
        } else {
            md.append("- **Project Type**: ").append(projectType != null ? projectType : "Single Module").append("\n");
        }
        md.append("\n");
    }

    private void generateConfigurationSummary(StringBuilder md, String repositoryId) {
        md.append("## Configuration Summary\n\n");

        // Configuration Statistics
        Long totalConfigFiles = queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ?", repositoryId);
        Long springConfigs = queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'SPRING'", repositoryId);
        Long dockerConfigs = queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'DOCKER'", repositoryId);
        Long k8sConfigs = queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'KUBERNETES'", repositoryId);
        Long ciCdConfigs = queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'CI_CD'", repositoryId);
        Long envConfigs = queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'ENVIRONMENT'", repositoryId);
        Long loggingConfigs = queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'LOGGING'", repositoryId);

        md.append("| Configuration Type | Count |\n");
        md.append("|-------------------|-------|\n");
        md.append("| **Total Configuration Files** | ").append(totalConfigFiles != null ? totalConfigFiles : 0).append(" |\n");
        md.append("| **Spring Configurations** | ").append(springConfigs != null ? springConfigs : 0).append(" |\n");
        md.append("| **Docker Files** | ").append(dockerConfigs != null ? dockerConfigs : 0).append(" |\n");
        md.append("| **Kubernetes Files** | ").append(k8sConfigs != null ? k8sConfigs : 0).append(" |\n");
        md.append("| **CI/CD Configurations** | ").append(ciCdConfigs != null ? ciCdConfigs : 0).append(" |\n");
        md.append("| **Environment Files** | ").append(envConfigs != null ? envConfigs : 0).append(" |\n");
        md.append("| **Logging Configurations** | ").append(loggingConfigs != null ? loggingConfigs : 0).append(" |\n");
        md.append("\n");

        // List configuration files
        List<String> configFiles = queryList("SELECT file_name FROM configuration_file WHERE repository_id = ? ORDER BY config_type, file_name", repositoryId);
        if (!configFiles.isEmpty()) {
            md.append("### Configuration Files\n\n");
            for (String file : configFiles) {
                md.append("- `").append(file).append("`\n");
            }
            md.append("\n");
        }
    }

    private void generateDatabaseSummary(StringBuilder md, String repositoryId) {
        md.append("## Database Summary\n\n");

        // Database Statistics
        Long databasesDetected = queryLong("SELECT COUNT(DISTINCT database_type) FROM database_artifact WHERE repository_id = ?", repositoryId);
        Long datasources = queryLong("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'DATASOURCE'", repositoryId);
        Long sqlFiles = queryLong("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'SQL_FILE'", repositoryId);
        Long migrationScripts = queryLong("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'MIGRATION'", repositoryId);

        md.append("| Property | Value |\n");
        md.append("|----------|-------|\n");
        md.append("| **Databases Detected** | ").append(databasesDetected != null ? databasesDetected : 0).append(" |\n");
        md.append("| **Datasources** | ").append(datasources != null ? datasources : 0).append(" |\n");
        md.append("| **SQL Files** | ").append(sqlFiles != null ? sqlFiles : 0).append(" |\n");
        md.append("| **Migration Scripts** | ").append(migrationScripts != null ? migrationScripts : 0).append(" |\n");
        md.append("\n");

        // List detected databases
        List<String> dbTypes = queryList("SELECT DISTINCT database_type FROM database_artifact WHERE repository_id = ?", repositoryId);
        if (!dbTypes.isEmpty()) {
            md.append("### Detected Databases\n\n");
            for (String dbType : dbTypes) {
                md.append("- ").append(dbType).append("\n");
            }
            md.append("\n");
        }
    }

    private void generateSpringSummary(StringBuilder md, String repositoryId) {
        md.append("## Spring Summary\n\n");

        // Spring Component Statistics
        Long totalComponents = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ?", repositoryId);
        Long services = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'SERVICE'", repositoryId);
        Long controllers = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'CONTROLLER'", repositoryId);
        Long restControllers = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'REST_CONTROLLER'", repositoryId);
        Long repositories = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'REPOSITORY'", repositoryId);
        Long configurations = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'CONFIGURATION'", repositoryId);
        Long beans = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'BEAN'", repositoryId);

        md.append("| Component Type | Count |\n");
        md.append("|---------------|-------|\n");
        md.append("| **Total Spring Components** | ").append(totalComponents != null ? totalComponents : 0).append(" |\n");
        md.append("| **Services** | ").append(services != null ? services : 0).append(" |\n");
        md.append("| **Controllers** | ").append(controllers != null ? controllers : 0).append(" |\n");
        md.append("| **REST Controllers** | ").append(restControllers != null ? restControllers : 0).append(" |\n");
        md.append("| **Repositories** | ").append(repositories != null ? repositories : 0).append(" |\n");
        md.append("| **Configuration Classes** | ").append(configurations != null ? configurations : 0).append(" |\n");
        md.append("| **Beans** | ").append(beans != null ? beans : 0).append(" |\n");
        md.append("\n");

        // Scheduled Tasks
        Long scheduledTasks = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND has_scheduled = 1", repositoryId);
        md.append("### Scheduled Tasks\n\n");
        md.append("- **Total Scheduled Tasks**: ").append(scheduledTasks != null ? scheduledTasks : 0).append("\n\n");

        // Async Components
        Long asyncComponents = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND has_async = 1", repositoryId);
        md.append("### Async Components\n\n");
        md.append("- **Total Async Components**: ").append(asyncComponents != null ? asyncComponents : 0).append("\n\n");

        // List Spring Components
        List<String> componentNames = queryList("SELECT component_name FROM spring_component WHERE repository_id = ? AND component_type != 'REST_ENDPOINT' ORDER BY component_type, component_name", repositoryId);
        if (!componentNames.isEmpty()) {
            md.append("### Spring Components\n\n");
            for (String name : componentNames) {
                md.append("- `").append(name).append("`\n");
            }
            md.append("\n");
        }
    }

    private void generateRestApiSummary(StringBuilder md, String repositoryId) {
        md.append("## REST API Summary\n\n");

        // REST API Statistics
        Long totalEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ?", repositoryId);
        Long totalControllers = queryLong("SELECT COUNT(DISTINCT controller_name) FROM rest_api_endpoint WHERE repository_id = ?", repositoryId);
        Long getEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'GET'", repositoryId);
        Long postEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'POST'", repositoryId);
        Long putEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'PUT'", repositoryId);
        Long deleteEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'DELETE'", repositoryId);
        Long patchEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'PATCH'", repositoryId);

        md.append("| Property | Value |\n");
        md.append("|----------|-------|\n");
        md.append("| **Total Controllers** | ").append(totalControllers != null ? totalControllers : 0).append(" |\n");
        md.append("| **Total Endpoints** | ").append(totalEndpoints != null ? totalEndpoints : 0).append(" |\n");
        md.append("| **GET Endpoints** | ").append(getEndpoints != null ? getEndpoints : 0).append(" |\n");
        md.append("| **POST Endpoints** | ").append(postEndpoints != null ? postEndpoints : 0).append(" |\n");
        md.append("| **PUT Endpoints** | ").append(putEndpoints != null ? putEndpoints : 0).append(" |\n");
        md.append("| **DELETE Endpoints** | ").append(deleteEndpoints != null ? deleteEndpoints : 0).append(" |\n");
        md.append("| **PATCH Endpoints** | ").append(patchEndpoints != null ? patchEndpoints : 0).append(" |\n");
        md.append("\n");

        // List Controllers and their endpoints
        List<String> controllerNames = queryList("SELECT DISTINCT controller_name FROM rest_api_endpoint WHERE repository_id = ? ORDER BY controller_name", repositoryId);
        if (!controllerNames.isEmpty()) {
            md.append("### API Controllers\n\n");
            for (String controller : controllerNames) {
                md.append("#### ").append(controller).append("\n\n");
                
                // Get base path
                String basePath = querySingleValue("SELECT base_path FROM rest_api_endpoint WHERE repository_id = ? AND controller_name = ? LIMIT 1", repositoryId, controller);
                if (basePath != null && !basePath.isEmpty()) {
                    md.append("- **Base Path**: `").append(basePath).append("`\n\n");
                }
                
                md.append("| Method | Endpoint |\n");
                md.append("|--------|----------|\n");
                
                List<Map<String, String>> endpoints = queryEndpointDetails(repositoryId, controller);
                for (Map<String, String> endpoint : endpoints) {
                    md.append("| ").append(endpoint.get("http_method")).append(" | `").append(endpoint.get("endpoint_path")).append("` |\n");
                }
                md.append("\n");
            }
        }
    }

    private void generateDependencySummary(StringBuilder md, String repositoryId) {
        md.append("## Dependency Summary\n\n");

        // Dependency Statistics
        Long totalDependencies = queryLong("SELECT COUNT(*) FROM dependency WHERE repository_id = ?", repositoryId);
        Long compileDeps = queryLong("SELECT COUNT(*) FROM dependency WHERE repository_id = ? AND dependency_type = 'COMPILE'", repositoryId);
        Long runtimeDeps = queryLong("SELECT COUNT(*) FROM dependency WHERE repository_id = ? AND dependency_type = 'RUNTIME'", repositoryId);
        Long testDeps = queryLong("SELECT COUNT(*) FROM dependency WHERE repository_id = ? AND dependency_type = 'TEST'", repositoryId);

        md.append("| Dependency Type | Count |\n");
        md.append("|----------------|-------|\n");
        md.append("| **Total Dependencies** | ").append(totalDependencies != null ? totalDependencies : 0).append(" |\n");
        md.append("| **Compile Dependencies** | ").append(compileDeps != null ? compileDeps : 0).append(" |\n");
        md.append("| **Runtime Dependencies** | ").append(runtimeDeps != null ? runtimeDeps : 0).append(" |\n");
        md.append("| **Test Dependencies** | ").append(testDeps != null ? testDeps : 0).append(" |\n");
        md.append("\n");

        // List major dependencies
        List<Map<String, String>> dependencies = queryDependencyDetails(repositoryId);
        if (!dependencies.isEmpty()) {
            md.append("### Key Dependencies\n\n");
            md.append("| Group ID | Artifact ID | Version | Scope |\n");
            md.append("|----------|-------------|---------|-------|\n");
            int count = 0;
            for (Map<String, String> dep : dependencies) {
                if (count >= 20) break; // Limit to top 20
                md.append("| ").append(dep.get("group_id"))
                  .append(" | ").append(dep.get("artifact_id"))
                  .append(" | ").append(dep.get("version"))
                  .append(" | ").append(dep.get("scope"))
                  .append(" |\n");
                count++;
            }
            md.append("\n");
        }
    }

    private void generateIndexingSummary(StringBuilder md, String repositoryId) {
        md.append("## Indexing Summary\n\n");

        // Code Statistics
        Long javaFiles = queryLong("SELECT COUNT(*) FROM file_index WHERE repository_id = ?", repositoryId);
        Long classes = queryLong("SELECT COUNT(*) FROM class_info WHERE repository_id = ? AND class_type = 'CLASS'", repositoryId);
        Long interfaces = queryLong("SELECT COUNT(*) FROM class_info WHERE repository_id = ? AND class_type = 'INTERFACE'", repositoryId);
        Long enums = queryLong("SELECT COUNT(*) FROM class_info WHERE repository_id = ? AND class_type = 'ENUM'", repositoryId);
        Long methods = queryLong("SELECT COUNT(*) FROM method_info WHERE repository_id = ?", repositoryId);
        Long fields = queryLong("SELECT COUNT(*) FROM field_info WHERE repository_id = ?", repositoryId);
        Long packages = queryLong("SELECT COUNT(DISTINCT package_name) FROM file_index WHERE repository_id = ?", repositoryId);

        md.append("| Metric | Count |\n");
        md.append("|--------|-------|\n");
        md.append("| **Indexed Java Files** | ").append(javaFiles != null ? javaFiles : 0).append(" |\n");
        md.append("| **Classes** | ").append(classes != null ? classes : 0).append(" |\n");
        md.append("| **Interfaces** | ").append(interfaces != null ? interfaces : 0).append(" |\n");
        md.append("| **Enums** | ").append(enums != null ? enums : 0).append(" |\n");
        md.append("| **Methods** | ").append(methods != null ? methods : 0).append(" |\n");
        md.append("| **Fields** | ").append(fields != null ? fields : 0).append(" |\n");
        md.append("| **Packages** | ").append(packages != null ? packages : 0).append(" |\n");
        md.append("\n");

        // Parsing errors
        Long parsingErrors = queryLong("SELECT parsing_errors FROM java_indexing_statistics WHERE repository_id = ?", repositoryId);
        md.append("### Parsing Statistics\n\n");
        md.append("- **Parsing Errors**: ").append(parsingErrors != null ? parsingErrors : 0).append("\n\n");
    }

    private void generateSetupGuide(StringBuilder md, Repository repository, String repositoryId) {
        md.append("## Setup Guide\n\n");

        // Prerequisites
        md.append("### Prerequisites\n\n");
        
        String javaVersion = querySingleValue("SELECT java_version FROM build_metadata WHERE repository_id = ?", repositoryId);
        if (javaVersion != null && !javaVersion.isEmpty()) {
            md.append("- **Java**: Version ").append(javaVersion).append(" or higher\n");
        } else {
            md.append("- **Java**: JDK 11 or higher (recommended)\n");
        }
        
        String buildSystemType = querySingleValue("SELECT build_system_type FROM build_metadata WHERE repository_id = ?", repositoryId);
        if ("MAVEN".equalsIgnoreCase(buildSystemType)) {
            md.append("- **Maven**: Version 3.6 or higher\n");
        } else if ("GRADLE".equalsIgnoreCase(buildSystemType)) {
            md.append("- **Gradle**: Version 6.0 or higher\n");
        }
        md.append("\n");

        // Build Command
        md.append("### Build Command\n\n");
        md.append("```bash\n");
        if ("MAVEN".equalsIgnoreCase(buildSystemType)) {
            String mavenWrapper = querySingleValue("SELECT maven_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);
            if ("1".equals(mavenWrapper)) {
                md.append("./mvnw clean install\n");
            } else {
                md.append("mvn clean install\n");
            }
        } else if ("GRADLE".equalsIgnoreCase(buildSystemType)) {
            String gradleWrapper = querySingleValue("SELECT gradle_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);
            if ("1".equals(gradleWrapper)) {
                md.append("./gradlew clean build\n");
            } else {
                md.append("gradle clean build\n");
            }
        } else {
            md.append("# Build command depends on your build system\n");
        }
        md.append("```\n\n");

        // Run Command
        md.append("### Run Command\n\n");
        md.append("```bash\n");
        if ("MAVEN".equalsIgnoreCase(buildSystemType)) {
            String mavenWrapper = querySingleValue("SELECT maven_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);
            if ("1".equals(mavenWrapper)) {
                md.append("./mvnw spring-boot:run\n");
            } else {
                md.append("mvn spring-boot:run\n");
            }
        } else if ("GRADLE".equalsIgnoreCase(buildSystemType)) {
            String gradleWrapper = querySingleValue("SELECT gradle_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);
            if ("1".equals(gradleWrapper)) {
                md.append("./gradlew bootRun\n");
            } else {
                md.append("gradle bootRun\n");
            }
        } else {
            md.append("# Run command depends on your build system\n");
        }
        md.append("```\n\n");

        // Test Command
        md.append("### Test Command\n\n");
        md.append("```bash\n");
        if ("MAVEN".equalsIgnoreCase(buildSystemType)) {
            String mavenWrapper = querySingleValue("SELECT maven_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);
            if ("1".equals(mavenWrapper)) {
                md.append("./mvnw test\n");
            } else {
                md.append("mvn test\n");
            }
        } else if ("GRADLE".equalsIgnoreCase(buildSystemType)) {
            String gradleWrapper = querySingleValue("SELECT gradle_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);
            if ("1".equals(gradleWrapper)) {
                md.append("./gradlew test\n");
            } else {
                md.append("gradle test\n");
            }
        } else {
            md.append("# Test command depends on your build system\n");
        }
        md.append("```\n\n");

        // Configuration Files Required
        md.append("### Configuration Files\n\n");
        List<String> configFiles = queryList("SELECT file_name FROM configuration_file WHERE repository_id = ? AND config_type = 'SPRING' ORDER BY file_name", repositoryId);
        if (!configFiles.isEmpty()) {
            md.append("The following configuration files are required:\n\n");
            for (String file : configFiles) {
                md.append("- `").append(file).append("`\n");
            }
        } else {
            md.append("- Standard Spring Boot configuration (`application.properties` or `application.yml`)\n");
        }
        md.append("\n");
    }

    // ==================== Helper Methods ====================

    private String querySingleValue(String sql, String... params) {
        try {
            if (params.length == 1) {
                return jdbcTemplate.queryForObject(sql, String.class, params[0]);
            } else if (params.length == 2) {
                return jdbcTemplate.queryForObject(sql, String.class, params[0], params[1]);
            }
            return jdbcTemplate.queryForObject(sql, String.class, (Object[]) params);
        } catch (Exception e) {
            return null;
        }
    }

    private Long queryLong(String sql, String... params) {
        try {
            if (params.length == 1) {
                return jdbcTemplate.queryForObject(sql, Long.class, params[0]);
            }
            return jdbcTemplate.queryForObject(sql, Long.class, (Object[]) params);
        } catch (Exception e) {
            return 0L;
        }
    }

    private List<String> queryList(String sql, String... params) {
        try {
            if (params.length == 1) {
                return jdbcTemplate.queryForList(sql, String.class, params[0]);
            }
            return jdbcTemplate.queryForList(sql, String.class, (Object[]) params);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, String>> queryEndpointDetails(String repositoryId, String controller) {
        try {
            String sql = "SELECT http_method, endpoint_path FROM rest_api_endpoint WHERE repository_id = ? AND controller_name = ? ORDER BY http_method, endpoint_path";
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, String> endpoint = new HashMap<>();
                endpoint.put("http_method", rs.getString("http_method"));
                endpoint.put("endpoint_path", rs.getString("endpoint_path"));
                return endpoint;
            }, repositoryId, controller);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, String>> queryDependencyDetails(String repositoryId) {
        try {
            String sql = "SELECT group_id, artifact_id, version, scope FROM dependency WHERE repository_id = ? ORDER BY group_id, artifact_id";
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, String> dep = new HashMap<>();
                dep.put("group_id", rs.getString("group_id") != null ? rs.getString("group_id") : "");
                dep.put("artifact_id", rs.getString("artifact_id") != null ? rs.getString("artifact_id") : "");
                dep.put("version", rs.getString("version") != null ? rs.getString("version") : "");
                dep.put("scope", rs.getString("scope") != null ? rs.getString("scope") : "compile");
                return dep;
            }, repositoryId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> parseJsonArray(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.length() < 2) {
            return result;
        }
        
        try {
            String inner = json.substring(1, json.length() - 1);
            if (inner.isEmpty()) {
                return result;
            }
            
            // Handle both simple string arrays and object arrays
            if (inner.startsWith("{")) {
                // Object array - extract name fields
                int startIndex = 0;
                while (startIndex < inner.length()) {
                    int objStart = inner.indexOf('{', startIndex);
                    if (objStart == -1) break;
                    
                    int objEnd = findMatchingBracket(inner, objStart);
                    if (objEnd == -1) break;
                    
                    String objStr = inner.substring(objStart + 1, objEnd);
                    String name = extractStringValue(objStr, "name");
                    if (name != null && !name.isEmpty()) {
                        result.add(name);
                    }
                    
                    startIndex = objEnd + 1;
                }
            } else {
                // Simple string array
                String[] parts = inner.split(",");
                for (String part : parts) {
                    String cleaned = part.trim().replace("\"", "");
                    if (!cleaned.isEmpty()) {
                        result.add(cleaned);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse JSON array: {}", json, e);
        }
        
        return result;
    }

    private String extractStringValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int startIndex = json.indexOf(pattern);
        if (startIndex == -1) return null;
        
        int colonIndex = json.indexOf(':', startIndex + pattern.length());
        if (colonIndex == -1) return null;
        
        int valueStart = json.indexOf('"', colonIndex + 1);
        if (valueStart == -1) return null;
        
        int valueEnd = json.indexOf('"', valueStart + 1);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart + 1, valueEnd);
    }

    private int findMatchingBracket(String json, int startIndex) {
        if (startIndex >= json.length() || json.charAt(startIndex) != '{') {
            return -1;
        }
        
        int depth = 0;
        boolean inString = false;
        
        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        
        return -1;
    }

    private String extractPrimaryLanguage(String languagesJson) {
        List<String> languages = parseJsonArray(languagesJson);
        return languages.isEmpty() ? "Java" : languages.get(0);
    }

    private String extractDirectory(String filePath) {
        if (filePath == null) return "";
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash == -1) lastSlash = filePath.lastIndexOf('\\');
        return lastSlash > 0 ? filePath.substring(0, lastSlash) : filePath;
    }
}