package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.model.RepositoryStatus;
import com.projectiq.indexerlocal.model.chat.*;
import com.projectiq.indexerlocal.repository.ChatHistoryRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Repository Chat APIs.
 * Provides context generation, entity search, and chat history management.
 * All operations query only persisted indexed metadata.
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private static final List<String> ALL_ENTITY_TYPES = Arrays.asList(
            "CLASS", "INTERFACE", "ENUM", "RECORD", "METHOD", "FIELD",
            "ANNOTATION", "COMPONENT", "ENDPOINT", "DEPENDENCY", "CONFIGURATION", "DATABASE_ARTIFACT"
    );

    private final RepositoryRepository repositoryRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final SearchService searchService;
    private final RepositoryStatisticsService repositoryStatisticsService;
    private final JdbcTemplate jdbcTemplate;

    public ChatService(RepositoryRepository repositoryRepository,
                       ChatHistoryRepository chatHistoryRepository,
                       SearchService searchService,
                       RepositoryStatisticsService repositoryStatisticsService,
                       JdbcTemplate jdbcTemplate) {
        this.repositoryRepository = repositoryRepository;
        this.chatHistoryRepository = chatHistoryRepository;
        this.searchService = searchService;
        this.repositoryStatisticsService = repositoryStatisticsService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Generate structured repository context from persisted indexed metadata.
     */
    public ChatContextResponse generateContext(String repositoryId, ChatContextRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Chat context request: repositoryId={}, query={}, entityTypes={}",
                repositoryId, request.getQuery(), request.getEntityTypes());

        // Validate repository exists and is indexed
        Repository repo = validateRepository(repositoryId);

        ChatContextResponse response = new ChatContextResponse();

        // Build repository metadata
        ChatContextResponse.RepositoryMetadata metadata = buildRepositoryMetadata(repo, request);
        response.setRepositoryMetadata(metadata);

        // Determine entity types to retrieve
        List<String> entityTypes = request.getEntityTypes();
        if (entityTypes == null || entityTypes.isEmpty()) {
            entityTypes = ALL_ENTITY_TYPES;
        }

        // Retrieve entities by type
        Map<String, List<Map<String, Object>>> entities = new LinkedHashMap<>();
        Map<String, Integer> entityCountsByType = new LinkedHashMap<>();
        int totalEntities = 0;
        int maxPerType = request.getMaxEntitiesPerType() != null ? request.getMaxEntitiesPerType() : 50;

        for (String entityType : entityTypes) {
            List<Map<String, Object>> typeEntities = retrieveEntitiesByType(
                    repositoryId, entityType.toUpperCase(), request.getQuery(), maxPerType);
            entities.put(entityType.toUpperCase(), typeEntities);
            entityCountsByType.put(entityType.toUpperCase(), typeEntities.size());
            totalEntities += typeEntities.size();
        }

        response.setEntities(entities);

        // Build context summary
        ChatContextResponse.ContextSummary summary = new ChatContextResponse.ContextSummary();
        summary.setTotalEntitiesRetrieved(totalEntities);
        summary.setEntityTypesIncluded(new ArrayList<>(entityCountsByType.keySet()));
        summary.setEntityCountsByType(entityCountsByType);
        summary.setQueryUsed(request.getQuery());
        summary.setMaxEntitiesPerType(maxPerType);
        response.setContextSummary(summary);

        // Include statistics if requested
        if (request.getIncludeStatistics() == null || request.getIncludeStatistics()) {
            try {
                Map<String, Object> stats = repositoryStatisticsService.getRepositoryStatistics(repositoryId);
                response.setStatistics(stats);
            } catch (Exception e) {
                logger.warn("Failed to retrieve statistics for repository {}: {}", repositoryId, e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        response.setExecutionDurationMs(duration);
        response.setGeneratedAt(LocalDateTime.now());

        // Persist chat history
        persistChatHistory(repositoryId, "CONTEXT", request.getQuery(),
                String.join(",", entityTypes), totalEntities, duration);

        logger.info("Chat context generated: repositoryId={}, entities={}, duration={}ms",
                repositoryId, totalEntities, duration);

        return response;
    }

    /**
     * Search indexed entities matching supplied criteria.
     */
    public ChatSearchResponse searchEntities(String repositoryId, ChatSearchRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Chat search request: repositoryId={}, query={}, entityType={}",
                repositoryId, request.getQuery(), request.getEntityType());

        // Validate repository exists and is indexed
        validateRepository(repositoryId);

        ChatSearchResponse response = new ChatSearchResponse();
        response.setRepositoryId(repositoryId);

        String query = request.getQuery() != null ? request.getQuery() : "";
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        List<Map<String, Object>> results;
        long totalElements;

        String entityType = request.getEntityType();
        if (entityType != null && !entityType.isEmpty()) {
            entityType = entityType.toUpperCase();
            switch (entityType) {
                case "CLASS":
                case "INTERFACE":
                case "ENUM":
                case "RECORD":
                    var classResults = searchService.searchClasses(repositoryId, query,
                            request.getPackageName(), page, size, "name", "ASC");
                    results = classResults.getContent();
                    totalElements = classResults.getTotalElements();
                    break;
                case "METHOD":
                    var methodResults = searchService.searchMethods(repositoryId, query,
                            request.getPackageName(), page, size, "name", "ASC");
                    results = methodResults.getContent();
                    totalElements = methodResults.getTotalElements();
                    break;
                case "FIELD":
                    var fieldResults = searchService.searchFields(repositoryId, query,
                            request.getPackageName(), page, size, "name", "ASC");
                    results = fieldResults.getContent();
                    totalElements = fieldResults.getTotalElements();
                    break;
                case "ANNOTATION":
                    var annotationResults = searchService.searchAnnotations(repositoryId, query,
                            page, size, "name", "ASC");
                    results = annotationResults.getContent();
                    totalElements = annotationResults.getTotalElements();
                    break;
                case "COMPONENT":
                    var componentResults = searchService.searchComponents(repositoryId, query,
                            request.getPackageName(), request.getAnnotation(), page, size, "name", "ASC");
                    results = componentResults.getContent();
                    totalElements = componentResults.getTotalElements();
                    break;
                case "ENDPOINT":
                    var endpointResults = searchService.searchRestEndpoints(repositoryId, query,
                            request.getHttpMethod(), page, size, "name", "ASC");
                    results = endpointResults.getContent();
                    totalElements = endpointResults.getTotalElements();
                    break;
                case "DEPENDENCY":
                    var depResults = searchService.searchDependencies(repositoryId, query,
                            null, null, page, size, "name", "ASC");
                    results = depResults.getContent();
                    totalElements = depResults.getTotalElements();
                    break;
                case "CONFIGURATION":
                    results = searchConfigurations(repositoryId, query, page, size);
                    totalElements = results.size();
                    break;
                case "DATABASE_ARTIFACT":
                    results = searchDatabaseArtifacts(repositoryId, query, page, size);
                    totalElements = results.size();
                    break;
                default:
                    // General search across all types
                    var generalResults = searchService.generalSearch(repositoryId, query,
                            request.getPackageName(), request.getAnnotation());
                    int fromIndex = page * size;
                    int toIndex = Math.min(fromIndex + size, generalResults.size());
                    results = fromIndex < generalResults.size()
                            ? generalResults.subList(fromIndex, toIndex)
                            : Collections.emptyList();
                    totalElements = generalResults.size();
                    break;
            }
        } else {
            // General search across all types
            var generalResults = searchService.generalSearch(repositoryId, query,
                    request.getPackageName(), request.getAnnotation());
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, generalResults.size());
            results = fromIndex < generalResults.size()
                    ? generalResults.subList(fromIndex, toIndex)
                    : Collections.emptyList();
            totalElements = generalResults.size();
        }

        response.setEntities(results);
        response.setTotalResults(totalElements);
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages((int) Math.ceil((double) totalElements / size));
        response.setEntityTypeSearched(entityType);
        response.setQueryUsed(query);

        long duration = System.currentTimeMillis() - startTime;
        response.setExecutionDurationMs(duration);
        response.setGeneratedAt(LocalDateTime.now());

        // Persist chat history
        persistChatHistory(repositoryId, "SEARCH", query,
                entityType != null ? entityType : "ALL", results.size(), duration);

        logger.info("Chat search completed: repositoryId={}, results={}, duration={}ms",
                repositoryId, totalElements, duration);

        return response;
    }

    /**
     * Retrieve chat request history for a repository.
     */
    public List<ChatHistory> getChatHistory(String repositoryId) {
        logger.info("Retrieving chat history for repositoryId={}", repositoryId);

        // Validate repository exists
        validateRepository(repositoryId);

        List<ChatHistory> history = chatHistoryRepository.findByRepositoryId(repositoryId);
        logger.info("Chat history retrieved: repositoryId={}, count={}", repositoryId, history.size());
        return history;
    }

    /**
     * Validate that the repository exists and is indexed.
     */
    private Repository validateRepository(String repositoryId) {
        Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo == null) {
            throw new IllegalArgumentException("Repository not found: " + repositoryId);
        }
        if (repo.getStatus() != RepositoryStatus.INDEXED && repo.getStatus() != RepositoryStatus.READY) {
            throw new IllegalStateException("Repository is not indexed. Current status: " +
                    (repo.getStatus() != null ? repo.getStatus().name() : "NULL"));
        }
        return repo;
    }

    /**
     * Build repository metadata for the context response.
     */
    private ChatContextResponse.RepositoryMetadata buildRepositoryMetadata(Repository repo, ChatContextRequest request) {
        ChatContextResponse.RepositoryMetadata metadata = new ChatContextResponse.RepositoryMetadata();
        metadata.setRepositoryId(repo.getRepositoryId());
        metadata.setRepositoryName(repo.getRepositoryName());

        String repositoryId = repo.getRepositoryId();

        // Build system
        if (request.getIncludeBuildSystem() == null || request.getIncludeBuildSystem()) {
            metadata.setBuildSystem(getBuildSystem(repositoryId));
        }

        // Technology stack
        if (request.getIncludeTechnologyStack() == null || request.getIncludeTechnologyStack()) {
            metadata.setTechnologyStack(getTechnologyStack(repositoryId));
        }

        // Project summary
        if (request.getIncludeProjectSummary() == null || request.getIncludeProjectSummary()) {
            metadata.setProjectSummary(buildProjectSummary(repositoryId));
        }

        // Configuration summary
        if (request.getIncludeConfigurationSummary() == null || request.getIncludeConfigurationSummary()) {
            metadata.setConfigurationSummary(getConfigurationSummary(repositoryId));
        }

        // Database summary
        if (request.getIncludeDatabaseSummary() == null || request.getIncludeDatabaseSummary()) {
            metadata.setDatabaseSummary(getDatabaseSummary(repositoryId));
        }

        // Spring component summary
        if (request.getIncludeSpringComponentSummary() == null || request.getIncludeSpringComponentSummary()) {
            metadata.setSpringComponentSummary(getSpringComponentSummary(repositoryId));
        }

        // REST API summary
        if (request.getIncludeRestApiSummary() == null || request.getIncludeRestApiSummary()) {
            metadata.setRestApiSummary(getRestApiSummary(repositoryId));
        }

        return metadata;
    }

    /**
     * Retrieve entities by type with optional query filter.
     */
    private List<Map<String, Object>> retrieveEntitiesByType(String repositoryId, String entityType,
                                                              String query, int maxPerType) {
        String q = query != null ? query : "";
        try {
            switch (entityType) {
                case "CLASS":
                case "INTERFACE":
                case "ENUM":
                case "RECORD":
                    var classResults = searchService.searchClasses(repositoryId, q, null, 0, maxPerType, "name", "ASC");
                    return classResults.getContent();
                case "METHOD":
                    var methodResults = searchService.searchMethods(repositoryId, q, null, 0, maxPerType, "name", "ASC");
                    return methodResults.getContent();
                case "FIELD":
                    var fieldResults = searchService.searchFields(repositoryId, q, null, 0, maxPerType, "name", "ASC");
                    return fieldResults.getContent();
                case "ANNOTATION":
                    var annotationResults = searchService.searchAnnotations(repositoryId, q, 0, maxPerType, "name", "ASC");
                    return annotationResults.getContent();
                case "COMPONENT":
                    var componentResults = searchService.searchComponents(repositoryId, q, null, null, 0, maxPerType, "name", "ASC");
                    return componentResults.getContent();
                case "ENDPOINT":
                    var endpointResults = searchService.searchRestEndpoints(repositoryId, q, null, 0, maxPerType, "name", "ASC");
                    return endpointResults.getContent();
                case "DEPENDENCY":
                    var depResults = searchService.searchDependencies(repositoryId, q, null, null, 0, maxPerType, "name", "ASC");
                    return depResults.getContent();
                case "CONFIGURATION":
                    return searchConfigurations(repositoryId, q, 0, maxPerType);
                case "DATABASE_ARTIFACT":
                    return searchDatabaseArtifacts(repositoryId, q, 0, maxPerType);
                default:
                    return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.warn("Error retrieving entities of type {} for repository {}: {}", entityType, repositoryId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Search configuration files from persisted index.
     */
    private List<Map<String, Object>> searchConfigurations(String repositoryId, String query, int page, int size) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT * FROM configuration_file WHERE repository_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(repositoryId);

            if (query != null && !query.isEmpty()) {
                sql.append(" AND (file_name LIKE ? OR config_type LIKE ? OR file_path LIKE ?)");
                String likeQuery = "%" + query.toLowerCase() + "%";
                params.add(likeQuery);
                params.add(likeQuery);
                params.add(likeQuery);
            }

            sql.append(" ORDER BY file_name ASC LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);

            return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("fileName", rs.getString("file_name"));
                map.put("filePath", rs.getString("file_path"));
                map.put("configType", rs.getString("config_type"));
                map.put("fileSize", rs.getObject("file_size"));
                return map;
            }, params.toArray());
        } catch (Exception e) {
            logger.warn("Error searching configurations for repository {}: {}", repositoryId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Search database artifacts from persisted index.
     */
    private List<Map<String, Object>> searchDatabaseArtifacts(String repositoryId, String query, int page, int size) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT * FROM database_artifact WHERE repository_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(repositoryId);

            if (query != null && !query.isEmpty()) {
                sql.append(" AND (artifact_name LIKE ? OR database_type LIKE ? OR artifact_type LIKE ?)");
                String likeQuery = "%" + query.toLowerCase() + "%";
                params.add(likeQuery);
                params.add(likeQuery);
                params.add(likeQuery);
            }

            sql.append(" ORDER BY artifact_name ASC LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);

            return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("artifactName", rs.getString("artifact_name"));
                map.put("artifactType", rs.getString("artifact_type"));
                map.put("databaseType", rs.getString("database_type"));
                map.put("filePath", rs.getString("file_path"));
                return map;
            }, params.toArray());
        } catch (Exception e) {
            logger.warn("Error searching database artifacts for repository {}: {}", repositoryId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getBuildSystem(String repositoryId) {
        try {
            String result = jdbcTemplate.queryForObject(
                    "SELECT build_system FROM build_metadata WHERE repository_id = ?",
                    String.class, repositoryId);
            return result != null ? result : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String getTechnologyStack(String repositoryId) {
        try {
            String result = jdbcTemplate.queryForObject(
                    "SELECT GROUP_CONCAT(technology_name, ', ') FROM technology_stack WHERE repository_id = ?",
                    String.class, repositoryId);
            return result != null ? result : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String buildProjectSummary(String repositoryId) {
        try {
            Long totalClasses = queryCount("SELECT COUNT(*) FROM class_info WHERE repository_id = ?", repositoryId);
            Long totalMethods = queryCount("SELECT COUNT(*) FROM method_info WHERE repository_id = ?", repositoryId);
            Long totalFields = queryCount("SELECT COUNT(*) FROM field_info WHERE repository_id = ?", repositoryId);
            Long totalEndpoints = queryCount("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ?", repositoryId);
            Long totalComponents = queryCount("SELECT COUNT(*) FROM spring_component WHERE repository_id = ?", repositoryId);
            Long totalDeps = queryCount("SELECT COUNT(*) FROM dependency WHERE repository_id = ?", repositoryId);

            return String.format("Project contains %d classes, %d methods, %d fields, %d REST endpoints, %d Spring components, and %d dependencies.",
                    totalClasses, totalMethods, totalFields, totalEndpoints, totalComponents, totalDeps);
        } catch (Exception e) {
            return "Project summary unavailable";
        }
    }

    private Map<String, Object> getConfigurationSummary(String repositoryId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            summary.put("totalConfigFiles", queryCount("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ?", repositoryId));
            summary.put("springConfigs", queryCount("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'SPRING'", repositoryId));
            summary.put("dockerConfigs", queryCount("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'DOCKER'", repositoryId));
            summary.put("ciCdConfigs", queryCount("SELECT COUNT(*) FROM configuration_file WHERE repository_id = ? AND config_type = 'CI_CD'", repositoryId));
        } catch (Exception e) {
            logger.warn("Error getting configuration summary for repository {}: {}", repositoryId, e.getMessage());
        }
        return summary;
    }

    private Map<String, Object> getDatabaseSummary(String repositoryId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            summary.put("databasesDetected", queryCount("SELECT COUNT(DISTINCT database_type) FROM database_artifact WHERE repository_id = ?", repositoryId));
            summary.put("totalArtifacts", queryCount("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ?", repositoryId));
            summary.put("datasources", queryCount("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'DATASOURCE'", repositoryId));
            summary.put("sqlFiles", queryCount("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'SQL_FILE'", repositoryId));
            summary.put("migrationScripts", queryCount("SELECT COUNT(*) FROM database_artifact WHERE repository_id = ? AND artifact_type = 'MIGRATION'", repositoryId));
        } catch (Exception e) {
            logger.warn("Error getting database summary for repository {}: {}", repositoryId, e.getMessage());
        }
        return summary;
    }

    private Map<String, Object> getSpringComponentSummary(String repositoryId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            summary.put("totalComponents", queryCount("SELECT COUNT(*) FROM spring_component WHERE repository_id = ?", repositoryId));
            summary.put("services", queryCount("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'SERVICE'", repositoryId));
            summary.put("controllers", queryCount("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'CONTROLLER'", repositoryId));
            summary.put("restControllers", queryCount("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'REST_CONTROLLER'", repositoryId));
            summary.put("repositories", queryCount("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'REPOSITORY'", repositoryId));
            summary.put("configurations", queryCount("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'CONFIGURATION'", repositoryId));
            summary.put("beans", queryCount("SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND component_type = 'BEAN'", repositoryId));
        } catch (Exception e) {
            logger.warn("Error getting Spring component summary for repository {}: {}", repositoryId, e.getMessage());
        }
        return summary;
    }

    private Map<String, Object> getRestApiSummary(String repositoryId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            summary.put("totalEndpoints", queryCount("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ?", repositoryId));
            summary.put("getEndpoints", queryCount("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'GET'", repositoryId));
            summary.put("postEndpoints", queryCount("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'POST'", repositoryId));
            summary.put("putEndpoints", queryCount("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'PUT'", repositoryId));
            summary.put("deleteEndpoints", queryCount("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'DELETE'", repositoryId));
            summary.put("patchEndpoints", queryCount("SELECT COUNT(*) FROM rest_api_endpoint WHERE repository_id = ? AND http_method = 'PATCH'", repositoryId));
            summary.put("totalControllers", queryCount("SELECT COUNT(DISTINCT controller_name) FROM rest_api_endpoint WHERE repository_id = ?", repositoryId));
        } catch (Exception e) {
            logger.warn("Error getting REST API summary for repository {}: {}", repositoryId, e.getMessage());
        }
        return summary;
    }

    private Long queryCount(String sql, String repositoryId) {
        try {
            Number result = jdbcTemplate.queryForObject(sql, Long.class, repositoryId);
            return result != null ? result.longValue() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Persist chat request history.
     */
    private void persistChatHistory(String repositoryId, String requestType, String query,
                                     String entityTypes, int entitiesRetrieved, long durationMs) {
        try {
            ChatHistory history = new ChatHistory();
            history.setRepositoryId(repositoryId);
            history.setRequestType(requestType);
            history.setQuery(query);
            history.setEntityTypes(entityTypes);
            history.setEntitiesRetrieved(entitiesRetrieved);
            history.setExecutionDurationMs(durationMs);
            history.setRequestTimestamp(LocalDateTime.now());
            chatHistoryRepository.save(history);
        } catch (Exception e) {
            logger.warn("Failed to persist chat history for repository {}: {}", repositoryId, e.getMessage());
        }
    }
}