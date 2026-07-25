package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.RepositoryStatus;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.repositorysearch.RepositorySearchResult;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import com.projectiq.indexerlocal.service.impl.RepositorySearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RepositorySearchServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class RepositorySearchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RepositoryRepository repositoryRepository;

    private RepositorySearchService repositorySearchService;

    @BeforeEach
    void setUp() {
        repositorySearchService = new RepositorySearchServiceImpl(jdbcTemplate, repositoryRepository);
    }

    @Test
    void findFiles_WithFileName_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        RepositorySearchResult mockResult = new RepositorySearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setName("App.java");
        mockResult.setAbsolutePath("C:/projects/my-app/src/main/java/App.java");
        mockResult.setRelativePath("src/main/java/App.java");
        mockResult.setExtension("java");
        mockResult.setResourceType("FILE");
        mockResult.setFileSize(2048L);
        mockResult.setLanguage("Java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFiles(
                "repo-1", "App.java", null, null, "exact", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("App.java", result.getContent().get(0).getName());
        assertEquals("FILE", result.getContent().get(0).getResourceType());
    }

    @Test
    void findFiles_WithExtensionFilter_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(2L);

        RepositorySearchResult file1 = new RepositorySearchResult();
        file1.setRepositoryId("repo-1");
        file1.setName("App.java");
        file1.setExtension("java");
        file1.setResourceType("FILE");

        RepositorySearchResult file2 = new RepositorySearchResult();
        file2.setRepositoryId("repo-1");
        file2.setName("Service.java");
        file2.setExtension("java");
        file2.setResourceType("FILE");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(file1, file2));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFiles(
                null, null, "java", null, "exact", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findFiles_WithSizeFilter_ReturnsFilteredResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        RepositorySearchResult mockResult = new RepositorySearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setName("large-file.bin");
        mockResult.setFileSize(1024000L);
        mockResult.setResourceType("FILE");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFiles(
                null, null, null, null, "partial", 1000L, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findFolders_WithFolderName_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        RepositorySearchResult mockResult = new RepositorySearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setName("src");
        mockResult.setResourceType("FOLDER");
        mockResult.setDirectoryClassification("SOURCE");
        mockResult.setDepth(1);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFolders(
                "repo-1", "src", null, "prefix", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("FOLDER", result.getContent().get(0).getResourceType());
    }

    @Test
    void findFolders_WithClassification_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        RepositorySearchResult mockResult = new RepositorySearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setName("resources");
        mockResult.setResourceType("FOLDER");
        mockResult.setDirectoryClassification("RESOURCE");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFolders(
                "repo-1", null, "RESOURCE", "partial", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("RESOURCE", result.getContent().get(0).getDirectoryClassification());
    }

    @Test
    void findExtensions_WithRepository_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(3L);

        List<String> mockExtensions = List.of("java", "xml", "properties");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(mockExtensions.stream().map(ext -> ext).toList());

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findExtensions(
                "repo-1", null, null, "partial", 0, 20);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
    }

    @Test
    void findRepositories_WithName_ReturnsResults() {
        com.projectiq.indexerlocal.model.Repository repo = new com.projectiq.indexerlocal.model.Repository();
        repo.setRepositoryId("repo-1");
        repo.setRepositoryName("my-project");
        repo.setOriginalPath("C:/projects/my-project");
        repo.setStatus(RepositoryStatus.INDEXED);
        repo.setLastIndexingTimestamp(LocalDateTime.now());

        // Mock schema init and repository search
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        RepositorySearchResult mockResult = new RepositorySearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setRepositoryName("my-project");
        mockResult.setName("my-project");
        mockResult.setResourceType("REPOSITORY");
        mockResult.setRepositoryStatus("INDEXED");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findRepositories(
                null, "my-project", null, null, null, "partial", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("my-project", result.getContent().get(0).getRepositoryName());
    }

    @Test
    void findRepositories_WithStatusFilter_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        RepositorySearchResult mockResult = new RepositorySearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setRepositoryName("indexed-project");
        mockResult.setResourceType("REPOSITORY");
        mockResult.setRepositoryStatus("INDEXED");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findRepositories(
                null, null, "INDEXED", null, null, "partial", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("INDEXED", result.getContent().get(0).getRepositoryStatus());
    }

    @Test
    void findLanguages_WithRepository_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(3L);

        List<String> mockClassifications = List.of("JAVA_SOURCE", "XML", "PROPERTIES");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(mockClassifications.stream().map(cls -> cls).toList());

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findLanguages(
                "repo-1", null, "partial", 0, 20);

        assertNotNull(result);
        assertTrue(result.getTotalElements() > 0);
    }

    @Test
    void findRootModules_WithRepository_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(2L);

        RepositorySearchResult module1 = new RepositorySearchResult();
        module1.setRepositoryId("repo-1");
        module1.setName("src");
        module1.setResourceType("FOLDER");
        module1.setDepth(1);

        RepositorySearchResult module2 = new RepositorySearchResult();
        module2.setRepositoryId("repo-1");
        module2.setName("lib");
        module2.setResourceType("FOLDER");
        module2.setDepth(1);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(module1, module2));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findRootModules(
                "repo-1", null, "partial", 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findSourceDirectories_WithRepository_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        RepositorySearchResult mockResult = new RepositorySearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setName("main");
        mockResult.setResourceType("FOLDER");
        mockResult.setDirectoryClassification("SOURCE");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findSourceDirectories(
                "repo-1", null, "partial", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SOURCE", result.getContent().get(0).getDirectoryClassification());
    }

    @Test
    void findResourceDirectories_WithRepository_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        RepositorySearchResult mockResult = new RepositorySearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setName("resources");
        mockResult.setResourceType("FOLDER");
        mockResult.setDirectoryClassification("RESOURCE");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findResourceDirectories(
                "repo-1", null, "partial", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("RESOURCE", result.getContent().get(0).getDirectoryClassification());
    }

    @Test
    void findTestDirectories_WithRepository_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(2L);

        RepositorySearchResult test1 = new RepositorySearchResult();
        test1.setRepositoryId("repo-1");
        test1.setName("test");
        test1.setResourceType("FOLDER");
        test1.setDirectoryClassification("TEST");

        RepositorySearchResult test2 = new RepositorySearchResult();
        test2.setRepositoryId("repo-1");
        test2.setName("integration-test");
        test2.setResourceType("FOLDER");
        test2.setDirectoryClassification("TEST");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(test1, test2));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findTestDirectories(
                "repo-1", null, "partial", 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("TEST", result.getContent().get(0).getDirectoryClassification());
    }

    @Test
    void findFiles_WithNoResults_ReturnsEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(0L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of());

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFiles(
                "nonexistent-repo", null, null, null, "partial", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void pagination_WithPageSize_ReturnsCorrectPagination() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(5L);

        RepositorySearchResult mockResult = new RepositorySearchResult();
        mockResult.setRepositoryId("repo-1");
        mockResult.setName("file.java");
        mockResult.setResourceType("FILE");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockResult));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFiles(
                null, null, "java", null, "exact", null, null, 0, 2);

        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(0, result.getPageNumber());
        assertEquals(2, result.getPageSize());
        assertTrue(result.isFirstPage());
    }

    @Test
    void findFiles_WithPrefixMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(2L);

        RepositorySearchResult file1 = new RepositorySearchResult();
        file1.setRepositoryId("repo-1");
        file1.setName("AppMain.java");
        file1.setExtension("java");
        file1.setResourceType("FILE");

        RepositorySearchResult file2 = new RepositorySearchResult();
        file2.setRepositoryId("repo-1");
        file2.setName("AppConfig.java");
        file2.setExtension("java");
        file2.setResourceType("FILE");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(file1, file2));

        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFiles(
                null, "App", null, null, "prefix", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }
}