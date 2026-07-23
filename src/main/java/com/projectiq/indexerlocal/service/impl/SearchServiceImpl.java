package com.projectiq.indexerlocal.service.impl;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.DependencyRepository;
import com.projectiq.indexerlocal.repository.IndexRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import com.projectiq.indexerlocal.service.SearchService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of SearchService that queries only persisted indexed metadata.
 * No filesystem scanning or code parsing is performed during search operations.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private final JdbcTemplate jdbcTemplate;
    private final IndexRepository indexRepository;
    private final RepositoryRepository repositoryRepository;
    private final DependencyRepository dependencyRepository;

    public SearchServiceImpl(JdbcTemplate jdbcTemplate, IndexRepository indexRepository,
                             RepositoryRepository repositoryRepository, DependencyRepository dependencyRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.indexRepository = indexRepository;
        this.repositoryRepository = repositoryRepository;
        this.dependencyRepository = dependencyRepository;
    }

    // ==================== General Search ====================

    @Override
    public List<Map<String, Object>> generalSearch(String repositoryId, String query, String packageName, String annotation) {
        List<Map<String, Object>> results = new ArrayList<>();
        String searchPattern = (query != null && !query.isEmpty()) ? "%" + query.toLowerCase() + "%" : "%";

        // Search classes using database query
        List<ClassInfo> classes = indexRepository.searchClassesByName(query != null ? query : "");
        for (ClassInfo cls : classes) {
            if (packageName != null && !packageName.isEmpty()) {
                String filePath = findFilePathForClass(cls.getId());
                if (filePath == null || !filePath.toLowerCase().contains(packageName.toLowerCase())) {
                    continue;
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("type", "CLASS");
            result.put("data", toClassMap(cls));
            results.add(result);
        }

        // Search methods using database query
        List<MethodInfo> methods = indexRepository.searchMethodsByName(query != null ? query : "");
        for (MethodInfo method : methods) {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "METHOD");
            result.put("data", toMethodMap(method));
            results.add(result);
        }

        // Search fields using database query
        List<FieldInfo> fields = indexRepository.searchFieldsByName(query != null ? query : "");
        for (FieldInfo field : fields) {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "FIELD");
            result.put("data", toFieldMap(field));
            results.add(result);
        }

        // Search annotations using database query
        List<AnnotationInfo> annotations = indexRepository.searchAnnotationsByName(query != null ? query : "");
        for (AnnotationInfo annotationInfo : annotations) {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "ANNOTATION");
            result.put("data", toAnnotationMap(annotationInfo));
            results.add(result);
        }

        // Search Spring components
        List<SpringComponent> components = indexRepository.findAllSpringComponents();
        for (SpringComponent component : components) {
            boolean match = true;
            if (query != null && !query.isEmpty()) {
                String nameToSearch = (component.getClassName() != null ? component.getClassName() : "") + " " +
                    (component.getComponentName() != null ? component.getComponentName() : "");
                match = nameToSearch.toLowerCase().contains(query.toLowerCase());
            }
            if (annotation != null && !annotation.isEmpty()) {
                match = match && hasAnnotation(component, annotation);
            }
            if (match) {
                Map<String, Object> result = new HashMap<>();
                result.put("type", "COMPONENT");
                result.put("data", toComponentMap(component));
                results.add(result);
            }
        }

        return results;
    }

    // ==================== Class Search ====================

    @Override
    public ClassSearchResult searchClasses(String repositoryId, String query, String packageName, int page, int pageSize, String sortBy, String sortDir) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Use database search instead of loading all classes
        List<ClassInfo> classes = indexRepository.searchClassesByName(query != null ? query : "");
        for (ClassInfo cls : classes) {
            if (packageName != null && !packageName.isEmpty()) {
                String filePath = findFilePathForClass(cls.getId());
                if (filePath == null || !filePath.toLowerCase().contains(packageName.toLowerCase())) {
                    continue;
                }
            }
            results.add(toClassMap(cls));
        }

        long totalElements = results.size();

        // Apply pagination
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) totalElements);
        if (fromIndex < totalElements) {
            results = results.subList(fromIndex, toIndex);
        } else {
            results = new ArrayList<>();
        }

        return new ClassSearchResult(results, totalElements);
    }

    @Override
    public ClassSearchResult searchClassesByType(String repositoryId, String query, String entityType, String visibility, String packageName, int page, int pageSize, String sortBy, String sortDir) {
        List<Map<String, Object>> results = new ArrayList<>();

        String upperEntityType = entityType != null ? entityType.toUpperCase() : null;

        // Use database search instead of loading all classes
        List<ClassInfo> classes = indexRepository.searchClassesByName(query != null ? query : "");
        for (ClassInfo cls : classes) {
            boolean match = true;

            if (upperEntityType != null && !upperEntityType.isEmpty()) {
                match = upperEntityType.equals(cls.getClassType());
            }
            if (match && visibility != null && !visibility.isEmpty()) {
                match = visibility.toUpperCase().equals(cls.getVisibility());
            }
            if (match && packageName != null && !packageName.isEmpty()) {
                String filePath = findFilePathForClass(cls.getId());
                match = filePath != null && filePath.toLowerCase().contains(packageName.toLowerCase());
            }

            if (match) {
                results.add(toClassMap(cls));
            }
        }

        long totalElements = results.size();

        // Apply pagination
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) totalElements);
        if (fromIndex < totalElements) {
            results = results.subList(fromIndex, toIndex);
        } else {
            results = new ArrayList<>();
        }

        return new ClassSearchResult(results, totalElements);
    }

    // ==================== Method Search ====================

    @Override
    public MethodSearchResult searchMethods(String repositoryId, String query, String packageName, int page, int pageSize, String sortBy, String sortDir) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Use database search instead of loading all methods
        List<MethodInfo> methods = indexRepository.searchMethodsByName(query != null ? query : "");
        for (MethodInfo method : methods) {
            results.add(toMethodMap(method));
        }

        long totalElements = results.size();

        // Apply pagination
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) totalElements);
        if (fromIndex < totalElements) {
            results = results.subList(fromIndex, toIndex);
        } else {
            results = new ArrayList<>();
        }

        return new MethodSearchResult(results, totalElements);
    }

    // ==================== Field Search ====================

    @Override
    public FieldSearchResult searchFields(String repositoryId, String query, String packageName, int page, int pageSize, String sortBy, String sortDir) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Use database search instead of loading all fields
        List<FieldInfo> fields = indexRepository.searchFieldsByName(query != null ? query : "");
        for (FieldInfo field : fields) {
            results.add(toFieldMap(field));
        }

        long totalElements = results.size();

        // Apply pagination
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) totalElements);
        if (fromIndex < totalElements) {
            results = results.subList(fromIndex, toIndex);
        } else {
            results = new ArrayList<>();
        }

        return new FieldSearchResult(results, totalElements);
    }

    // ==================== REST API Search ====================

    @Override
    public SearchService.EndpointSearchResult searchRestEndpoints(String repositoryId, String query, String httpMethod, int page, int pageSize, String sortBy, String sortDir) {
        List<Map<String, Object>> results = new ArrayList<>();
        long totalElements = 0;

        // Query using SpringComponent directly from IndexRepository's in-memory data
        List<SpringComponent> allComponents = indexRepository.findAllSpringComponents();
        List<SpringComponent> components = allComponents.stream()
            .filter(c -> "REST_ENDPOINT".equals(c.getComponentType()))
            .toList();

        for (SpringComponent component : components) {
            boolean match = true;

            if (query != null && !query.isEmpty()) {
                String searchText = (component.getClassName() != null ? component.getClassName() : "") + " " +
                    (component.getComponentName() != null ? component.getComponentName() : "");
                match = searchText.toLowerCase().contains(query.toLowerCase());
            }

            if (httpMethod != null && !httpMethod.isEmpty()) {
                // Extract HTTP method from componentName which is stored as "HTTP_METHOD /path"
                String componentName = component.getComponentName();
                if (componentName != null && componentName.contains(" ")) {
                    String[] parts = componentName.split(" ", 2);
                    match = parts[0].equals(httpMethod.toUpperCase());
                } else {
                    match = false;
                }
            }

            if (match) {
                Map<String, Object> result = new HashMap<>();
                result.put("type", "ENDPOINT");
                result.put("data", toEndpointMap(component));
                results.add(result);
            }
        }

        totalElements = results.size();

        // Apply pagination
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) totalElements);
        if (fromIndex < totalElements) {
            results = results.subList(fromIndex, toIndex);
        } else {
            results = new ArrayList<>();
        }

        return new SearchService.EndpointSearchResult(results, totalElements);
    }

    // ==================== Spring Component Search ====================

    @Override
    public SearchService.ComponentSearchResult searchComponents(String repositoryId, String query, String packageName, String annotation, int page, int pageSize, String sortBy, String sortDir) {
        List<Map<String, Object>> results = new ArrayList<>();
        long totalElements = 0;

        // Query using SpringComponent directly from IndexRepository's in-memory data
        List<SpringComponent> allComponents = indexRepository.findAllSpringComponents();
        List<SpringComponent> components = allComponents.stream()
            .filter(c -> "SERVICE".equals(c.getComponentType()) || "REPOSITORY".equals(c.getComponentType()) ||
                "CONTROLLER".equals(c.getComponentType()) || "REST_CONTROLLER".equals(c.getComponentType()) ||
                "CONFIGURATION".equals(c.getComponentType()) || "COMPONENT".equals(c.getComponentType()) ||
                "BEAN".equals(c.getComponentType()))
            .toList();

        for (SpringComponent component : components) {
            boolean match = true;

            if (query != null && !query.isEmpty()) {
                String searchText = (component.getClassName() != null ? component.getClassName() : "") + " " +
                    (component.getComponentName() != null ? component.getComponentName() : "");
                match = searchText.toLowerCase().contains(query.toLowerCase());
            }

            if (packageName != null && !packageName.isEmpty()) {
                String packageNameLower = packageName.toLowerCase();
                match = match && (component.getPackageName() != null && component.getPackageName().toLowerCase().contains(packageNameLower));
            }

            if (annotation != null && !annotation.isEmpty()) {
                match = hasAnnotation(component, annotation);
            }

            if (match) {
                results.add(toComponentMap(component));
            }
        }

        totalElements = results.size();

        // Apply pagination
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) totalElements);
        if (fromIndex < totalElements) {
            results = results.subList(fromIndex, toIndex);
        } else {
            results = new ArrayList<>();
        }

        return new SearchService.ComponentSearchResult(results, totalElements);
    }

    // ==================== Dependency Search ====================

    @Override
    public DependencySearchResult searchDependencies(String repositoryId, String query, String dependencyType, String scope, int page, int pageSize, String sortBy, String sortDir) {
        List<Map<String, Object>> results = new ArrayList<>();
        long totalElements = 0;

        if (dependencyRepository != null) {
            List<Dependency> dependencies = dependencyRepository.findByRepositoryId(repositoryId);
            for (Dependency dep : dependencies) {
                boolean match = true;

                if (query != null && !query.isEmpty()) {
                    String searchText = (dep.getGroupId() != null ? dep.getGroupId() : "") + " " +
                        (dep.getArtifactId() != null ? dep.getArtifactId() : "");
                    match = searchText.toLowerCase().contains(query.toLowerCase());
                }

                if (match && dependencyType != null && !dependencyType.isEmpty()) {
                    match = dep.getType() != null && dep.getType().toString().equals(dependencyType.toUpperCase());
                }

                if (match) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("type", "DEPENDENCY");
                    result.put("data", toDependencyMap(dep));
                    results.add(result);
                }
            }
        }

        totalElements = results.size();

        // Apply pagination
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) totalElements);
        if (fromIndex < totalElements) {
            results = results.subList(fromIndex, toIndex);
        } else {
            results = new ArrayList<>();
        }

        return new DependencySearchResult(results, totalElements);
    }

    // ==================== Annotation Search ====================

    @Override
    public AnnotationSearchResult searchAnnotations(String repositoryId, String query, int page, int pageSize, String sortBy, String sortDir) {
        List<Map<String, Object>> results = new ArrayList<>();
        long totalElements = 0;

        String searchPattern = query != null && !query.isEmpty() ? query : "";
        List<AnnotationInfo> allAnnotations = indexRepository.searchAnnotationsByName(searchPattern);

        for (AnnotationInfo annotationInfo : allAnnotations) {
            results.add(toAnnotationMap(annotationInfo));
        }

        totalElements = results.size();

        // Apply pagination
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) totalElements);
        if (fromIndex < totalElements) {
            results = results.subList(fromIndex, toIndex);
        } else {
            results = new ArrayList<>();
        }

        return new AnnotationSearchResult(results, totalElements);
    }

    // ==================== Search Statistics ====================

    @Override
    public Map<String, Long> getSearchStatistics(String repositoryId) {
        Map<String, Long> stats = new HashMap<>();

        // Count classes
        Long classCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_info WHERE class_type = 'CLASS'", Long.class);
        stats.put("classes", classCount != null ? classCount : 0L);

        // Count interfaces
        Long interfaceCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_info WHERE class_type = 'INTERFACE'", Long.class);
        stats.put("interfaces", interfaceCount != null ? interfaceCount : 0L);

        // Count enums
        Long enumCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_info WHERE class_type = 'ENUM'", Long.class);
        stats.put("enums", enumCount != null ? enumCount : 0L);

        // Count records
        Long recordCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_info WHERE class_type = 'RECORD'", Long.class);
        stats.put("records", recordCount != null ? recordCount : 0L);

        // Count methods
        Long methodCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM method_info", Long.class);
        stats.put("methods", methodCount != null ? methodCount : 0L);

        // Count fields
        Long fieldCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM field_info", Long.class);
        stats.put("fields", fieldCount != null ? fieldCount : 0L);

        // Count Spring components
        Long componentCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM spring_component WHERE component_type IN ('SERVICE', 'REPOSITORY', 'CONTROLLER', 'REST_CONTROLLER', 'CONFIGURATION', 'COMPONENT', 'BEAN')",
            Long.class);
        stats.put("components", componentCount != null ? componentCount : 0L);

        // Count REST endpoints
        Long endpointCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM spring_component WHERE component_type = 'REST_ENDPOINT'",
            Long.class);
        stats.put("endpoints", endpointCount != null ? endpointCount : 0L);

        // Count indexed files
        Long fileCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM file_index", Long.class);
        stats.put("files", fileCount != null ? fileCount : 0L);

        return stats;
    }

    // ==================== Helper Methods ====================

    private String findFilePathForClass(Long classId) {
        String sql = "SELECT fi.file_path FROM file_index fi INNER JOIN class_info ci ON ci.file_index_id = fi.id WHERE ci.id = ?";
        List<String> results = jdbcTemplate.queryForList(sql, String.class, classId);
        return results.isEmpty() ? null : results.get(0);
    }

    private boolean hasAnnotation(SpringComponent component, String annotation) {
        String annotationUpper = annotation.toUpperCase();
        switch (annotationUpper) {
            case "AUTOWIRED": return component.hasAutowired();
            case "SERVICE": return component.isService();
            case "REPOSITORY": return component.isRepository();
            case "CONTROLLER": return component.isController();
            case "REST_CONTROLLER": return component.isRestController();
            case "CONFIGURATION": return component.isConfiguration();
            case "COMPONENT": return component.isComponent();
            case "BEAN": return component.isBean();
            case "TRANSACTIONAL": return component.hasTransactional();
            default: return false;
        }
    }

    private String getSortColumn(String sortBy, String defaultType) {
        if (sortBy == null || sortBy.isEmpty()) {
            return "class_name";
        }
        switch (sortBy.toLowerCase()) {
            case "name": return "class_name";
            case "package": return "file_path";
            case "type": return "class_type";
            default: return "class_name";
        }
    }

    // ==================== Mapper Methods ====================

    private Map<String, Object> toClassMap(ClassInfo cls) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", cls.getId());
        map.put("className", cls.getClassName());
        map.put("classType", cls.getClassType());
        map.put("visibility", cls.getVisibility());
        map.put("superClass", cls.getSuperClass());
        map.put("interfaces", cls.getInterfaces());
        return map;
    }

    private Map<String, Object> toMethodMap(MethodInfo method) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", method.getId());
        map.put("classId", method.getClassId());
        map.put("methodName", method.getMethodName());
        map.put("methodSignature", method.getMethodSignature());
        map.put("returnType", method.getReturnType());
        map.put("visibility", method.getVisibility());
        map.put("static", method.isStatic());
        map.put("abstract", method.isAbstract());
        map.put("parameters", method.getParameters());
        map.put("exceptions", method.getExceptions());
        return map;
    }

    private Map<String, Object> toFieldMap(FieldInfo field) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", field.getId());
        map.put("classId", field.getClassId());
        map.put("fieldName", field.getFieldName());
        map.put("fieldType", field.getFieldType());
        map.put("visibility", field.getVisibility());
        map.put("static", field.isStatic());
        map.put("final", field.isFinal());
        return map;
    }

    private Map<String, Object> toAnnotationMap(AnnotationInfo annotation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", annotation.getId());
        map.put("annotationName", annotation.getAnnotationName());
        map.put("fullName", annotation.getFullName());
        map.put("targetType", annotation.getTargetType());
        map.put("targetId", annotation.getTargetId());
        return map;
    }

    private Map<String, Object> toComponentMap(SpringComponent component) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", component.getId());
        map.put("componentName", component.getComponentName());
        map.put("componentType", component.getComponentType());
        map.put("className", component.getClassName());
        map.put("packageName", component.getPackageName());
        map.put("sourceFile", component.getSourceFile());
        return map;
    }

    private Map<String, Object> toEndpointMap(SpringComponent component) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", component.getId());
        map.put("className", component.getClassName());
        map.put("componentName", component.getComponentName());
        map.put("packageName", component.getPackageName());
        map.put("sourceFile", component.getSourceFile());
        map.put("isRestController", component.isRestController());

        // Parse HTTP method and path from componentName (stored as "HTTP_METHOD /path")
        String componentName = component.getComponentName();
        if (componentName != null && componentName.contains(" ")) {
            String[] parts = componentName.split(" ", 2);
            map.put("httpMethod", parts[0]);
            map.put("endpointPath", parts.length > 1 ? parts[1] : "");
        }

        // Parse media types from sourceFile (stored as "produces=...;consumes=...")
        String sourceFile = component.getSourceFile();
        if (sourceFile != null && !sourceFile.isEmpty()) {
            map.put("mediaTypes", sourceFile);
        }

        return map;
    }

    private Map<String, Object> toDependencyMap(Dependency dep) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", dep.getRepositoryId() + ":" + dep.getGroupId() + ":" + dep.getArtifactId());
        map.put("name", dep.getArtifactId());
        map.put("groupId", dep.getGroupId());
        map.put("artifactId", dep.getArtifactId());
        map.put("version", dep.getVersion());
        map.put("type", dep.getType() != null ? dep.getType().toString() : null);
        map.put("scope", dep.getConfiguration());
        return map;
    }
}