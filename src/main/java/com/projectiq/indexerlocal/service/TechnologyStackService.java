package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.analyzer.TechnologyStackAnalyzer;
import com.projectiq.indexerlocal.model.BuildMetadata;
import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.model.TechnologyStack;
import com.projectiq.indexerlocal.repository.ProjectStructureRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for detecting and managing technology stack information.
 */
@Service
public class TechnologyStackService {

    private static final Logger logger = LoggerFactory.getLogger(TechnologyStackService.class);

    private final TechnologyStackAnalyzer technologyStackAnalyzer;
    private final RepositoryRepository repositoryRepository;
    private final ProjectStructureRepository projectStructureRepository;
    private final BuildSystemService buildSystemService;
    private final JdbcTemplate jdbcTemplate;

    public TechnologyStackService(TechnologyStackAnalyzer technologyStackAnalyzer,
                                  RepositoryRepository repositoryRepository,
                                  ProjectStructureRepository projectStructureRepository,
                                  BuildSystemService buildSystemService,
                                  JdbcTemplate jdbcTemplate) {
        this.technologyStackAnalyzer = technologyStackAnalyzer;
        this.repositoryRepository = repositoryRepository;
        this.projectStructureRepository = projectStructureRepository;
        this.buildSystemService = buildSystemService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Detect the technology stack of a repository.
     */
    public TechnologyStack detectTechnologyStack(String repositoryId) {
        // Validate repository exists
        Repository repository = validateRepositoryExists(repositoryId);

        // Check if project structure has been analyzed
        long fileCount = projectStructureRepository.countFilesByRepositoryId(repositoryId);
        if (fileCount == 0) {
            throw new IllegalStateException("Project structure must be analyzed before detecting technology stack for repository: " + repositoryId);
        }

        // Check if build system analysis has been completed
        BuildMetadata buildMetadata = getBuildMetadataFromStorage(repositoryId);
        if (buildMetadata == null || buildMetadata.getAnalyzedAt() == null) {
            throw new IllegalStateException("Build system analysis must be completed before detecting technology stack for repository: " + repositoryId);
        }

        logger.info("Starting technology stack detection for repository: {}", repositoryId);
        long startTime = System.currentTimeMillis();

        Path workspacePath = Path.of(repository.getWorkspacePath());

        // Perform technology stack detection
        TechnologyStack techStack = technologyStackAnalyzer.detectTechnologyStack(repositoryId, workspacePath);

        // Persist technology stack information
        persistTechnologyStack(repositoryId, techStack);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Technology stack detection completed for repository: {}. Duration: {}ms", repositoryId, duration);

        return techStack;
    }

    /**
     * Get the detected technology stack for a repository.
     */
    public TechnologyStack getTechnologyStack(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return getTechnologyStackFromStorage(repositoryId);
    }

    // ==================== Private Methods ====================

    private Repository validateRepositoryExists(String repositoryId) {
        Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with ID '" + repositoryId + "' not found");
        }
        return repository;
    }

    private BuildMetadata getBuildMetadataFromStorage(String repositoryId) {
        return buildSystemService.getBuild(repositoryId);
    }

    /**
     * Create the technology_stack table if it doesn't exist.
     */
    public void initTechnologyStackSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS technology_stack (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "repository_id TEXT UNIQUE NOT NULL, " +
                "languages TEXT, " +
                "frameworks TEXT, " +
                "build_tools TEXT, " +
                "databases TEXT, " +
                "testing_frameworks TEXT, " +
                "frontend_technologies TEXT, " +
                "detected_at TIMESTAMP, " +
                "FOREIGN KEY (repository_id) REFERENCES repository(repository_id))";
        jdbcTemplate.execute(sql);
    }

