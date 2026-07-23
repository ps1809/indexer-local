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
 * Service for generating enhanced README from persisted metadata.
 * Generates concise, developer-friendly README without parsing source code.
 */
@Service
public class ReadmeService {

    private static final Logger logger = LoggerFactory.getLogger(ReadmeService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RepositoryRepository repositoryRepository;
    private final ReadmeRepository readmeRepository;
    private final JdbcTemplate jdbcTemplate;

    public ReadmeService(RepositoryRepository repositoryRepository,
                         ReadmeRepository readmeRepository,
                         JdbcTemplate jdbcTemplate) {
        this.repositoryRepository = repositoryRepository;
        this.readmeRepository = readmeRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Generate enhanced README for a repository from persisted metadata.
     */
    @Transactional
    public RepositoryReadme generateReadme(String repositoryId) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting README generation for repository: {}", repositoryId);

        // Validate repository exists and is indexed
        Repository repository = validateRepositoryExistsAndIndexed(repositoryId);

        // Generate Markdown content from persisted metadata
        String markdownContent = generateMarkdownContent(repository);

        // Create readme entity
        RepositoryReadme readme = new RepositoryReadme();
        readme.setRepositoryId(repositoryId);
        readme.setMarkdownContent(markdownContent);
        readme.setGeneratedAt(LocalDateTime.now());
        readme.setContentSize((long) markdownContent.length());
        readme.setGenerationStatus("SUCCESS");

        // Persist readme
        readmeRepository.save(readme);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("README generation completed for repository: {}. Duration: {}ms, Size: {} bytes",
                repositoryId, duration, markdownContent.length());

        return readme;
    }

    /**
     * Retrieve existing README for a repository.
     */
    public RepositoryReadme getReadme(String repositoryId) {
        validateRepositoryExistsAndIndexed(repositoryId);
        
        RepositoryReadme readme = readmeRepository.findByRepositoryId(repositoryId);
        if (readme == null) {
            throw new IllegalStateException("README not found for repository: " + repositoryId + 
                    ". Please generate README first.");
        }
        return readme;
    }

    /**
     * Check if README exists for a repository.
     */
    public boolean readmeExists(String repositoryId) {
        return readmeRepository.existsByRepositoryId(repositoryId);
    }

    /**
     * Delete README for a repository.
     */
    public void deleteReadme(String repositoryId) {
        readmeRepository.deleteByRepositoryId(repositoryId);
        logger.info("README deleted for repository: {}", repositoryId);
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

        // Header
        generateHeader(md, repository);

        // Project Overview
        generateProjectOverview(md, repository, repositoryId);

        // Technology Stack
        generateTechnologyStack(md, repositoryId);

        // Project Structure
        generateProjectStructure(md, repositoryId);

        // Build and Run
        generateBuildAndRun(md, repositoryId);

        // Configuration
        generateConfiguration(md, repositoryId);

        // REST API Overview
        generateRestApiOverview(md, repositoryId);

        // Spring Overview
        generateSpringOverview(md, repositoryId);

        // Database Overview
        generateDatabaseOverview(md, repositoryId);

        // Project Statistics
        generateProjectStatistics(md, repositoryId);

        // Footer
        md.append("---\n\n");
        md.append("*This README was auto-generated from indexed metadata on ")
          .append(LocalDateTime.now().format(DATE_FORMATTER)).append("*\n");

        return md.toString();
    }

    private void generateHeader(StringBuilder md, Repository repository) {
        md.append("# ").append(repository.getRepositoryName()).append("\n\n");
        
        // Project description from build metadata
        String projectName = querySingleValue("SELECT project_name FROM build_metadata WHERE repository_id = ?", 
                repository.getRepositoryId());
        if (projectName != null && !projectName.isEmpty() && !projectName.equals(repository.getRepositoryName())) {
            md.append("**").append(projectName).append("**\n\n");
        }
        
        md.append("> Auto-generated README from indexed repository metadata\n\n");
        md.append("---\n\n");
    }

    private void generateProjectOverview(StringBuilder md, Repository repository, String repositoryId) {
        md.append("## 📋 Project Overview\n\n");
        
        // Get build metadata
        String buildSystemType = querySingleValue("SELECT build_system_type FROM build_metadata WHERE repository_id = ?", repositoryId);
        String projectType = querySingleValue("SELECT project_type FROM build_metadata WHERE repository_id = ?", repositoryId);
        String javaVersion = querySingleValue("SELECT java_version FROM build_metadata WHERE repository_id = ?", repositoryId);
        String springBootVersion = querySingleValue("SELECT spring_boot_version FROM build_metadata WHERE repository_id = ?", repositoryId);
        
        md.append("| Property | Value |\n");
        md.append("|----------|-------|\n");
        md.append("| **Project Type** | ").append(projectType != null && !projectType.isEmpty() ? projectType : "Single Module").append(" |\n");
        md.append("| **Primary Language** | Java").append(javaVersion != null && !javaVersion.isEmpty() ? " " + javaVersion : "").append(" |\n");
        md.append("| **Build System** | ").append(buildSystemType != null ? buildSystemType : "Unknown").append(" |\n");
        
        if (springBootVersion != null && !springBootVersion.isEmpty()) {
            md.append("| **Spring Boot** | ").append(springBootVersion).append(" |\n");
        }
        
        md.append("| **Status** | ").append(repository.getStatus() != null ? repository.getStatus().name() : "Unknown").append(" |\n");
        md.append("\n");
    }

    private void generateTechnologyStack(StringBuilder md, String repositoryId) {
        md.append("## 🛠️ Technology Stack\n\n");

        // Languages
        String languages = querySingleValue("SELECT languages FROM technology_stack WHERE repository_id = ?", repositoryId);
        if (languages != null && !languages.isEmpty() && !"[]".equals(languages)) {
            md.append("### Languages\n");
            List<String> langList = parseJsonArray(languages);
            for (String lang : langList) {
                md.append("- ").append(lang).append("\n");
            }
            md.append("\n");
        }

        // Frameworks
        String frameworks = querySingleValue("SELECT frameworks FROM technology_stack WHERE repository_id = ?", repositoryId);
        if (frameworks != null && !frameworks.isEmpty() && !"[]".equals(frameworks)) {
            md.append("### Frameworks\n");
            List<String> fwList = parseJsonArray(frameworks);
            for (String fw : fwList) {
                md.append("- ").append(fw).append("\n");
            }
            md.append("\n");
        }

        // Databases
        String databases = querySingleValue("SELECT databases FROM technology_stack WHERE repository_id = ?", repositoryId);
        if (databases != null && !databases.isEmpty() && !"[]".equals(databases)) {
            md.append("### Databases\n");
            List<String> dbList = parseJsonArray(databases);
            for (String db : dbList) {
                md.append("- ").append(db).append("\n");
            }
            md.append("\n");
        }

        // Build Tools
        String buildTools = querySingleValue("SELECT build_tools FROM technology_stack WHERE repository_id = ?", repositoryId);
        if (buildTools != null && !buildTools.isEmpty() && !"[]".equals(buildTools)) {
            md.append("### Build Tools\n");
            List<String> btList = parseJsonArray(buildTools);
            for (String bt : btList) {
                md.append("- ").append(bt).append("\n");
            }
            md.append("\n");
        }
    }

    private void generateProjectStructure(StringBuilder md, String repositoryId) {
        md.append("## 📁 Project Structure\n\n");

        // Modules
        String childModules = querySingleValue("SELECT child_modules FROM build_metadata WHERE repository_id = ?", repositoryId);
        if (childModules != null && !childModules.isEmpty() && !"[]".equals(childModules)) {
            md.append("### Modules\n");
            List<String> modules = parseJsonArray(childModules);
            for (String module : modules) {
                md.append("- `").append(module).append("`\n");
            }
            md.append("\n");
        }

        // Package count
        Long packageCount = queryLong("SELECT COUNT(DISTINCT package_name) FROM file_index WHERE repository_id = ?", repositoryId);
        Long fileCount = queryLong("SELECT COUNT(*) FROM file_index WHERE repository_id = ?", repositoryId);
        
        md.append("- **Packages**: ").append(packageCount != null ? packageCount : 0).append("\n");
        md.append("- **Source Files**: ").append(fileCount != null ? fileCount : 0).append("\n\n");
    }

    private void generateBuildAndRun(StringBuilder md, String repositoryId) {
        md.append("## 🚀 Build and Run\n\n");

        String buildSystemType = querySingleValue("SELECT build_system_type FROM build_metadata WHERE repository_id = ?", repositoryId);
        String javaVersion = querySingleValue("SELECT java_version FROM build_metadata WHERE repository_id = ?", repositoryId);
        String mavenWrapper = querySingleValue("SELECT maven_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);
        String gradleWrapper = querySingleValue("SELECT gradle_wrapper_present FROM build_metadata WHERE repository_id = ?", repositoryId);

        // Prerequisites
        md.append("### Prerequisites\n\n");
        if (javaVersion != null && !javaVersion.isEmpty()) {
            md.append("- Java ").append(javaVersion).append(" or higher\n");
        } else {
            md.append("- Java 11 or higher (recommended)\n");
        }
        
        if ("MAVEN".equalsIgnoreCase(buildSystemType)) {
            md.append("- Maven 3.6+\n");
        } else if ("GRADLE".equalsIgnoreCase(buildSystemType)) {
            md.append("- Gradle 6.0+\n");
        }
        md.append("\n");

        // Commands
        md.append("### Commands\n\n");
        md.append("| Action | Command |\n");
        md.append("|--------|--------|\n");
        
        if ("MAVEN".equalsIgnoreCase(buildSystemType)) {
            String mvnCmd = "1".equals(mavenWrapper) ? "./mvnw" : "mvn";
            md.append("| **Build** | `").append(mvnCmd).append(" clean install` |\n");
            md.append("| **Run** | `").append(mvnCmd).append(" spring-boot:run` |\n");
            md.append("| **Test** | `").append(mvnCmd).append(" test` |\n");
        } else if ("GRADLE".equalsIgnoreCase(buildSystemType)) {
            String gradleCmd = "1".equals(gradleWrapper) ? "./gradlew" : "gradle";
            md.append("| **Build** | `").append(gradleCmd).append(" clean build` |\n");
            md.append("| **Run** | `").append(gradleCmd).append(" bootRun` |\n");
            md.append("| **Test** | `").append(gradleCmd).append(" test` |\n");
        } else {
            md.append("| **Build** | Depends on build system |\n");
            md.append("| **Run** | Depends on build system |\n");
            md.append("| **Test** | Depends on build system |\n");
        }
        md.append("\n");
    }

    private void generateConfiguration(StringBuilder md, String repositoryId) {
        md.append("## ⚙️ Configuration\n\n");

        Long totalConfigFiles = queryLong("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ?", repositoryId);
        
        if (totalConfigFiles != null && totalConfigFiles > 0) {
            List<String> configFiles = queryList("SELECT file_name FROM configuration_file WHERE repository_id = ? ORDER BY config_type, file_name", repositoryId);
            if (!configFiles.isEmpty()) {
                md.append("### Configuration Files\n\n");
                for (String file : configFiles) {
                    md.append("- `").append(file).append("`\n");
                }
                md.append("\n");
            }
        } else {
            md.append("- Standard Spring Boot configuration (`application.properties` or `application.yml`)\n\n");
        }
    }

    private void generateRestApiOverview(StringBuilder md, String repositoryId) {
        md.append("## 🔌 REST API Overview\n\n");

        Long totalEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ?", repositoryId);
        Long totalControllers = queryLong("SELECT COUNT(DISTINCT controller_name) FROM rest_api_endpoint WHERE repository_id = ?", repositoryId);
        Long getEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'GET'", repositoryId);
        Long postEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'POST'", repositoryId);
        Long putEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'PUT'", repositoryId);
        Long deleteEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'DELETE'", repositoryId);
        Long patchEndpoints = queryLong("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'PATCH'", repositoryId);

        if (totalEndpoints != null && totalEndpoints > 0) {
            md.append("| Metric | Count |\n");
            md.append("|--------|-------|\n");
            md.append("| **Controllers** | ").append(totalControllers != null ? totalControllers : 0).append(" |\n");
            md.append("| **Total Endpoints** | ").append(totalEndpoints).append(" |\n");
            md.append("| **GET** | ").append(getEndpoints != null ? getEndpoints : 0).append(" |\n");
            md.append("| **POST** | ").append(postEndpoints != null ? postEndpoints : 0).append(" |\n");
            md.append("| **PUT** | ").append(putEndpoints != null ? putEndpoints : 0).append(" |\n");
            md.append("| **DELETE** | ").append(deleteEndpoints != null ? deleteEndpoints : 0).append(" |\n");
            md.append("| **PATCH** | ").append(patchEndpoints != null ? patchEndpoints : 0).append(" |\n");
            md.append("\n");
        } else {
            md.append("No REST API endpoints detected.\n\n");
        }
    }

    private void generateSpringOverview(StringBuilder md, String repositoryId) {
        md.append("## 🌱 Spring Overview\n\n");

        Long totalComponents = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ?", repositoryId);
        Long services = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'SERVICE'", repositoryId);
        Long controllers = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND (component_type = 'CONTROLLER' OR component_type = 'REST_CONTROLLER')", repositoryId);
        Long repositories = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'REPOSITORY'", repositoryId);
        Long configurations = queryLong("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'CONFIGURATION'", repositoryId);

        if (totalComponents != null && totalComponents > 0) {
            md.append("| Component Type | Count |\n");
            md.append("|---------------|-------|\n");
            md.append("| **Controllers** | ").append(controllers != null ? controllers : 0).append(" |\n");
            md.append("| **Services** | ").append(services != null ? services : 0).append(" |\n");
            md.append("| **Repositories** | ").append(repositories != null ? repositories : 0).append(" |\n");
            md.append("| **Configurations** | ").append(configurations != null ? configurations : 0).append(" |\n");
            md.append("| **Total** | ").append(totalComponents).append(" |\n");
            md.append("\n");
        } else {
            md.append("No Spring components detected.\n\n");
        }
    }

    private void generateDatabaseOverview(StringBuilder md, String repositoryId) {
        md.append("## 🗄️ Database Overview\n\n");

        Long databasesDetected = queryLong("SELECT COUNT(DISTINCT database_type) FROM database_artifact WHERE repository_id = ?", repositoryId);
        Long migrationScripts = queryLong("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'MIGRATION'", repositoryId);
        Long sqlFiles = queryLong("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'SQL_FILE'", repositoryId);

        if (databasesDetected != null && databasesDetected > 0) {
            List<String> dbTypes = queryList("SELECT DISTINCT database_type FROM database_artifact WHERE repository_id = ?", repositoryId);
            md.append("### Detected Databases\n\n");
            for (String dbType : dbTypes) {
                md.append("- ").append(dbType).append("\n");
            }
            md.append("\n");
        }

        if ((migrationScripts != null && migrationScripts > 0) || (sqlFiles != null && sqlFiles > 0)) {
            md.append("### Database Artifacts\n\n");
            md.append("- **Migration Scripts**: ").append(migrationScripts != null ? migrationScripts : 0).append("\n");
            md.append("- **SQL Files**: ").append(sqlFiles != null ? sqlFiles : 0).append("\n\n");
        }

        if ((databasesDetected == null || databasesDetected == 0) && 
            (migrationScripts == null || migrationScripts == 0) && 
            (sqlFiles == null || sqlFiles == 0)) {
            md.append("No database artifacts detected.\n\n");
        }
    }

    private void generateProjectStatistics(StringBuilder md, String repositoryId) {
        md.append("## 📊 Project Statistics\n\n");

        Long javaFiles = queryLong("SELECT COUNT(*) FROM file_index WHERE repository_id = ?", repositoryId);
        Long packages = queryLong("SELECT COUNT(DISTINCT package_name) FROM file_index WHERE repository_id = ?", repositoryId);
        Long classes = queryLong("SELECT COUNT(*) FROM class_info WHERE repository_id = ? AND class_type = 'CLASS'", repositoryId);
        Long interfaces = queryLong("SELECT COUNT(*) FROM class_info WHERE repository_id = ? AND class_type = 'INTERFACE'", repositoryId);
        Long methods = queryLong("SELECT COUNT(*) FROM method_info WHERE repository_id = ?", repositoryId);
        Long fields = queryLong("SELECT COUNT(*) FROM field_info WHERE repository_id = ?", repositoryId);
        Long dependencies = queryLong("SELECT COUNT(*) FROM dependency WHERE repository_id = ?", repositoryId);

        md.append("| Metric | Count |\n");
        md.append("|--------|-------|\n");
        md.append("| **Java Files** | ").append(javaFiles != null ? javaFiles : 0).append(" |\n");
        md.append("| **Packages** | ").append(packages != null ? packages : 0).append(" |\n");
        md.append("| **Classes** | ").append(classes != null ? classes : 0).append(" |\n");
        md.append("| **Interfaces** | ").append(interfaces != null ? interfaces : 0).append(" |\n");
        md.append("| **Methods** | ").append(methods != null ? methods : 0).append(" |\n");
        md.append("| **Fields** | ").append(fields != null ? fields : 0).append(" |\n");
        md.append("| **Dependencies** | ").append(dependencies != null ? dependencies : 0).append(" |\n");
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
}