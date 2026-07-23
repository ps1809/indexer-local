package com.projectiq.indexerlocal.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for searching indexed repository metadata.
 * All search operations query only persisted indexed data without filesystem scanning or code parsing.
 */
public interface SearchService {

    // ==================== General Search ====================

    /**
     * General search across all indexed entity types for a repository.
     * Performs case-insensitive exact and partial matching.
     *
     * @param repositoryId the repository ID
     * @param query the search query (case-insensitive)
     * @param packageName optional package filter
     * @param annotation optional annotation filter
     * @return list of search results as maps with 'type' and 'data' keys
     */
    List<Map<String, Object>> generalSearch(String repositoryId, String query, String packageName, String annotation);

    // ==================== Class Search ====================

    /**
     * Search classes for a repository.
     *
     * @param repositoryId the repository ID
     * @param query the search query
     * @param packageName optional package filter
     * @param page page number (0-based)
     * @param pageSize page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @return paginated class results with total element count
     */
    ClassSearchResult searchClasses(String repositoryId, String query, String packageName, int page, int pageSize, String sortBy, String sortDir);

    /**
     * Search classes by specific entity type.
     *
     * @param repositoryId the repository ID
     * @param query the search query
     * @param entityType the class type filter (CLASS, INTERFACE, ENUM, RECORD)
     * @param visibility optional visibility filter
     * @param packageName optional package filter
     * @param page page number (0-based)
     * @param pageSize page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @return paginated class results with total element count
     */
    ClassSearchResult searchClassesByType(String repositoryId, String query, String entityType, String visibility, String packageName, int page, int pageSize, String sortBy, String sortDir);

    // ==================== Method Search ====================

    /**
     * Search methods for a repository.
     *
     * @param repositoryId the repository ID
     * @param query the search query
     * @param packageName optional package filter
     * @param page page number (0-based)
     * @param pageSize page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @return paginated method results with total element count
     */
    MethodSearchResult searchMethods(String repositoryId, String query, String packageName, int page, int pageSize, String sortBy, String sortDir);

    // ==================== Field Search ====================

    /**
     * Search fields for a repository.
     *
     * @param repositoryId the repository ID
     * @param query the search query
     * @param packageName optional package filter
     * @param page page number (0-based)
     * @param pageSize page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @return paginated field results with total element count
     */
    FieldSearchResult searchFields(String repositoryId, String query, String packageName, int page, int pageSize, String sortBy, String sortDir);

    // ==================== REST API Search ====================

    /**
     * Search REST endpoints for a repository.
     *
     * @param repositoryId the repository ID
     * @param query the search query
     * @param httpMethod optional HTTP method filter
     * @param page page number (0-based)
     * @param pageSize page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @return paginated endpoint results with total element count
     */
    EndpointSearchResult searchRestEndpoints(String repositoryId, String query, String httpMethod, int page, int pageSize, String sortBy, String sortDir);

    // ==================== Spring Component Search ====================

    /**
     * Search Spring components for a repository.
     *
     * @param repositoryId the repository ID
     * @param query the search query
     * @param packageName optional package filter
     * @param annotation optional annotation filter
     * @param page page number (0-based)
     * @param pageSize page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @return paginated component results with total element count
     */
    ComponentSearchResult searchComponents(String repositoryId, String query, String packageName, String annotation, int page, int pageSize, String sortBy, String sortDir);

    // ==================== Dependency Search ====================

    /**
     * Search dependencies for a repository.
     *
     * @param repositoryId the repository ID
     * @param query the search query
     * @param dependencyType optional dependency type filter
     * @param scope optional scope filter
     * @param page page number (0-based)
     * @param pageSize page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @return paginated dependency results with total element count
     */
    DependencySearchResult searchDependencies(String repositoryId, String query, String dependencyType, String scope, int page, int pageSize, String sortBy, String sortDir);

    // ==================== Annotation Search ====================

    /**
     * Search annotations for a repository.
     *
     * @param repositoryId the repository ID
     * @param query the search query
     * @param page page number (0-based)
     * @param pageSize page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @return paginated annotation results with total element count
     */
    AnnotationSearchResult searchAnnotations(String repositoryId, String query, int page, int pageSize, String sortBy, String sortDir);

    // ==================== Search Statistics ====================

    /**
     * Get search statistics for a repository.
     * Returns counts for each indexed entity type.
     *
     * @param repositoryId the repository ID
     * @return map of entity type to count
     */
    Map<String, Long> getSearchStatistics(String repositoryId);

    /**
     * Result wrapper for class search.
     */
    class ClassSearchResult {
        private final List<Map<String, Object>> content;
        private final long totalElements;

        public ClassSearchResult(List<Map<String, Object>> content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }

        public List<Map<String, Object>> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
    }

    /**
     * Result wrapper for method search.
     */
    class MethodSearchResult {
        private final List<Map<String, Object>> content;
        private final long totalElements;

        public MethodSearchResult(List<Map<String, Object>> content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }

        public List<Map<String, Object>> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
    }

    /**
     * Result wrapper for field search.
     */
    class FieldSearchResult {
        private final List<Map<String, Object>> content;
        private final long totalElements;

        public FieldSearchResult(List<Map<String, Object>> content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }

        public List<Map<String, Object>> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
    }

    /**
     * Result wrapper for endpoint search.
     */
    class EndpointSearchResult {
        private final List<Map<String, Object>> content;
        private final long totalElements;

        public EndpointSearchResult(List<Map<String, Object>> content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }

        public List<Map<String, Object>> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
    }

    /**
     * Result wrapper for component search.
     */
    class ComponentSearchResult {
        private final List<Map<String, Object>> content;
        private final long totalElements;

        public ComponentSearchResult(List<Map<String, Object>> content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }

        public List<Map<String, Object>> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
    }

    /**
     * Result wrapper for dependency search.
     */
    class DependencySearchResult {
        private final List<Map<String, Object>> content;
        private final long totalElements;

        public DependencySearchResult(List<Map<String, Object>> content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }

        public List<Map<String, Object>> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
    }

    /**
     * Result wrapper for annotation search.
     */
    class AnnotationSearchResult {
        private final List<Map<String, Object>> content;
        private final long totalElements;

        public AnnotationSearchResult(List<Map<String, Object>> content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }

        public List<Map<String, Object>> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
    }
}