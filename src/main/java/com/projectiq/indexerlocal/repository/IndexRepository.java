package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;

/**
 * Repository for persisting indexed metadata into SQLite.
 */
@Repository
public class IndexRepository {

    private final JdbcTemplate jdbcTemplate;

    public IndexRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== Row Mappers ====================

    private final RowMapper<FileIndex> fileRowMapper = (rs, rowNum) -> {
        FileIndex file = new FileIndex();
        file.setId(rs.getLong("id"));
        file.setFilePath(rs.getString("file_path"));
        file.setFileName(rs.getString("file_name"));
        file.setClassCount(rs.getInt("class_count"));
        file.setMethodCount(rs.getInt("method_count"));
        file.setFieldCount(rs.getInt("field_count"));
        file.setAnnotationCount(rs.getInt("annotation_count"));
        return file;
    };

    private final RowMapper<ClassInfo> classRowMapper = (rs, rowNum) -> {
        ClassInfo cls = new ClassInfo();
        cls.setId(rs.getLong("id"));
        cls.setFileIndexId(rs.getLong("file_index_id"));
        cls.setClassName(rs.getString("class_name"));
        cls.setClassType(rs.getString("class_type"));
        cls.setVisibility(rs.getString("visibility"));
        cls.setSuperClass(rs.getString("super_class"));
        String interfaces = rs.getString("interfaces");
        if (interfaces != null && !interfaces.isEmpty()) {
            cls.setInterfaces(List.of(interfaces.split(",")));
        }
        return cls;
    };

    private final RowMapper<MethodInfo> methodRowMapper = (rs, rowNum) -> {
        MethodInfo method = new MethodInfo();
        method.setId(rs.getLong("id"));
        method.setClassId(rs.getLong("class_id"));
        method.setMethodName(rs.getString("method_name"));
        method.setMethodSignature(rs.getString("method_signature"));
        method.setReturnType(rs.getString("return_type"));
        method.setVisibility(rs.getString("visibility"));
        method.setStatic(rs.getInt("is_static") != 0);
        method.setAbstract(rs.getInt("is_abstract") != 0);
        String parameters = rs.getString("parameters");
        if (parameters != null && !parameters.isEmpty()) {
            method.setParameters(List.of(parameters.split(",")));
        }
        String exceptions = rs.getString("exceptions");
        if (exceptions != null && !exceptions.isEmpty()) {
            method.setExceptions(List.of(exceptions.split(",")));
        }
        return method;
    };

    private final RowMapper<FieldInfo> fieldRowMapper = (rs, rowNum) -> {
        FieldInfo field = new FieldInfo();
        field.setId(rs.getLong("id"));
        field.setClassId(rs.getLong("class_id"));
        field.setFieldName(rs.getString("field_name"));
        field.setFieldType(rs.getString("field_type"));
        field.setVisibility(rs.getString("visibility"));
        field.setStatic(rs.getInt("is_static") != 0);
        field.setFinal(rs.getInt("is_final") != 0);
        return field;
    };

    private final RowMapper<SpringComponent> springComponentRowMapper = (rs, rowNum) -> {
        SpringComponent component = new SpringComponent();
        component.setId(rs.getLong("id"));
        component.setRepositoryId(rs.getString("repository_id"));
        component.setClassId(rs.getLong("class_id"));
        component.setFileIndexId(rs.getLong("file_index_id"));
        component.setComponentName(rs.getString("component_name"));
        component.setComponentType(rs.getString("component_type"));
        component.setClassName(rs.getString("class_name"));
        component.setPackageName(rs.getString("package_name"));
        component.setSourceFile(rs.getString("source_file"));
        
        // Boolean flags from INTEGER columns (0 or 1)
        component.setComponent(rs.getInt("is_component") != 0);
        component.setService(rs.getInt("is_service") != 0);
        component.setRepository(rs.getInt("is_repository") != 0);
        component.setController(rs.getInt("is_controller") != 0);
        component.setRestController(rs.getInt("is_rest_controller") != 0);
        component.setConfiguration(rs.getInt("is_configuration") != 0);
        component.setBean(rs.getInt("is_bean") != 0);
        component.setConfigurationProperties(rs.getInt("is_configuration_properties") != 0);
        component.setPropertySource(rs.getInt("is_property_source") != 0);
        component.setImport(rs.getInt("is_import") != 0);
        component.setAutowired(rs.getInt("has_autowired") != 0);
        component.setInject(rs.getInt("has_inject") != 0);
        component.setResource(rs.getInt("has_resource") != 0);
        component.setHasConstructorInjection(rs.getInt("has_constructor_injection") != 0);
        component.setHasSetterInjection(rs.getInt("has_setter_injection") != 0);
        component.setControllerAdvice(rs.getInt("is_controller_advice") != 0);
        component.setRestControllerAdvice(rs.getInt("is_rest_controller_advice") != 0);
        component.setCrossOrigin(rs.getInt("has_cross_origin") != 0);
        component.setResponseBody(rs.getInt("has_response_body") != 0);
        component.setTransactional(rs.getInt("has_transactional") != 0);
        component.setTransactionPropagation(rs.getString("transaction_propagation"));
        component.setTransactionIsolation(rs.getString("transaction_isolation"));
        component.setHasEnableScheduling(rs.getInt("has_enable_scheduling") != 0);
        component.setScheduled(rs.getInt("has_scheduled") != 0);
        component.setHasEnableAsync(rs.getInt("has_enable_async") != 0);
        component.setAsync(rs.getInt("has_async") != 0);
        component.setHasEnableCaching(rs.getInt("has_enable_caching") != 0);
        component.setCacheable(rs.getInt("has_cacheable") != 0);
        component.setCachePut(rs.getInt("has_cache_put") != 0);
        component.setCacheEvict(rs.getInt("has_cache_evict") != 0);
        component.setHasEnableWebSecurity(rs.getInt("has_enable_web_security") != 0);
        component.setHasEnableMethodSecurity(rs.getInt("has_enable_method_security") != 0);
        component.setPreAuthorize(rs.getInt("has_pre_authorize") != 0);
        component.setPostAuthorize(rs.getInt("has_post_authorize") != 0);
        component.setRolesAllowed(rs.getInt("has_roles_allowed") != 0);
        component.setSecured(rs.getInt("has_secured") != 0);
        component.setEventListener(rs.getInt("has_event_listener") != 0);
        component.setKafkaListener(rs.getInt("has_kafka_listener") != 0);
        component.setRabbitListener(rs.getInt("has_rabbit_listener") != 0);
        component.setJmsListener(rs.getInt("has_jms_listener") != 0);
        
        component.setBeanName(rs.getString("bean_name"));
        component.setDetectedAt(rs.getTimestamp("detected_at") != null ? 
                rs.getTimestamp("detected_at").toLocalDateTime() : null);
        return component;
    };

