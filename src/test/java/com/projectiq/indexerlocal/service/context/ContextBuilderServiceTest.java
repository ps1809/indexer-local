package com.projectiq.indexerlocal.service.context;

import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;
import com.projectiq.indexerlocal.model.symbol.SymbolEntry;
import com.projectiq.indexerlocal.model.symbol.SymbolSearchResult;
import com.projectiq.indexerlocal.model.springsearch.SpringSearchResult;
import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipEntry;
import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipType;
import com.projectiq.indexerlocal.model.buildsearch.BuildSearchResult;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.repositorysearch.RepositorySearchResult;
import com.projectiq.indexerlocal.service.*;
import com.projectiq.indexerlocal.service.context.impl.ContextBuilderServiceImpl;
import com.projectiq.indexerlocal.service.context.impl.PromptOptimizationServiceImpl;
import com.projectiq.indexerlocal.service.context.impl.SmartContextServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContextBuilderServiceTest {

    @Mock
    private SymbolSearchService symbolSearchService;
    @Mock
    private SpringSearchService springSearchService;
    @Mock
    private RelationshipSearchService relationshipSearchService;
    @Mock
    private BuildSearchService buildSearchService;
    @Mock
    private RepositorySearchService repositorySearchService;

    private ContextBuilderService contextBuilderService;
    private SmartContextService smartContextService;
    private PromptOptimizationService promptOptimizationService;

    @BeforeEach
    void setUp() {
        promptOptimizationService = new PromptOptimizationServiceImpl();
        smartContextService = new SmartContextServiceImpl(
                symbolSearchService, relationshipSearchService, springSearchService);
        contextBuilderService = new ContextBuilderServiceImpl(
                symbolSearchService, springSearchService, relationshipSearchService,
                buildSearchService, repositorySearchService,
                smartContextService, promptOptimizationService);
    }

    @Test
    void testBuildSymbolContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setSymbolName("UserService");
        request.setSymbolType("CLASS");
        request.setRepositoryId("test-repo");
        request.setContextType("symbol");
        request.setExpansionDepth(0);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("UserService");
        entry.setFullyQualifiedName("com.example.UserService");
        entry.setSymbolType("CLASS");
        entry.setPackageName("com.example");
        entry.setFilePath("/src/UserService.java");
        entry.setVisibility("PUBLIC");

        SymbolSearchResult searchResult = new SymbolSearchResult(
                List.of(entry), 0, 20, 1);

        when(symbolSearchService.searchSymbols(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(searchResult);

        // Act
        ContextResponse response = contextBuilderService.buildSymbolContext(request);

        // Assert
        assertNotNull(response);
        assertEquals("symbol", response.getContextType());
        assertFalse(response.getSymbols().isEmpty());
        assertEquals("UserService", response.getSymbols().get(0).getName());
        assertEquals("com.example.UserService", response.getSymbols().get(0).getFullyQualifiedName());
    }

    @Test
    void testBuildFileContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setFilePath("/src/UserService.java");
        request.setRepositoryId("test-repo");
        request.setContextType("file");
        request.setExpansionDepth(0);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("UserService");
        entry.setFullyQualifiedName("com.example.UserService");
        entry.setSymbolType("CLASS");
        entry.setPackageName("com.example");
        entry.setFilePath("/src/UserService.java");

        SymbolSearchResult searchResult = new SymbolSearchResult(
                List.of(entry), 0, 20, 1);

        when(symbolSearchService.searchSymbols(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(searchResult);

        // Act
        ContextResponse response = contextBuilderService.buildFileContext(request);

        // Assert
        assertNotNull(response);
        assertEquals("file", response.getContextType());
        assertFalse(response.getSymbols().isEmpty());
        assertFalse(response.getFiles().isEmpty());
    }

    @Test
    void testBuildPackageContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setPackageName("com.example");
        request.setRepositoryId("test-repo");
        request.setContextType("package");
        request.setExpansionDepth(0);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("UserService");
        entry.setFullyQualifiedName("com.example.UserService");
        entry.setSymbolType("CLASS");
        entry.setPackageName("com.example");
        entry.setFilePath("/src/UserService.java");

        SymbolSearchResult searchResult = new SymbolSearchResult(
                List.of(entry), 0, 20, 1);

        when(symbolSearchService.searchSymbols(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(searchResult);

        // Act
        ContextResponse response = contextBuilderService.buildPackageContext(request);

        // Assert
        assertNotNull(response);
        assertEquals("package", response.getContextType());
        assertFalse(response.getSymbols().isEmpty());
    }

    @Test
    void testBuildModuleContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setModuleName("core");
        request.setRepositoryId("test-repo");
        request.setContextType("module");
        request.setExpansionDepth(0);

        BuildSearchResult buildResult = new BuildSearchResult();
        buildResult.setModuleName("core");
        buildResult.setBuildFilePath("/core/pom.xml");

        PaginatedResponse<BuildSearchResult> moduleResult = PaginatedResponse.of(
                List.of(buildResult), 0, 20, 1, 1);

        when(buildSearchService.findModules(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(moduleResult);

        SymbolSearchResult emptyResult = new SymbolSearchResult(
                Collections.emptyList(), 0, 20, 0);

        when(symbolSearchService.searchSymbols(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(emptyResult);

        // Act
        ContextResponse response = contextBuilderService.buildModuleContext(request);

        // Assert
        assertNotNull(response);
        assertEquals("module", response.getContextType());
        assertFalse(response.getModules().isEmpty());
        assertEquals("core", response.getModules().get(0).getName());
    }

    @Test
    void testBuildRepositoryContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setContextType("repository");
        request.setExpansionDepth(0);

        RepositorySearchResult repoResult = new RepositorySearchResult();
        repoResult.setRepositoryName("test-repo");
        repoResult.setRepositoryId("test-repo");
        repoResult.setName("Test Repository");

        PaginatedResponse<RepositorySearchResult> repoResponse = PaginatedResponse.of(
                List.of(repoResult), 0, 1, 1, 1);

        when(repositorySearchService.findRepositories(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(repoResponse);

        PaginatedResponse<BuildSearchResult> emptyModules = PaginatedResponse.of(
                Collections.emptyList(), 0, 20, 0, 0);

        when(buildSearchService.findModules(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(emptyModules);

        SymbolSearchResult emptySymbols = new SymbolSearchResult(
                Collections.emptyList(), 0, 20, 0);

        when(symbolSearchService.searchSymbols(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(emptySymbols);

        // Act
        ContextResponse response = contextBuilderService.buildRepositoryContext(request);

        // Assert
        assertNotNull(response);
        assertEquals("repository", response.getContextType());
    }

    @Test
    void testBuildContextWithExpansion() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setSymbolName("UserService");
        request.setSymbolType("CLASS");
        request.setRepositoryId("test-repo");
        request.setContextType("symbol");
        request.setExpansionDepth(1);
        request.setIncludeDependencies(false);  // Disable to reduce mocking needs
        request.setIncludeSpringBeans(false);
        request.setIncludeConfiguration(false);
        request.setIncludeRelationships(false);
        request.setIncludeCallGraph(false);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("UserService");
        entry.setFullyQualifiedName("com.example.UserService");
        entry.setSymbolType("CLASS");
        entry.setPackageName("com.example");
        entry.setFilePath("/src/UserService.java");

        SymbolSearchResult searchResult = new SymbolSearchResult(
                List.of(entry), 0, 20, 1);

        when(symbolSearchService.searchSymbols(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(searchResult);

        // Act
        ContextResponse response = contextBuilderService.buildContext(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isOptimized());
        assertTrue(response.getTotalEntries() > 0);
    }

    @Test
    void testBuildContextWithEmptyRepository() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setRepositoryId("empty-repo");
        request.setContextType("symbol");
        request.setExpansionDepth(0);

        SymbolSearchResult emptyResult = new SymbolSearchResult(
                Collections.emptyList(), 0, 20, 0);

        when(symbolSearchService.searchSymbols(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(emptyResult);

        // Act
        ContextResponse response = contextBuilderService.buildContext(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSymbols().isEmpty());
        assertEquals(0, response.getTotalEntries());
    }

    @Test
    void testBuildContextWithNullRequest() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setContextType("symbol");
        request.setExpansionDepth(0);

        SymbolSearchResult emptyResult = new SymbolSearchResult(
                Collections.emptyList(), 0, 20, 0);

        when(symbolSearchService.searchSymbols(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(emptyResult);

        // Act
        ContextResponse response = contextBuilderService.buildContext(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getSymbols());
    }
}