package com.projectiq.indexerlocal.service.impl;

import com.projectiq.indexerlocal.model.springsearch.SpringSearchResult;
import com.projectiq.indexerlocal.service.SpringSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of SpringSearchService that queries the indexed SQLite database
 * for fast, deterministic discovery of Spring Framework artifacts.
 * <p>
 * Reuses the spring_component table which stores all Spring component metadata
 * including components, controllers, services, repositories, configuration classes,
 * beans, REST endpoints, scheduled tasks, and event listeners.
 */
@Service
public class SpringSearchServiceImpl implements SpringSearchService {

    private static final Logger log = LoggerFactory.getLogger(SpringSearchServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public SpringSearchServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== Select Column Fragment ====================

    private static final String SELECT_COLUMNS =
        "SELECT sc.component_name, sc.component_type, sc.class_name, sc.package_name, " +
        "sc.repository_id, sc.source_file, sc.bean_name, sc.detected_at " +
        "FROM spring_component sc ";

    // ==================== Controllers ====================

    @Override
    public List<SpringSearchResult> findControllers(String repositoryId, String packageName,
                                                     String module, int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
        List<Object> params = new ArrayList<>();
        sql.append("WHERE (sc.is_controller = 1 OR sc.is_rest_controller = 1)");
        applyBasicFilters(sql, params, repositoryId, packageName, module);
        sql.append(" ORDER BY sc.component_name ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);
        return mapResults(sql.toString(), params);
    }

    @Override
    public long countControllers(String repositoryId, String packageName, String module) {
        return countWithFilters(
            "WHERE (sc.is_controller = 1 OR sc.is_rest_controller = 1)",
            repositoryId, packageName, module);
    }

    // ==================== Endpoints ====================

    @Override
    public List<SpringSearchResult> findEndpoints(String repositoryId, String httpMethod,
                                                    String path, String controllerName,
                                                    String packageName, String module,
                                                    int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
        List<Object> params = new ArrayList<>();
        sql.append("WHERE sc.component_type = 'REST_ENDPOINT'");
        applyBasicFilters(sql, params, repositoryId, packageName, module);

        if (httpMethod != null && !httpMethod.isEmpty()) {
            sql.append(" AND sc.component_name LIKE ?");
            params.add(httpMethod.toUpperCase() + " %");
        }

        if (path != null && !path.isEmpty()) {
            sql.append(" AND sc.component_name LIKE ?");
            params.add("%" + path + "%");
        }

        if (controllerName != null && !controllerName.isEmpty()) {
            sql.append(" AND sc.class_name LIKE ?");
            params.add("%" + controllerName + "%");
        }

        sql.append(" ORDER BY sc.component_name ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);
        return mapResults(sql.toString(), params);
    }

    @Override
    public long countEndpoints(String repositoryId, String httpMethod, String path,
                                String controllerName, String packageName, String module) {
        StringBuilder conditions = new StringBuilder("WHERE sc.component_type = 'REST_ENDPOINT'");
        List<Object> params = new ArrayList<>();

        appendFilterCondition(conditions, params, repositoryId, packageName, module);

        if (httpMethod != null && !httpMethod.isEmpty()) {
            conditions.append(" AND sc.component_name LIKE ?");
            params.add(httpMethod.toUpperCase() + " %");
        }
        if (path != null && !path.isEmpty()) {
            conditions.append(" AND sc.component_name LIKE ?");
            params.add("%" + path + "%");
        }
        if (controllerName != null && !controllerName.isEmpty()) {
            conditions.append(" AND sc.class_name LIKE ?");
            params.add("%" + controllerName + "%");
        }

        return countWithConditions(conditions.toString(), params);
    }

    // ==================== Services ====================

    @Override
    public List<SpringSearchResult> findServices(String repositoryId, String packageName,
                                                   String module, int page, int size) {
        return findByFlag("is_service", repositoryId, packageName, module, page, size);
    }

    @Override
    public long countServices(String repositoryId, String packageName, String module) {
        return countByFlag("is_service", repositoryId, packageName, module);
    }

    // ==================== Repositories ====================

    @Override
    public List<SpringSearchResult> findRepositories(String repositoryId, String packageName,
                                                       String module, int page, int size) {
        return findByFlag("is_repository", repositoryId, packageName, module, page, size);
    }

    @Override
    public long countRepositories(String repositoryId, String packageName, String module) {
        return countByFlag("is_repository", repositoryId, packageName, module);
    }

    // ==================== Components ====================

    @Override
    public List<SpringSearchResult> findComponents(String repositoryId, String packageName,
                                                     String module, int page, int size) {
        return findByFlag("is_component", repositoryId, packageName, module, page, size);
    }

    @Override
    public long countComponents(String repositoryId, String packageName, String module) {
        return countByFlag("is_component", repositoryId, packageName, module);
    }

    // ==================== Configuration Classes ====================

    @Override
    public List<SpringSearchResult> findConfigurationClasses(String repositoryId, String packageName,
                                                               String module, int page, int size) {
        return findByFlag("is_configuration", repositoryId, packageName, module, page, size);
    }

    @Override
    public long countConfigurationClasses(String repositoryId, String packageName, String module) {
        return countByFlag("is_configuration", repositoryId, packageName, module);
    }

    // ==================== Beans ====================

    @Override
    public List<SpringSearchResult> findBeans(String repositoryId, String packageName,
                                                String beanName, String module, int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
        List<Object> params = new ArrayList<>();
        sql.append("WHERE sc.is_bean = 1");
        applyBasicFilters(sql, params, repositoryId, packageName, module);

        if (beanName != null && !beanName.isEmpty()) {
            sql.append(" AND sc.bean_name LIKE ?");
            params.add("%" + beanName + "%");
        }

        sql.append(" ORDER BY sc.component_name ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);
        return mapResults(sql.toString(), params);
    }

    @Override
    public long countBeans(String repositoryId, String packageName, String beanName, String module) {
        StringBuilder conditions = new StringBuilder("WHERE sc.is_bean = 1");
        List<Object> params = new ArrayList<>();
        appendFilterCondition(conditions, params, repositoryId, packageName, module);

        if (beanName != null && !beanName.isEmpty()) {
            conditions.append(" AND sc.bean_name LIKE ?");
            params.add("%" + beanName + "%");
        }

        return countWithConditions(conditions.toString(), params);
    }

    // ==================== Scheduled Tasks ====================

    @Override
    public List<SpringSearchResult> findScheduledTasks(String repositoryId, String packageName,
                                                         String module, int page, int size) {
        return findByFlag("has_scheduled", repositoryId, packageName, module, page, size);
    }

    @Override
    public long countScheduledTasks(String repositoryId, String packageName, String module) {
        return countByFlag("has_scheduled", repositoryId, packageName, module);
    }

    // ==================== Event Listeners ====================

    @Override
    public List<SpringSearchResult> findEventListeners(String repositoryId, String packageName,
                                                         String module, int page, int size) {
        return findByFlag("has_event_listener", repositoryId, packageName, module, page, size);
    }

    @Override
    public long countEventListeners(String repositoryId, String packageName, String module) {
        return countByFlag("has_event_listener", repositoryId, packageName, module);
    }

    // ==================== Shared Query Builders ====================

    private List<SpringSearchResult> findByFlag(String flagColumn, String repositoryId,
                                                  String packageName, String module,
                                                  int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
        List<Object> params = new ArrayList<>();
        sql.append("WHERE sc.").append(flagColumn).append(" = 1");
        applyBasicFilters(sql, params, repositoryId, packageName, module);
        sql.append(" ORDER BY sc.component_name ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);
        return mapResults(sql.toString(), params);
    }

    private long countByFlag(String flagColumn, String repositoryId,
                              String packageName, String module) {
        return countWithFilters(
            "WHERE sc." + flagColumn + " = 1",
            repositoryId, packageName, module);
    }

    private long countWithFilters(String whereClause, String repositoryId,
                                   String packageName, String module) {
        StringBuilder conditions = new StringBuilder(whereClause);
        List<Object> params = new ArrayList<>();
        appendFilterCondition(conditions, params, repositoryId, packageName, module);
        return countWithConditions(conditions.toString(), params);
    }

    private void applyBasicFilters(StringBuilder sql, List<Object> params,
                                    String repositoryId, String packageName, String module) {
        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND sc.repository_id = ?");
            params.add(repositoryId);
        }
        if (packageName != null && !packageName.isEmpty()) {
            sql.append(" AND sc.package_name LIKE ?");
            params.add("%" + packageName + "%");
        }
        if (module != null && !module.isEmpty()) {
            sql.append(" AND sc.source_file LIKE ?");
            params.add("%" + module + "%");
        }
    }

    private void appendFilterCondition(StringBuilder conditions, List<Object> params,
                                        String repositoryId, String packageName, String module) {
        if (repositoryId != null && !repositoryId.isEmpty()) {
            conditions.append(" AND sc.repository_id = ?");
            params.add(repositoryId);
        }
        if (packageName != null && !packageName.isEmpty()) {
            conditions.append(" AND sc.package_name LIKE ?");
            params.add("%" + packageName + "%");
        }
        if (module != null && !module.isEmpty()) {
            conditions.append(" AND sc.source_file LIKE ?");
            params.add("%" + module + "%");
        }
    }

    private long countWithConditions(String conditions, List<Object> params) {
        String sql = "SELECT COUNT(*) FROM spring_component sc " + conditions;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    // ==================== Result Mapping ====================

    private List<SpringSearchResult> mapResults(String sql, List<Object> params) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SpringSearchResult result = new SpringSearchResult();
            result.setComponentName(rs.getString("component_name"));
            result.setComponentType(rs.getString("component_type"));
            result.setClassName(rs.getString("class_name"));
            result.setPackageName(rs.getString("package_name"));
            result.setRepositoryId(rs.getString("repository_id"));
            result.setFilePath(rs.getString("source_file"));
            result.setBeanName(rs.getString("bean_name"));

            // Derive annotation from component type
            result.setAnnotation(deriveAnnotation(result.getComponentType()));

            // For REST_ENDPOINT entries, component_name is "HTTP_METHOD /path"
            if ("REST_ENDPOINT".equalsIgnoreCase(result.getComponentType())) {
                parseEndpointInfo(result);
            }

            // Derive module from source file path
            result.setModule(deriveModule(result.getFilePath()));

            return result;
        }, params.toArray());
    }

    /**
     * Derive the Spring annotation string from the component type.
     */
    private String deriveAnnotation(String componentType) {
        if (componentType == null) return "";
        return switch (componentType.toUpperCase()) {
            case "COMPONENT" -> "@Component";
            case "SERVICE" -> "@Service";
            case "REPOSITORY" -> "@Repository";
            case "CONTROLLER" -> "@Controller";
            case "REST_CONTROLLER" -> "@RestController";
            case "CONFIGURATION" -> "@Configuration";
            case "BEAN" -> "@Bean";
            case "REST_ENDPOINT" -> "@RequestMapping";
            default -> "@" + componentType;
        };
    }

    /**
     * Parse REST endpoint info from component_name (format: "GET /api/users").
     */
    private void parseEndpointInfo(SpringSearchResult result) {
        String componentName = result.getComponentName();
        if (componentName != null && componentName.contains(" ")) {
            int spaceIdx = componentName.indexOf(' ');
            String method = componentName.substring(0, spaceIdx);
            String path = componentName.substring(spaceIdx + 1);
            result.setHttpMethod(method);
            result.setRestPath(path);
            // Also try to get basepath from bean_name
            String beanName = result.getBeanName();
            if (beanName != null && !beanName.isEmpty() && !path.startsWith("/")) {
                result.setRestPath(beanName + "/" + path);
            }
        }
    }

    /**
     * Derive module name from source file path by extracting the repository name
     * portion of the path.
     */
    private String deriveModule(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "";
        // Try to extract a meaningful module name from the path
        // If path contains repository ID pattern, extract what comes after
        if (filePath.contains("/src/")) {
            int srcIdx = filePath.indexOf("/src/");
            String beforeSrc = filePath.substring(0, srcIdx);
            int lastSlash = beforeSrc.lastIndexOf('/');
            if (lastSlash >= 0) {
                return beforeSrc.substring(lastSlash + 1);
            }
            return beforeSrc;
        }
        // Fallback: use the first directory component
        String normalized = filePath.replace('\\', '/');
        int firstSlash = normalized.indexOf('/');
        if (firstSlash > 0) {
            return normalized.substring(0, firstSlash);
        }
        return "";
    }
}