    // ==================== Schema Initialization ====================

    /**
     * Initialize the database schema.
     */
    public void initSchema() {
        String sqlCreateFileIndex = 
            "CREATE TABLE IF NOT EXISTS file_index (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "file_path TEXT NOT NULL, " +
            "file_name TEXT NOT NULL, " +
            "class_count INTEGER DEFAULT 0, " +
            "method_count INTEGER DEFAULT 0, " +
            "field_count INTEGER DEFAULT 0, " +
            "annotation_count INTEGER DEFAULT 0, " +
            "indexed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String sqlCreateClassInfo = 
            "CREATE TABLE IF NOT EXISTS class_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "file_index_id INTEGER, " +
            "class_name TEXT NOT NULL, " +
            "class_type TEXT, " +
            "visibility TEXT, " +
            "super_class TEXT, " +
            "interfaces TEXT, " +
            "FOREIGN KEY (file_index_id) REFERENCES file_index(id))";

        String sqlCreateFieldInfo = 
            "CREATE TABLE IF NOT EXISTS field_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "class_id INTEGER, " +
            "field_name TEXT NOT NULL, " +
            "field_type TEXT, " +
            "visibility TEXT, " +
            "is_static INTEGER DEFAULT 0, " +
            "is_final INTEGER DEFAULT 0, " +
            "FOREIGN KEY (class_id) REFERENCES class_info(id))";

        String sqlCreateMethodInfo = 
            "CREATE TABLE IF NOT EXISTS method_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "class_id INTEGER, " +
            "method_name TEXT NOT NULL, " +
            "method_signature TEXT, " +
            "return_type TEXT, " +
            "visibility TEXT, " +
            "is_static INTEGER DEFAULT 0, " +
            "is_abstract INTEGER DEFAULT 0, " +
            "parameters TEXT, " +
            "exceptions TEXT, " +
            "FOREIGN KEY (class_id) REFERENCES class_info(id))";

        String sqlCreateAnnotationInfo = 
            "CREATE TABLE IF NOT EXISTS annotation_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "annotation_name TEXT NOT NULL, " +
            "full_name TEXT, " +
            "target_type TEXT, " +
            "target_id INTEGER)";

        String sqlCreateImportInfo = 
            "CREATE TABLE IF NOT EXISTS import_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "file_index_id INTEGER, " +
            "import_name TEXT NOT NULL, " +
            "is_static INTEGER DEFAULT 0, " +
            "FOREIGN KEY (file_index_id) REFERENCES file_index(id))";

        String sqlCreateSpringComponent = 
            "CREATE TABLE IF NOT EXISTS spring_component (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "repository_id TEXT, " +
            "class_id INTEGER, " +
            "file_index_id INTEGER, " +
            "component_name TEXT, " +
            "component_type TEXT NOT NULL, " +
            "class_name TEXT, " +
            "package_name TEXT, " +
            "source_file TEXT, " +
            "is_component INTEGER DEFAULT 0, " +
            "is_service INTEGER DEFAULT 0, " +
            "is_repository INTEGER DEFAULT 0, " +
            "is_controller INTEGER DEFAULT 0, " +
            "is_rest_controller INTEGER DEFAULT 0, " +
            "is_configuration INTEGER DEFAULT 0, " +
            "is_bean INTEGER DEFAULT 0, " +
            "is_configuration_properties INTEGER DEFAULT 0, " +
            "is_property_source INTEGER DEFAULT 0, " +
            "is_import INTEGER DEFAULT 0, " +
            "has_autowired INTEGER DEFAULT 0, " +
            "has_inject INTEGER DEFAULT 0, " +
            "has_resource INTEGER DEFAULT 0, " +
            "has_constructor_injection INTEGER DEFAULT 0, " +
            "has_setter_injection INTEGER DEFAULT 0, " +
            "is_controller_advice INTEGER DEFAULT 0, " +
            "is_rest_controller_advice INTEGER DEFAULT 0, " +
            "has_cross_origin INTEGER DEFAULT 0, " +
            "has_response_body INTEGER DEFAULT 0, " +
            "has_transactional INTEGER DEFAULT 0, " +
            "transaction_propagation TEXT, " +
            "transaction_isolation TEXT, " +
            "has_enable_scheduling INTEGER DEFAULT 0, " +
            "has_scheduled INTEGER DEFAULT 0, " +
            "has_enable_async INTEGER DEFAULT 0, " +
            "has_async INTEGER DEFAULT 0, " +
            "has_enable_caching INTEGER DEFAULT 0, " +
            "has_cacheable INTEGER DEFAULT 0, " +
            "has_cache_put INTEGER DEFAULT 0, " +
            "has_cache_evict INTEGER DEFAULT 0, " +
            "has_enable_web_security INTEGER DEFAULT 0, " +
            "has_enable_method_security INTEGER DEFAULT 0, " +
            "has_pre_authorize INTEGER DEFAULT 0, " +
            "has_post_authorize INTEGER DEFAULT 0, " +
            "has_roles_allowed INTEGER DEFAULT 0, " +
            "has_secured INTEGER DEFAULT 0, " +
            "has_event_listener INTEGER DEFAULT 0, " +
            "has_kafka_listener INTEGER DEFAULT 0, " +
            "has_rabbit_listener INTEGER DEFAULT 0, " +
            "has_jms_listener INTEGER DEFAULT 0, " +
            "bean_name TEXT, " +
            "detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (class_id) REFERENCES class_info(id), " +
            "FOREIGN KEY (file_index_id) REFERENCES file_index(id))";

        // File tracking table for incremental indexing
        String sqlCreateFileTracking = 
            "CREATE TABLE IF NOT EXISTS file_tracking (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "repository_id TEXT NOT NULL, " +
            "file_path TEXT NOT NULL, " +
            "relative_path TEXT NOT NULL, " +
            "last_modified TIMESTAMP, " +
            "file_size INTEGER, " +
            "checksum TEXT, " +
            "index_status TEXT DEFAULT 'INDEXED', " +
            "tracked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "UNIQUE(repository_id, file_path))";

