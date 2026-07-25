package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.springsearch.SpringSearchResult;
import com.projectiq.indexerlocal.service.impl.SpringSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SpringSearchServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class SpringSearchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SpringSearchService springSearchService;

    @BeforeEach
    void setUp() {
        springSearchService = new SpringSearchServiceImpl(jdbcTemplate);
    }

    @Test
    void findControllers_WithRepositoryId_ReturnsResults() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(2L);

        SpringSearchResult ctrl1 = createResult("UserController", "REST_CONTROLLER", "com.example.controller", "repo-1", "/path/UserController.java");
        SpringSearchResult ctrl2 = createResult("AdminController", "CONTROLLER", "com.example.controller", "repo-1", "/path/AdminController.java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(ctrl1, ctrl2));

        List<SpringSearchResult> results = springSearchService.findControllers("repo-1", null, null, 0, 20);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> "UserController".equals(r.getComponentName())));
        assertTrue(results.stream().anyMatch(r -> "AdminController".equals(r.getComponentName())));
    }

    @Test
    void findControllers_WithPackageFilter_ReturnsFilteredResults() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult ctrl = createResult("UserController", "REST_CONTROLLER", "com.example.api", "repo-1", "/path/UserController.java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(ctrl));

        List<SpringSearchResult> results = springSearchService.findControllers("repo-1", "com.example.api", null, 0, 20);

        assertEquals(1, results.size());
        assertEquals("com.example.api", results.get(0).getPackageName());
    }

    @Test
    void findServices_ReturnsServices() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult svc = createResult("UserService", "SERVICE", "com.example.service", "repo-1", "/path/UserService.java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(svc));

        List<SpringSearchResult> results = springSearchService.findServices("repo-1", null, null, 0, 20);

        assertEquals(1, results.size());
        assertEquals("SERVICE", results.get(0).getComponentType());
        assertEquals("@Service", results.get(0).getAnnotation());
    }

    @Test
    void findRepositories_ReturnsRepositories() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult repo = createResult("UserRepository", "REPOSITORY", "com.example.repo", "repo-1", "/path/UserRepository.java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(repo));

        List<SpringSearchResult> results = springSearchService.findRepositories("repo-1", null, null, 0, 20);

        assertEquals(1, results.size());
        assertEquals("REPOSITORY", results.get(0).getComponentType());
        assertEquals("@Repository", results.get(0).getAnnotation());
    }

    @Test
    void findComponents_ReturnsComponents() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult comp = createResult("MyComponent", "COMPONENT", "com.example.comp", "repo-1", "/path/MyComponent.java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(comp));

        List<SpringSearchResult> results = springSearchService.findComponents("repo-1", null, null, 0, 20);

        assertEquals(1, results.size());
        assertEquals("COMPONENT", results.get(0).getComponentType());
    }

    @Test
    void findConfigurationClasses_ReturnsConfigurations() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult config = createResult("AppConfig", "CONFIGURATION", "com.example.config", "repo-1", "/path/AppConfig.java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(config));

        List<SpringSearchResult> results = springSearchService.findConfigurationClasses("repo-1", null, null, 0, 20);

        assertEquals(1, results.size());
        assertEquals("CONFIGURATION", results.get(0).getComponentType());
        assertEquals("@Configuration", results.get(0).getAnnotation());
    }

    @Test
    void findEndpoints_WithHttpMethodFilter_ReturnsFilteredEndpoints() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult endpoint = createResult("GET /api/users", "REST_ENDPOINT", "com.example.controller", "repo-1", "/path/UserController.java");
        endpoint.setHttpMethod("GET");
        endpoint.setRestPath("/api/users");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(endpoint));

        List<SpringSearchResult> results = springSearchService.findEndpoints("repo-1", "GET", null, null, null, null, 0, 20);

        assertEquals(1, results.size());
        assertEquals("GET", results.get(0).getHttpMethod());
    }

    @Test
    void findEndpoints_WithPathFilter_ReturnsFilteredEndpoints() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult endpoint = createResult("POST /api/users", "REST_ENDPOINT", "com.example.controller", "repo-1", "/path/UserController.java");
        endpoint.setHttpMethod("POST");
        endpoint.setRestPath("/api/users");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(endpoint));

        List<SpringSearchResult> results = springSearchService.findEndpoints("repo-1", null, "/api/users", null, null, null, 0, 20);

        assertEquals(1, results.size());
        assertEquals("/api/users", results.get(0).getRestPath());
    }

    @Test
    void findBeans_WithBeanNameFilter_ReturnsFilteredBeans() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult bean = createResult("dataSource", "BEAN", "com.example.config", "repo-1", "/path/DataSourceConfig.java");
        bean.setBeanName("dataSource");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(bean));

        List<SpringSearchResult> results = springSearchService.findBeans("repo-1", null, "dataSource", null, 0, 20);

        assertEquals(1, results.size());
        assertEquals("dataSource", results.get(0).getBeanName());
    }

    @Test
    void findScheduledTasks_ReturnsScheduledComponents() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult scheduled = createResult("SchedulerService", "SERVICE", "com.example.schedule", "repo-1", "/path/SchedulerService.java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(scheduled));

        List<SpringSearchResult> results = springSearchService.findScheduledTasks("repo-1", null, null, 0, 20);

        assertEquals(1, results.size());
    }

    @Test
    void findEventListeners_ReturnsEventListenerComponents() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult listener = createResult("UserEventListener", "COMPONENT", "com.example.event", "repo-1", "/path/UserEventListener.java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(listener));

        List<SpringSearchResult> results = springSearchService.findEventListeners("repo-1", null, null, 0, 20);

        assertEquals(1, results.size());
    }

    @Test
    void findControllers_WithEmptyRepository_ReturnsEmptyList() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(0L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of());

        List<SpringSearchResult> results = springSearchService.findControllers("non-existent-repo", null, null, 0, 20);

        assertTrue(results.isEmpty());
    }

    @Test
    void findControllers_WithPagination_ReturnsCorrectPage() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(3L);

        SpringSearchResult ctrl = createResult("UserController", "REST_CONTROLLER", "com.example.controller", "repo-1", "/path/UserController.java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(ctrl));

        List<SpringSearchResult> results = springSearchService.findControllers("repo-1", null, null, 1, 1);

        assertEquals(1, results.size());
    }

    @Test
    void countControllers_ReturnsCorrectCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(5L);

        long count = springSearchService.countControllers("repo-1", null, null);

        assertEquals(5L, count);
    }

    @Test
    void countEndpoints_ReturnsCorrectCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(10L);

        long count = springSearchService.countEndpoints("repo-1", "GET", null, null, null, null);

        assertEquals(10L, count);
    }

    @Test
    void findControllers_WithModuleFilter_UsesModuleFilter() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SpringSearchResult ctrl = createResult("UserController", "REST_CONTROLLER", "com.example.controller", "repo-1", "/workspace/my-module/src/main/java/com/example/controller/UserController.java");
        ctrl.setModule("my-module");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(ctrl));

        List<SpringSearchResult> results = springSearchService.findControllers("repo-1", null, "my-module", 0, 20);

        assertEquals(1, results.size());
        assertEquals("my-module", results.get(0).getModule());
    }

    // ==================== Helper Methods ====================

    private SpringSearchResult createResult(String componentName, String componentType,
                                             String packageName, String repositoryId,
                                             String sourceFile) {
        SpringSearchResult result = new SpringSearchResult();
        result.setComponentName(componentName);
        result.setComponentType(componentType);
        result.setClassName(componentName);
        result.setPackageName(packageName);
        result.setRepositoryId(repositoryId);
        result.setFilePath(sourceFile);
        result.setAnnotation(deriveAnnotation(componentType));
        return result;
    }

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
}