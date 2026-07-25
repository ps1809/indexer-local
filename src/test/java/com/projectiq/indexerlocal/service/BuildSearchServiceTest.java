package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.Dependency;
import com.projectiq.indexerlocal.model.DependencyType;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.buildsearch.BuildSearchResult;
import com.projectiq.indexerlocal.repository.DependencyRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import com.projectiq.indexerlocal.service.impl.BuildSearchServiceImpl;
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
 * Unit tests for BuildSearchServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class BuildSearchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private DependencyRepository dependencyRepository;

    private BuildSearchService buildSearchService;

    @BeforeEach
    void setUp() {
        buildSearchService = new BuildSearchServiceImpl(jdbcTemplate, repositoryRepository, dependencyRepository);
    }

    @Test
    void findMavenProjects_WithRepositoryFilter_ReturnsResults() {
        // Mock count query
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        // Mock data query
        BuildSearchResult mockResult = new BuildSearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setBuildSystem("MAVEN");
        mockResult.setBuildFilePath("pom.xml");
        mockResult.setGroupId("com.example");
        mockResult.setArtifactId("my-app");
        mockResult.setVersion("1.0.0");
        mockResult.setPackaging("jar");
        mockResult.setProjectType("Single Module");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findMavenProjects(
                "repo-1", null, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("MAVEN", result.getContent().get(0).getBuildSystem());
        assertEquals("repo-1", result.getContent().get(0).getRepositoryId());
    }

    @Test
    void findGradleProjects_WithProjectName_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        BuildSearchResult mockResult = new BuildSearchResult();
        mockResult.setRepositoryId("repo-2");
        mockResult.setBuildSystem("GRADLE");
        mockResult.setBuildFilePath("build.gradle");
        mockResult.setModuleName("my-gradle-app");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findGradleProjects(
                null, "my-gradle-app", null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("GRADLE", result.getContent().get(0).getBuildSystem());
    }

    @Test
    void findModules_WithModuleName_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(2L);

        BuildSearchResult module1 = new BuildSearchResult();
        module1.setRepositoryId("repo-1");
        module1.setModuleName("module-a");
        module1.setBuildSystem("MAVEN");

        BuildSearchResult module2 = new BuildSearchResult();
        module2.setRepositoryId("repo-1");
        module2.setModuleName("module-b");
        module2.setBuildSystem("MAVEN");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(module1, module2));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findModules(
                "repo-1", "module", null, 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findPlugins_WithPluginName_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        BuildSearchResult mockResult = new BuildSearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setBuildSystem("MAVEN");
        mockResult.setSpringBootVersion("3.2.0");
        mockResult.setPlugins(List.of("spring-boot:3.2.0"));

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findPlugins(
                "repo-1", "spring-boot", null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findDependencies_WithGroupId_ReturnsResults() {
        Dependency dep = new Dependency("org.springframework", "spring-core", "6.1.0", DependencyType.COMPILE);
        dep.setRepositoryId("repo-1");

        when(dependencyRepository.findByRepositoryId("repo-1"))
            .thenReturn(List.of(dep));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findDependencies(
                "repo-1", "org.springframework", null, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getDependencies().stream()
                .anyMatch(d -> d.contains("spring-core")));
    }

    @Test
    void findBuildProfiles_WithProfileName_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        BuildSearchResult mockResult = new BuildSearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setBuildSystem("MAVEN");
        mockResult.setProjectType("Multi Module");
        mockResult.setProfiles(List.of("Multi Module"));

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findBuildProfiles(
                "repo-1", "Multi Module", null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findParentProjects_WithParentArtifactId_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        BuildSearchResult mockResult = new BuildSearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setBuildSystem("MAVEN");
        mockResult.setParentModule("spring-boot-starter-parent");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findParentProjects(
                "repo-1", null, "spring-boot-starter-parent", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("spring-boot-starter-parent", result.getContent().get(0).getParentModule());
    }

    @Test
    void findChildModules_WithParentModule_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(2L);

        BuildSearchResult child1 = new BuildSearchResult();
        child1.setRepositoryId("repo-1");
        child1.setModuleName("child-a");
        child1.setProjectType("Multi Module");

        BuildSearchResult child2 = new BuildSearchResult();
        child2.setRepositoryId("repo-1");
        child2.setModuleName("child-b");
        child2.setProjectType("Multi Module");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(child1, child2));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findChildModules(
                "repo-1", "parent-app", 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findBuildConfigurations_WithPackaging_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        BuildSearchResult mockResult = new BuildSearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setBuildSystem("MAVEN");
        mockResult.setPackaging("war");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findBuildConfigurations(
                "repo-1", "MAVEN", "war", null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("war", result.getContent().get(0).getPackaging());
    }

    @Test
    void searchBuild_WithMultipleFilters_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        BuildSearchResult mockResult = new BuildSearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setBuildSystem("MAVEN");
        mockResult.setGroupId("com.example");
        mockResult.setArtifactId("my-app");
        mockResult.setVersion("1.0.0");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.searchBuild(
                "repo-1", "MAVEN", null, "com.example", "my-app", "1.0.0",
                null, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findBuildFiles_WithBuildFileName_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        BuildSearchResult mockResult = new BuildSearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setBuildSystem("MAVEN");
        mockResult.setBuildFilePath("pom.xml");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findBuildFiles(
                "repo-1", "MAVEN", "pom.xml", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("pom.xml", result.getContent().get(0).getBuildFilePath());
    }

    @Test
    void searchBuild_WithNoResults_ReturnsEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(0L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of());

        PaginatedResponse<BuildSearchResult> result = buildSearchService.searchBuild(
                "nonexistent-repo", null, null, null, null, null,
                null, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void findDependencies_WithNoMatchingDeps_ReturnsEmpty() {
        when(dependencyRepository.findByRepositoryId("empty-repo"))
            .thenReturn(List.of());

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findDependencies(
                "empty-repo", null, null, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void pagination_WithPageSize_ReturnsCorrectPagination() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(5L);

        BuildSearchResult mockResult = new BuildSearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setBuildSystem("MAVEN");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findMavenProjects(
                null, null, null, null, 0, 2);

        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(0, result.getPageNumber());
        assertEquals(2, result.getPageSize());
        assertTrue(result.isFirstPage());
    }

    @Test
    void findDependencies_WithVersionFilter_ReturnsFilteredResults() {
        Dependency dep1 = new Dependency("org.springframework", "spring-core", "6.1.0", DependencyType.COMPILE);
        dep1.setRepositoryId("repo-1");
        Dependency dep2 = new Dependency("org.springframework", "spring-boot", "3.2.0", DependencyType.COMPILE);
        dep2.setRepositoryId("repo-1");

        when(dependencyRepository.findByRepositoryId("repo-1"))
            .thenReturn(List.of(dep1, dep2));

        PaginatedResponse<BuildSearchResult> result = buildSearchService.findDependencies(
                "repo-1", null, null, "6.1", null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}