        // Indexing stats table for incremental indexing statistics
        String sqlCreateIndexingStats = 
            "CREATE TABLE IF NOT EXISTS indexing_stats (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "repository_id TEXT NOT NULL, " +
            "index_type TEXT NOT NULL, " +
            "status TEXT NOT NULL, " +
            "files_added INTEGER DEFAULT 0, " +
            "files_modified INTEGER DEFAULT 0, " +
            "files_deleted INTEGER DEFAULT 0, " +
            "files_unchanged INTEGER DEFAULT 0, " +
            "processing_time_ms INTEGER, " +
            "last_indexing_timestamp TIMESTAMP, " +
            "started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "completed_at TIMESTAMP)";

        jdbcTemplate.execute(sqlCreateFileIndex);
        jdbcTemplate.execute(sqlCreateClassInfo);
        jdbcTemplate.execute(sqlCreateFieldInfo);
        jdbcTemplate.execute(sqlCreateMethodInfo);
        jdbcTemplate.execute(sqlCreateAnnotationInfo);
        jdbcTemplate.execute(sqlCreateImportInfo);
        jdbcTemplate.execute(sqlCreateSpringComponent);
        jdbcTemplate.execute(sqlCreateFileTracking);
        jdbcTemplate.execute(sqlCreateIndexingStats);
    }

    // ==================== Save Methods ====================

    /**
     * Persist the complete IndexResult into SQLite.
     */
    public void saveIndexResult(IndexResult result) {
        initSchema();

        // Save file indexes
        for (FileIndex fileIndex : result.getFileIndexes()) {
            Long fileIndexId = saveFileIndex(fileIndex);

            // Save classes
            if (fileIndex.getClasses() != null) {
                for (ClassInfo classInfo : fileIndex.getClasses()) {
                    Long classId = saveClassInfo(classInfo, fileIndexId);
                    classInfo.setId(classId);

                    // Save fields
                    if (classInfo.getFields() != null) {
                        for (FieldInfo fieldInfo : classInfo.getFields()) {
                            saveFieldInfo(fieldInfo, classId);
                        }
                    }

                    // Save methods
                    if (classInfo.getMethods() != null) {
                        for (MethodInfo methodInfo : classInfo.getMethods()) {
                            saveMethodInfo(methodInfo, classId);
                        }
                    }

                    // Save annotations
                    if (classInfo.getAnnotations() != null) {
                        for (AnnotationInfo annotationInfo : classInfo.getAnnotations()) {
                            saveAnnotationInfo(annotationInfo, "CLASS", classId);
                        }
                    }
                }
            }

            // Save imports
            if (fileIndex.getImports() != null) {
                for (ImportInfo importInfo : fileIndex.getImports()) {
                    saveImportInfo(importInfo, fileIndexId);
                }
            }
        }
    }

    private Long saveFileIndex(FileIndex fileIndex) {
        String sql = "INSERT INTO file_index (file_path, file_name, class_count, method_count, field_count, annotation_count) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                fileIndex.getFilePath(),
                fileIndex.getFileName(),
                fileIndex.getClassCount(),
                fileIndex.getMethodCount(),
                fileIndex.getFieldCount(),
                fileIndex.getAnnotationCount());

        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        return id;
    }

    private Long saveClassInfo(ClassInfo classInfo, Long fileIndexId) {
        String sql = "INSERT INTO class_info (file_index_id, class_name, class_type, visibility, super_class, interfaces) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                fileIndexId,
                classInfo.getClassName(),
                classInfo.getClassType(),
                classInfo.getVisibility(),
                classInfo.getSuperClass(),
                classInfo.getInterfaces() != null ? String.join(",", classInfo.getInterfaces()) : null);

        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        return id;
    }

    private void saveFieldInfo(FieldInfo fieldInfo, Long classId) {
        String sql = "INSERT INTO field_info (class_id, field_name, field_type, visibility, is_static, is_final) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                classId,
                fieldInfo.getFieldName(),
                fieldInfo.getFieldType(),
                fieldInfo.getVisibility(),
                fieldInfo.isStatic() ? 1 : 0,
                fieldInfo.isFinal() ? 1 : 0);

        if (fieldInfo.getAnnotations() != null && !fieldInfo.getAnnotations().isEmpty()) {
            for (AnnotationInfo annotationInfo : fieldInfo.getAnnotations()) {
                saveAnnotationInfo(annotationInfo, "FIELD", classId);
            }
        }
    }

    private void saveMethodInfo(MethodInfo methodInfo, Long classId) {
        String sql = "INSERT INTO method_info (class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                classId,
                methodInfo.getMethodName(),
                methodInfo.getMethodSignature(),
                methodInfo.getReturnType(),
                methodInfo.getVisibility(),
                methodInfo.isStatic() ? 1 : 0,
                methodInfo.isAbstract() ? 1 : 0,
                methodInfo.getParameters() != null ? String.join(",", methodInfo.getParameters()) : null,
                methodInfo.getExceptions() != null ? String.join(",", methodInfo.getExceptions()) : null);

        if (methodInfo.getAnnotations() != null) {
            for (AnnotationInfo annotationInfo : methodInfo.getAnnotations()) {
                saveAnnotationInfo(annotationInfo, "METHOD", classId);
            }
        }
    }

    private void saveAnnotationInfo(AnnotationInfo annotationInfo, String targetType, Long targetId) {
        String sql = "INSERT INTO annotation_info (annotation_name, full_name, target_type, target_id) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                annotationInfo.getAnnotationName(),
                annotationInfo.getFullName(),
                targetType,
                targetId);
    }

    private void saveImportInfo(ImportInfo importInfo, Long fileIndexId) {
        String sql = "INSERT INTO import_info (file_index_id, import_name, is_static) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql,
                fileIndexId,
                importInfo.getImportName(),
                importInfo.isStatic() ? 1 : 0);
    }

    public Long saveSpringComponent(SpringComponent component) {
        String sql = "INSERT INTO spring_component (repository_id, class_id, file_index_id, component_name, component_type, class_name, package_name, source_file, is_component, is_service, is_repository, is_controller, is_rest_controller, is_configuration, is_bean, is_configuration_properties, is_property_source, is_import, has_autowired, has_inject, has_resource, has_constructor_injection, has_setter_injection, is_controller_advice, is_rest_controller_advice, has_cross_origin, has_response_body, has_transactional, transaction_propagation, transaction_isolation, has_enable_scheduling, has_scheduled, has_enable_async, has_async, has_enable_caching, has_cacheable, has_cache_put, has_cache_evict, has_enable_web_security, has_enable_method_security, has_pre_authorize, has_post_authorize, has_roles_allowed, has_secured, has_event_listener, has_kafka_listener, has_rabbit_listener, has_jms_listener, bean_name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, component.getRepositoryId());
            ps.setLong(2, component.getClassId() != null ? component.getClassId() : 0);
            ps.setLong(3, component.getFileIndexId() != null ? component.getFileIndexId() : 0);
            ps.setString(4, component.getComponentName());
            ps.setString(5, component.getComponentType());
            ps.setString(6, component.getClassName());
            ps.setString(7, component.getPackageName());
            ps.setString(8, component.getSourceFile());
            ps.setInt(9, component.isComponent() ? 1 : 0);
            ps.setInt(10, component.isService() ? 1 : 0);
            ps.setInt(11, component.isRepository() ? 1 : 0);
            ps.setInt(12, component.isController() ? 1 : 0);
            ps.setInt(13, component.isRestController() ? 1 : 0);
            ps.setInt(14, component.isConfiguration() ? 1 : 0);
            ps.setInt(15, component.isBean() ? 1 : 0);
            ps.setInt(16, component.isConfigurationProperties() ? 1 : 0);
            ps.setInt(17, component.isPropertySource() ? 1 : 0);
            ps.setInt(18, component.isImport() ? 1 : 0);
            ps.setInt(19, component.hasAutowired() ? 1 : 0);
            ps.setInt(20, component.hasInject() ? 1 : 0);
            ps.setInt(21, component.hasResource() ? 1 : 0);
            ps.setInt(22, component.isHasConstructorInjection() ? 1 : 0);
            ps.setInt(23, component.isHasSetterInjection() ? 1 : 0);
            ps.setInt(24, component.isControllerAdvice() ? 1 : 0);
            ps.setInt(25, component.isRestControllerAdvice() ? 1 : 0);
            ps.setInt(26, component.hasCrossOrigin() ? 1 : 0);
            ps.setInt(27, component.hasResponseBody() ? 1 : 0);
            ps.setInt(28, component.hasTransactional() ? 1 : 0);
            ps.setString(29, component.getTransactionPropagation());
            ps.setString(30, component.getTransactionIsolation());
            ps.setInt(31, component.isHasEnableScheduling() ? 1 : 0);
            ps.setInt(32, component.hasScheduled() ? 1 : 0);
            ps.setInt(33, component.isHasEnableAsync() ? 1 : 0);
            ps.setInt(34, component.hasAsync() ? 1 : 0);
            ps.setInt(35, component.isHasEnableCaching() ? 1 : 0);
            ps.setInt(36, component.hasCacheable() ? 1 : 0);
            ps.setInt(37, component.hasCachePut() ? 1 : 0);
            ps.setInt(38, component.hasCacheEvict() ? 1 : 0);
            ps.setInt(39, component.isHasEnableWebSecurity() ? 1 : 0);
            ps.setInt(40, component.isHasEnableMethodSecurity() ? 1 : 0);
            ps.setInt(41, component.hasPreAuthorize() ? 1 : 0);
            ps.setInt(42, component.hasPostAuthorize() ? 1 : 0);
            ps.setInt(43, component.hasRolesAllowed() ? 1 : 0);
            ps.setInt(44, component.hasSecured() ? 1 : 0);
            ps.setInt(45, component.hasEventListener() ? 1 : 0);
            ps.setInt(46, component.hasKafkaListener() ? 1 : 0);
            ps.setInt(47, component.hasRabbitListener() ? 1 : 0);
            ps.setInt(48, component.hasJmsListener() ? 1 : 0);
            ps.setString(49, component.getBeanName());
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
        component.setId(id);
        return id;
    }

    /**
     * Save REST API endpoints and statistics to the spring_component table.
     * Deletes old REST_API entries first, then inserts new ones.
     */
    public void saveRestApiEndpoints(String repositoryId, List<RestApiEndpoint> endpoints, RestApiStatistics statistics) {
        // Delete old REST API entries for this repository
        deleteSpringComponentsByRepository(repositoryId + ":REST_API");

        // Insert endpoints as REST_ENDPOINT components
        for (RestApiEndpoint endpoint : endpoints) {
            SpringComponent component = new SpringComponent();
            component.setRepositoryId(repositoryId + ":REST_API");
            component.setClassName(endpoint.getClassName());
            component.setComponentType("REST_ENDPOINT");
            
            // Encode HTTP method and path in componentName for storage
            String fullPath = (endpoint.getHttpMethod() != null ? endpoint.getHttpMethod() : "") + 
                              " " + (endpoint.getEndpointPath() != null ? endpoint.getEndpointPath() : "");
            component.setComponentName(fullPath);
            
            // Store basepath in beanName
            component.setBeanName(endpoint.getBasepath());
            
            // Mark as REST controller
            component.setRestController(true);

            // Store security info from annotations
            if (endpoint.isPreAuthorize() || endpoint.isPostAuthorize() || 
                endpoint.isRolesAllowed() || endpoint.isSecured()) {
                component.setHasEnableMethodSecurity(true);
                component.setPreAuthorize(endpoint.isPreAuthorize());
                component.setPostAuthorize(endpoint.isPostAuthorize());
                component.setRolesAllowed(endpoint.isRolesAllowed());
                component.setSecured(endpoint.isSecured());
            }

            // Store produces/consumes media types in sourceFile
            StringBuilder mediaTypes = new StringBuilder();
            if (endpoint.getProducesMediaType() != null) {
                mediaTypes.append("produces=").append(endpoint.getProducesMediaType());
            }
            if (endpoint.getConsumesMediaType() != null) {
                if (!mediaTypes.isEmpty()) mediaTypes.append(";");
                mediaTypes.append("consumes=").append(endpoint.getConsumesMediaType());
            }
            component.setSourceFile(mediaTypes.toString());

            saveSpringComponent(component);
        }

        // Persist statistics as a special entry
        SpringComponent statsEntry = new SpringComponent();
        statsEntry.setRepositoryId(repositoryId + ":REST_API:STATISTICS");
        statsEntry.setClassName("RestApiStatistics");
        statsEntry.setComponentType("STATISTICS");
        statsEntry.setComponentName("REST_API_STATISTICS");

        // Store statistics JSON in beanName
        StringBuilder statsJson = new StringBuilder("{");
        statsJson.append("\"totalRestControllers\":").append(statistics.getTotalRestControllers()).append(",")
                 .append("\"totalEndpoints\":").append(statistics.getTotalEndpoints()).append(",")
                 .append("\"secureEndpoints\":").append(statistics.getSecureEndpoints()).append(",")
                 .append("\"publicEndpoints\":").append(statistics.getPublicEndpoints()).append(",")
                 .append("\"endpointsByGetMapping\":").append(statistics.getEndpointsByGetMapping()).append(",")
                 .append("\"endpointsByPostMapping\":").append(statistics.getEndpointsByPostMapping()).append(",")
                 .append("\"endpointsByPutMapping\":").append(statistics.getEndpointsByPutMapping()).append(",")
                 .append("\"endpointsByDeleteMapping\":").append(statistics.getEndpointsByDeleteMapping()).append(",")
                 .append("\"endpointsByPatchMapping\":").append(statistics.getEndpointsByPatchMapping()).append(",")
                 .append("\"endpointsByRequestMapping\":").append(statistics.getEndpointsByRequestMapping());
        statsJson.append("}");
        statsEntry.setBeanName(statsJson.toString());

        saveSpringComponent(statsEntry);
    }

    /**
     * Save a list of Spring components for a repository (deletes old ones first).
     */
    public void saveSpringComponents(String repositoryId, List<SpringComponent> components) {
        // Delete old components for this repository
        deleteSpringComponentsByRepository(repositoryId);

        // Insert new components
        for (SpringComponent component : components) {
            component.setRepositoryId(repositoryId);
            saveSpringComponent(component);
        }
    }

    public void deleteSpringComponentsByRepository(String repositoryId) {
        jdbcTemplate.update("DELETE FROM spring_component WHERE repository_id = ?", repositoryId);
    }

    /**
     * Delete all index data for a specific repository.
     * Clears file_index, class_info, method_info, field_info, annotation_info, import_info,
     * spring_component, and file_tracking tables for the given repository.
     */
    public void deleteAllByRepositoryId(String repositoryId) {
        // Delete file tracking records first (foreign key dependency)
        jdbcTemplate.update("DELETE FROM file_tracking WHERE repository_id = ?", repositoryId);

        // Get file indices for this repository and delete associated data
        List<FileIndex> files = findAllFilesByRepositoryId(repositoryId);
        for (FileIndex file : files) {
            // Delete imports
            jdbcTemplate.update("DELETE FROM import_info WHERE file_index_id = ?", file.getId());
            // Delete annotations (class-level)
            jdbcTemplate.update("DELETE FROM annotation_info WHERE target_type = 'CLASS' AND target_id = ?", file.getId());
        }

        // Delete all file indexes for this repository
        jdbcTemplate.update("DELETE FROM file_index WHERE file_path LIKE ?", repositoryId + "%");

        // Delete spring components for this repository
        jdbcTemplate.update("DELETE FROM spring_component WHERE repository_id LIKE ?", repositoryId + "%");

        // Delete indexing stats for this repository
        jdbcTemplate.update("DELETE FROM indexing_stats WHERE repository_id = ?", repositoryId);
    }

    // ==================== Repository-Scoped Spring Component Queries ====================

    /**
     * Find all Spring components for a repository.
     */
    public List<SpringComponent> findSpringComponentsByRepository(String repositoryId) {
        String sql = "SELECT id, repository_id, class_id, file_index_id, component_name, component_type, class_name, package_name, source_file, " +
                "is_component, is_service, is_repository, is_controller, is_rest_controller, is_configuration, is_bean, " +
                "is_configuration_properties, is_property_source, is_import, has_autowired, has_inject, has_resource, " +
                "has_constructor_injection, has_setter_injection, is_controller_advice, is_rest_controller_advice, " +
                "has_cross_origin, has_response_body, has_transactional, transaction_propagation, transaction_isolation, " +
                "has_enable_scheduling, has_scheduled, has_enable_async, has_async, has_enable_caching, has_cacheable, " +
                "has_cache_put, has_cache_evict, has_enable_web_security, has_enable_method_security, has_pre_authorize, " +
                "has_post_authorize, has_roles_allowed, has_secured, has_event_listener, has_kafka_listener, has_rabbit_listener, " +
                "has_jms_listener, bean_name, detected_at FROM spring_component WHERE repository_id = ? ORDER BY id";
        return jdbcTemplate.query(sql, springComponentRowMapper, repositoryId);
    }

    /**
     * Find Spring components by repository and type.
     */
    public List<SpringComponent> findSpringComponentsByRepositoryAndType(String repositoryId, String componentType) {
        String sql = "SELECT id, repository_id, class_id, file_index_id, component_name, component_type, class_name, package_name, source_file, " +
                "is_component, is_service, is_repository, is_controller, is_rest_controller, is_configuration, is_bean, " +
                "is_configuration_properties, is_property_source, is_import, has_autowired, has_inject, has_resource, " +
                "has_constructor_injection, has_setter_injection, is_controller_advice, is_rest_controller_advice, " +
                "has_cross_origin, has_response_body, has_transactional, transaction_propagation, transaction_isolation, " +
                "has_enable_scheduling, has_scheduled, has_enable_async, has_async, has_enable_caching, has_cacheable, " +
                "has_cache_put, has_cache_evict, has_enable_web_security, has_enable_method_security, has_pre_authorize, " +
                "has_post_authorize, has_roles_allowed, has_secured, has_event_listener, has_kafka_listener, has_rabbit_listener, " +
                "has_jms_listener, bean_name, detected_at FROM spring_component WHERE repository_id = ? AND UPPER(component_type) = UPPER(?) ORDER BY id";
        return jdbcTemplate.query(sql, springComponentRowMapper, repositoryId, componentType);
    }

    /**
     * Find Spring components by annotation in a repository.
     */
    public List<SpringComponent> findSpringComponentsByAnnotation(String repositoryId, String annotationFlag) {
        String sql = "SELECT id, repository_id, class_id, file_index_id, component_name, component_type, class_name, package_name, source_file, " +
                "is_component, is_service, is_repository, is_controller, is_rest_controller, is_configuration, is_bean, " +
                "is_configuration_properties, is_property_source, is_import, has_autowired, has_inject, has_resource, " +
                "has_constructor_injection, has_setter_injection, is_controller_advice, is_rest_controller_advice, " +
                "has_cross_origin, has_response_body, has_transactional, transaction_propagation, transaction_isolation, " +
                "has_enable_scheduling, has_scheduled, has_enable_async, has_async, has_enable_caching, has_cacheable, " +
                "has_cache_put, has_cache_evict, has_enable_web_security, has_enable_method_security, has_pre_authorize, " +
                "has_post_authorize, has_roles_allowed, has_secured, has_event_listener, has_kafka_listener, has_rabbit_listener, " +
                "has_jms_listener, bean_name, detected_at FROM spring_component WHERE repository_id = ? AND " + annotationFlag + " = 1 ORDER BY id";
        return jdbcTemplate.query(sql, springComponentRowMapper, repositoryId);
    }

    /**
     * Get component type counts for a repository.
     */
    public java.util.Map<String, Integer> getSpringComponentTypeCounts(String repositoryId) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        
        String[] typeColumns = {"is_service", "is_repository", "is_controller", "is_rest_controller", 
                                "is_component", "is_configuration"};
        
        for (String col : typeColumns) {
            String sql = "SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND " + col + " = 1";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, repositoryId);
            if (count != null && count > 0) {
                switch (col) {
                    case "is_service": counts.put("SERVICE", count.intValue()); break;
                    case "is_repository": counts.put("REPOSITORY", count.intValue()); break;
                    case "is_controller": counts.put("CONTROLLER", count.intValue()); break;
                    case "is_rest_controller": counts.put("REST_CONTROLLER", count.intValue()); break;
                    case "is_component": counts.put("COMPONENT", count.intValue()); break;
                    case "is_configuration": counts.put("CONFIGURATION", count.intValue()); break;
                }
            }
        }
        
        return counts;
    }

    /**
     * Get annotation flag counts for a repository.
     */
    public java.util.Map<String, Integer> getSpringAnnotationCounts(String repositoryId) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        
        String[] annotationColumns = {"has_autowired", "has_inject", "has_resource", "has_transactional", 
                "has_scheduled", "has_async", "has_cacheable", "has_pre_authorize", "has_event_listener"};
        
        for (String col : annotationColumns) {
            String sql = "SELECT COUNT(*) FROM spring_component WHERE repository_id = ? AND " + col + " = 1";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, repositoryId);
            if (count != null && count > 0) {
                switch (col) {
                    case "has_autowired": counts.put("AUTOWIRED", count.intValue()); break;
                    case "has_inject": counts.put("INJECT", count.intValue()); break;
                    case "has_resource": counts.put("RESOURCE", count.intValue()); break;
                    case "has_transactional": counts.put("TRANSACTIONAL", count.intValue()); break;
                    case "has_scheduled": counts.put("SCHEDULED", count.intValue()); break;
                    case "has_async": counts.put("ASYNC", count.intValue()); break;
                    case "has_cacheable": counts.put("CACHEABLE", count.intValue()); break;
                    case "has_pre_authorize": counts.put("PRE_AUTHORIZE", count.intValue()); break;
                    case "has_event_listener": counts.put("EVENT_LISTENER", count.intValue()); break;
                }
            }
        }
        
        return counts;
    }

    // ==================== Query Methods for REST APIs ====================

    /**
     * List all indexed source files.
     */
    public List<FileIndex> findAllFiles() {
        String sql = "SELECT id, file_path, file_name, class_count, method_count, field_count, annotation_count FROM file_index ORDER BY id";
        return jdbcTemplate.query(sql, fileRowMapper);
    }

    /**
     * Find a source file by ID.
     */
    public FileIndex findFileById(Long id) {
        String sql = "SELECT id, file_path, file_name, class_count, method_count, field_count, annotation_count FROM file_index WHERE id = ?";
        List<FileIndex> results = jdbcTemplate.query(sql, fileRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a source file by file path.
     */
    public FileIndex findFileByPath(String filePath) {
        String sql = "SELECT id, file_path, file_name, class_count, method_count, field_count, annotation_count FROM file_index WHERE file_path = ?";
        List<FileIndex> results = jdbcTemplate.query(sql, fileRowMapper, filePath);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * List all Java classes.
     */
    public List<ClassInfo> findAllClasses() {
        String sql = "SELECT id, file_index_id, class_name, class_type, visibility, super_class, interfaces FROM class_info ORDER BY id";
        return jdbcTemplate.query(sql, classRowMapper);
    }

    /**
     * Find a class by ID.
     */
    public ClassInfo findClassById(Long id) {
        String sql = "SELECT id, file_index_id, class_name, class_type, visibility, super_class, interfaces FROM class_info WHERE id = ?";
        List<ClassInfo> results = jdbcTemplate.query(sql, classRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a class by name.
     */
    public ClassInfo findClassByName(String className) {
        String sql = "SELECT id, file_index_id, class_name, class_type, visibility, super_class, interfaces FROM class_info WHERE class_name = ? ORDER BY id DESC LIMIT 1";
        List<ClassInfo> results = jdbcTemplate.query(sql, classRowMapper, className);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * List all methods.
     */
    public List<MethodInfo> findAllMethods() {
        String sql = "SELECT id, class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions FROM method_info ORDER BY id";
        return jdbcTemplate.query(sql, methodRowMapper);
    }

    /**
     * Find a method by ID.
     */
    public MethodInfo findMethodById(Long id) {
        String sql = "SELECT id, class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions FROM method_info WHERE id = ?";
        List<MethodInfo> results = jdbcTemplate.query(sql, methodRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a method by name.
     */
    public MethodInfo findMethodByName(String methodName) {
        String sql = "SELECT id, class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions FROM method_info WHERE method_name = ? ORDER BY id DESC LIMIT 1";
        List<MethodInfo> results = jdbcTemplate.query(sql, methodRowMapper, methodName);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * List all fields.
     */
    public List<FieldInfo> findAllFields() {
        String sql = "SELECT id, class_id, field_name, field_type, visibility, is_static, is_final FROM field_info ORDER BY id";
        return jdbcTemplate.query(sql, fieldRowMapper);
    }

    /**
     * Find a field by ID.
     */
    public FieldInfo findFieldById(Long id) {
        String sql = "SELECT id, class_id, field_name, field_type, visibility, is_static, is_final FROM field_info WHERE id = ?";
        List<FieldInfo> results = jdbcTemplate.query(sql, fieldRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a field by name.
     */
    public FieldInfo findFieldByName(String fieldName) {
        String sql = "SELECT id, class_id, field_name, field_type, visibility, is_static, is_final FROM field_info WHERE field_name = ? ORDER BY id DESC LIMIT 1";
        List<FieldInfo> results = jdbcTemplate.query(sql, fieldRowMapper, fieldName);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * List all Spring components.
     */
    public List<SpringComponent> findAllSpringComponents() {
        String sql = "SELECT id, class_id, component_type, class_name, file_index_id FROM spring_component ORDER BY id";
        return jdbcTemplate.query(sql, springComponentRowMapper);
    }

    /**
     * Filter Spring components by type.
     */
    public List<SpringComponent> findSpringComponentsByType(String componentType) {
        String sql = "SELECT id, class_id, component_type, class_name, file_index_id FROM spring_component WHERE component_type = ? ORDER BY id";
        return jdbcTemplate.query(sql, springComponentRowMapper, componentType);
    }

    // ==================== Lookup Methods ====================

    /**
     * Search classes by name (partial match, case-insensitive).
     */
    public List<ClassInfo> searchClassesByName(String namePattern) {
        String sql = "SELECT id, file_index_id, class_name, class_type, visibility, super_class, interfaces FROM class_info WHERE class_name LIKE ? ORDER BY id";
        return jdbcTemplate.query(sql, classRowMapper, "%" + namePattern + "%");
    }

    /**
     * Search methods by name (partial match, case-insensitive).
     */
    public List<MethodInfo> searchMethodsByName(String namePattern) {
        String sql = "SELECT id, class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions FROM method_info WHERE method_name LIKE ? ORDER BY id";
        return jdbcTemplate.query(sql, methodRowMapper, "%" + namePattern + "%");
    }

    /**
     * Search fields by name (partial match, case-insensitive).
     */
    public List<FieldInfo> searchFieldsByName(String namePattern) {
        String sql = "SELECT id, class_id, field_name, field_type, visibility, is_static, is_final FROM field_info WHERE field_name LIKE ? ORDER BY id";
        return jdbcTemplate.query(sql, fieldRowMapper, "%" + namePattern + "%");
    }

    /**
     * Find classes by package (partial match on file path).
     */
    public List<ClassInfo> searchClassesByPackage(String packagePattern) {
        String sql = "SELECT ci.id, ci.file_index_id, ci.class_name, ci.class_type, ci.visibility, ci.super_class, ci.interfaces " +
                "FROM class_info ci INNER JOIN file_index fi ON ci.file_index_id = fi.id " +
                "WHERE fi.file_path LIKE ? ORDER BY ci.id";
        return jdbcTemplate.query(sql, classRowMapper, "%" + packagePattern + "%");
    }

    /**
     * Find classes by super class name.
     */
    public List<ClassInfo> searchClassesBySuperClass(String superClass) {
        String sql = "SELECT id, file_index_id, class_name, class_type, visibility, super_class, interfaces FROM class_info WHERE super_class LIKE ? ORDER BY id";
        return jdbcTemplate.query(sql, classRowMapper, "%" + superClass + "%");
    }

    /**
     * Search annotations by name (partial match, case-insensitive).
     */
    public List<AnnotationInfo> searchAnnotationsByName(String namePattern) {
        String sql = "SELECT id, annotation_name, full_name, target_type, target_id FROM annotation_info WHERE annotation_name LIKE ? ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AnnotationInfo annotation = new AnnotationInfo();
            annotation.setId(rs.getLong("id"));
            annotation.setAnnotationName(rs.getString("annotation_name"));
            annotation.setFullName(rs.getString("full_name"));
            annotation.setTargetType(rs.getString("target_type"));
            annotation.setTargetId(rs.getLong("target_id"));
            return annotation;
        }, "%" + namePattern + "%");
    }

    /**
     * Find annotations by target type and target ID.
     */
    public List<AnnotationInfo> findAnnotationsByTarget(String targetType, Long targetId) {
        String sql = "SELECT id, annotation_name, full_name, target_type, target_id FROM annotation_info WHERE target_type = ? AND target_id = ? ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AnnotationInfo annotation = new AnnotationInfo();
            annotation.setId(rs.getLong("id"));
            annotation.setAnnotationName(rs.getString("annotation_name"));
            annotation.setFullName(rs.getString("full_name"));
            annotation.setTargetType(rs.getString("target_type"));
            annotation.setTargetId(rs.getLong("target_id"));
            return annotation;
        }, targetType, targetId);
    }

    /**
     * Find methods by class ID.
     */
    public List<MethodInfo> findMethodsByClassId(Long classId) {
        String sql = "SELECT id, class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions FROM method_info WHERE class_id = ? ORDER BY method_name";
        return jdbcTemplate.query(sql, methodRowMapper, classId);
    }

    /**
     * Find fields by class ID.
     */
    public List<FieldInfo> findFieldsByClassId(Long classId) {
        String sql = "SELECT id, class_id, field_name, field_type, visibility, is_static, is_final FROM field_info WHERE class_id = ? ORDER BY field_name";
        return jdbcTemplate.query(sql, fieldRowMapper, classId);
    }

    /**
     * Search files by path (partial match).
     */
    public List<FileIndex> searchFilesByPath(String pathPattern) {
        String sql = "SELECT id, file_path, file_name, class_count, method_count, field_count, annotation_count FROM file_index WHERE file_path LIKE ? ORDER BY id";
        return jdbcTemplate.query(sql, fileRowMapper, "%" + pathPattern + "%");
    }

    /**
     * Find all distinct component types.
     */
    public List<String> findAllComponentTypes() {
        String sql = "SELECT DISTINCT component_type FROM spring_component ORDER BY component_type";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            java.util.List<String> types = new ArrayList<>();
            while (rs.next()) {
                types.add(rs.getString(1));
            }
            return types;
        });
    }

    /**
     * Search Spring components by type (case-insensitive).
     */
    public List<SpringComponent> searchSpringComponentsByTypeIgnoreCase(String componentType) {
        String sql = "SELECT id, class_id, component_type, class_name, file_index_id FROM spring_component WHERE UPPER(component_type) = UPPER(?) ORDER BY id";
        return jdbcTemplate.query(sql, springComponentRowMapper, componentType);
    }

    /**
     * Search Spring components by class name (partial match).
     */
    public List<SpringComponent> searchSpringComponentsByName(String namePattern) {
        String sql = "SELECT id, class_id, component_type, class_name, file_index_id FROM spring_component WHERE class_name LIKE ? ORDER BY id";
        return jdbcTemplate.query(sql, springComponentRowMapper, "%" + namePattern + "%");
    }

    /**
     * Get full class detail with methods, fields, annotations.
     */
    public ClassInfo findClassDetailById(Long classId) {
        ClassInfo cls = findClassById(classId);
        if (cls == null) {
            return null;
        }
        // Get methods
        List<MethodInfo> methods = findMethodsByClassId(classId);
        cls.setMethods(methods);
        // Get fields
        List<FieldInfo> fields = findFieldsByClassId(classId);
        cls.setFields(fields);
        // Get annotations
        List<AnnotationInfo> annotations = findAnnotationsByTarget("CLASS", classId);
        cls.setAnnotations(annotations);
        return cls;
    }

    /**
     * Get full file detail with all classes.
     */
    public FileIndex findFileDetailById(Long fileId) {
        FileIndex file = findFileById(fileId);
        if (file == null) {
            return null;
        }
        // Re-extract from in-memory would be expensive, so we provide class count summary
        return file;
    }

    // ==================== Java Code Indexing Engine Methods ====================

    /**
     * Find all indexed files for a specific repository by repository_id column.
     */
    public List<FileIndex> findAllFilesByRepositoryId(String repositoryId) {
        String sql = "SELECT id, file_path, file_name, class_count, method_count, field_count, annotation_count FROM file_index WHERE file_path LIKE ? ORDER BY id";
        return jdbcTemplate.query(sql, fileRowMapper, "%" + repositoryId + "%");
    }

    // ==================== Repository Summary Methods ====================

    /**
     * Get repository summary statistics from indexed metadata.
     */
    public RepositorySummary getRepositorySummary() {
        // Total Java files
        Long totalJavaFiles = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM file_index", Long.class);

        // Total packages (distinct parent directories)
        Long totalPackages = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT substr(file_path, 1, length(file_path) - length(replace(file_path, '/', '')))) FROM file_index", 
            Long.class);

        // Total classes
        Long totalClasses = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_info WHERE class_type = 'CLASS'", Long.class);

        // Total interfaces
        Long totalInterfaces = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_info WHERE class_type = 'INTERFACE'", Long.class);

        // Total enums
        Long totalEnums = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_info WHERE class_type = 'ENUM'", Long.class);

        // Total records
        Long totalRecords = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_info WHERE class_type = 'RECORD'", Long.class);

        // Total methods
        Long totalMethods = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM method_info", Long.class);

        // Total fields
        Long totalFields = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM field_info", Long.class);

        // Total Spring components
        Long totalSpringComponents = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM spring_component", Long.class);

        return new RepositorySummary(
            totalJavaFiles != null ? totalJavaFiles : 0,
            totalPackages != null ? totalPackages : 0,
            totalClasses != null ? totalClasses : 0,
            totalInterfaces != null ? totalInterfaces : 0,
            totalEnums != null ? totalEnums : 0,
            totalRecords != null ? totalRecords : 0,
            totalMethods != null ? totalMethods : 0,
            totalFields != null ? totalFields : 0,
            totalSpringComponents != null ? totalSpringComponents : 0
        );
    }
}