    /**
     * Persist technology stack information to the database.
     */
    private void persistTechnologyStack(String repositoryId, TechnologyStack techStack) {
        initTechnologyStackSchema();

        try {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT OR REPLACE INTO technology_stack (");
            sql.append("repository_id, languages, frameworks, build_tools, ");
            sql.append("databases, testing_frameworks, frontend_technologies, detected_at) ");
            sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

            String languagesJson = techStack.getLanguages().stream()
                    .map(l -> String.format("{\"name\":\"%s\",\"confidence\":%.2f}", l.getName(), l.getConfidence()))
                    .collect(Collectors.joining(",", "[", "]"));

            String frameworksJson = techStack.getFrameworks().stream()
                    .map(f -> String.format("{\"name\":\"%s\",\"confidence\":%.2f}", f.getName(), f.getConfidence()))
                    .collect(Collectors.joining(",", "[", "]"));

            String buildToolsJson = "[" + String.join(",", techStack.getBuildTools().stream().map(s -> "\"" + s + "\"").collect(Collectors.toList())) + "]";
            String databasesJson = "[" + String.join(",", techStack.getDatabases().stream().map(s -> "\"" + s + "\"").collect(Collectors.toList())) + "]";
            String testingFrameworksJson = "[" + String.join(",", techStack.getTestingFrameworks().stream().map(s -> "\"" + s + "\"").collect(Collectors.toList())) + "]";
            String frontendTechsJson = "[" + String.join(",", techStack.getFrontendTechnologies().stream().map(s -> "\"" + s + "\"").collect(Collectors.toList())) + "]";

            jdbcTemplate.update(sql.toString(),
                    repositoryId,
                    languagesJson.isEmpty() ? "[]" : languagesJson,
                    frameworksJson.isEmpty() ? "[]" : frameworksJson,
                    buildToolsJson.isEmpty() ? "[]" : buildToolsJson,
                    databasesJson.isEmpty() ? "[]" : databasesJson,
                    testingFrameworksJson.isEmpty() ? "[]" : testingFrameworksJson,
                    frontendTechsJson.isEmpty() ? "[]" : frontendTechsJson,
                    Timestamp.valueOf(LocalDateTime.now())
            );

            logger.info("Technology stack persisted for repository: {}", repositoryId);
        } catch (Exception e) {
            logger.error("Failed to persist technology stack for repository: {}", repositoryId, e);
        }
    }

    /**
     * Retrieve technology stack information from storage.
     */
    private TechnologyStack getTechnologyStackFromStorage(String repositoryId) {
        initTechnologyStackSchema();

        try {
            String sql = "SELECT * FROM technology_stack WHERE repository_id = ?";
            List<TechnologyStack> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                TechnologyStack techStack = new TechnologyStack();
                techStack.setRepositoryId(rs.getString("repository_id"));

                // Parse languages
                String languagesJson = rs.getString("languages");
                if (languagesJson != null && !languagesJson.isEmpty() && !"[]".equals(languagesJson)) {
                    techStack.setLanguages(parseLanguagesFromJson(languagesJson));
                }

                // Parse frameworks
                String frameworksJson = rs.getString("frameworks");
                if (frameworksJson != null && !frameworksJson.isEmpty() && !"[]".equals(frameworksJson)) {
                    techStack.setFrameworks(parseFrameworksFromJson(frameworksJson));
                }

                // Parse build tools
                String buildToolsJson = rs.getString("build_tools");
                if (buildToolsJson != null && !buildToolsJson.isEmpty() && !"[]".equals(buildToolsJson)) {
                    techStack.setBuildTools(parseStringListFromJson(buildToolsJson));
                }

                // Parse databases
                String databasesJson = rs.getString("databases");
                if (databasesJson != null && !databasesJson.isEmpty() && !"[]".equals(databasesJson)) {
                    techStack.setDatabases(parseStringListFromJson(databasesJson));
                }

                // Parse testing frameworks
                String testingFrameworksJson = rs.getString("testing_frameworks");
                if (testingFrameworksJson != null && !testingFrameworksJson.isEmpty() && !"[]".equals(testingFrameworksJson)) {
                    techStack.setTestingFrameworks(parseStringListFromJson(testingFrameworksJson));
                }

                // Parse frontend technologies
                String frontendTechsJson = rs.getString("frontend_technologies");
                if (frontendTechsJson != null && !frontendTechsJson.isEmpty() && !"[]".equals(frontendTechsJson)) {
                    techStack.setFrontendTechnologies(parseStringListFromJson(frontendTechsJson));
                }

                java.sql.Timestamp detectedAt = rs.getTimestamp("detected_at");
                if (detectedAt != null) {
                    techStack.setDetectedAt(detectedAt.toLocalDateTime());
                }

                return techStack;
            }, repositoryId);

            return results.isEmpty() ? new TechnologyStack() : results.get(0);
        } catch (Exception e) {
            logger.warn("No technology stack found for repository: {}", repositoryId);
            return new TechnologyStack();
        }
    }

    /**
     * Parse language list from JSON string.
     */
    private List<TechnologyStack.LanguageInfo> parseLanguagesFromJson(String json) {
        List<TechnologyStack.LanguageInfo> languages = new ArrayList<>();
        // Simple JSON parsing without external dependencies
        if (json == null || json.length() < 2) {
            return languages;
        }
        
        try {
            String inner = json.substring(1, json.length() - 1); // Remove [ and ]
            if (inner.isEmpty()) {
                return languages;
            }
            
            // Parse each language object
            int startIndex = 0;
            while (startIndex < inner.length()) {
                // Find object start
                int objStart = inner.indexOf('{', startIndex);
                if (objStart == -1) break;
                
                // Find object end
                int objEnd = findMatchingBracket(inner, objStart);
                if (objEnd == -1) break;
                
                String objStr = inner.substring(objStart + 1, objEnd);
                
                // Extract name
                String name = extractStringValue(objStr, "name");
                // Extract confidence
                double confidence = extractDoubleValue(objStr, "confidence");
                
                if (name != null && !name.isEmpty()) {
                    languages.add(new TechnologyStack.LanguageInfo(name, confidence));
                }
                
                startIndex = objEnd + 1;
            }
        } catch (Exception e) {
            logger.warn("Failed to parse languages JSON: {}", json, e);
        }
        
        return languages;
    }

    /**
     * Parse framework list from JSON string.
     */
    private List<TechnologyStack.FrameworkInfo> parseFrameworksFromJson(String json) {
        List<TechnologyStack.FrameworkInfo> frameworks = new ArrayList<>();
        if (json == null || json.length() < 2) {
            return frameworks;
        }
        
        try {
            String inner = json.substring(1, json.length() - 1);
            if (inner.isEmpty()) {
                return frameworks;
            }
            
            int startIndex = 0;
            while (startIndex < inner.length()) {
                int objStart = inner.indexOf('{', startIndex);
                if (objStart == -1) break;
                
                int objEnd = findMatchingBracket(inner, objStart);
                if (objEnd == -1) break;
                
                String objStr = inner.substring(objStart + 1, objEnd);
                
                String name = extractStringValue(objStr, "name");
                double confidence = extractDoubleValue(objStr, "confidence");
                
                if (name != null && !name.isEmpty()) {
                    frameworks.add(new TechnologyStack.FrameworkInfo(name, confidence));
                }
                
                startIndex = objEnd + 1;
            }
        } catch (Exception e) {
            logger.warn("Failed to parse frameworks JSON: {}", json, e);
        }
        
        return frameworks;
    }

    /**
     * Parse simple string list from JSON array string.
     */
    private List<String> parseStringListFromJson(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.length() < 2) {
            return list;
        }
        
        try {
            String inner = json.substring(1, json.length() - 1);
            if (inner.isEmpty()) {
                return list;
            }
            
            // Split by "," but be careful of quoted strings
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '"') {
                    // Skip the quote
                    continue;
                } else if (c == ',') {
                    String value = current.toString().trim();
                    if (!value.isEmpty()) {
                        list.add(value);
                    }
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            
            // Add last element
            String lastValue = current.toString().trim();
            if (!lastValue.isEmpty()) {
                list.add(lastValue);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse string list JSON: {}", json, e);
        }
        
        return list;
    }

    /**
     * Extract a string value from a JSON object string.
     */
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

    /**
     * Extract a double value from a JSON object string.
     */
    private double extractDoubleValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int startIndex = json.indexOf(pattern);
        if (startIndex == -1) return 0.0;
        
        int colonIndex = json.indexOf(':', startIndex + pattern.length());
        if (colonIndex == -1) return 0.0;
        
        // Find the number start
        int numStart = colonIndex + 1;
        while (numStart < json.length() && json.charAt(numStart) == ' ') {
            numStart++;
        }
        
        int numEnd = numStart;
        while (numEnd < json.length() && Character.isDigit(json.charAt(numEnd))) {
            numEnd++;
        }
        
        try {
            return Double.parseDouble(json.substring(numStart, numEnd));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Find the matching closing bracket for an opening brace.
     */
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