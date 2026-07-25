package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.AIContextRequest;
import com.projectiq.indexerlocal.model.ai.AIContextResult;
import com.projectiq.indexerlocal.service.*;
import com.projectiq.indexerlocal.service.ai.impl.AIContextServiceImpl;
import com.projectiq.indexerlocal.service.context.ContextBuilderService;
import com.projectiq.indexerlocal.service.context.PromptOptimizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AIContextServiceTest {

    @Mock
    private SymbolSearchService symbolSearchService;
    @Mock
    private VectorSearchService vectorSearchService;
    @Mock
    private RepositoryKnowledgeGraphService knowledgeGraphService;
    @Mock
    private ContextBuilderService contextBuilderService;
    @Mock
    private PromptOptimizationService promptOptimizationService;
    @Mock
    private ChunkGenerationService chunkGenerationService;
    @Mock
    private SpringSearchService springSearchService;
    @Mock
    private RepositorySearchService repositorySearchService;
    @Mock
    private RelationshipSearchService relationshipSearchService;

    private AIContextService aiContextService;

    @BeforeEach
    void setUp() {
        aiContextService = new AIContextServiceImpl(
                symbolSearchService, vectorSearchService, knowledgeGraphService,
                contextBuilderService, promptOptimizationService, chunkGenerationService,
                springSearchService, repositorySearchService, relationshipSearchService);
    }

    @Test
    void testBuildContext() {
        AIContextRequest request = new AIContextRequest();
        request.setRepositoryId("test-repo");
        request.setQuery("test query");
        request.setIncludeVectorSearch(false);
        request.setIncludeKnowledgeGraph(false);
        request.setIncludeSymbolSearch(false);
        request.setIncludeSpringComponents(false);
        request.setIncludeRestApis(false);
        request.setIncludeConfiguration(false);
        request.setIncludeDocumentation(false);
        request.setIncludeRepositorySummary(false);

        AIContextResult result = aiContextService.buildContext(request);
        assertNotNull(result);
        assertEquals("test-repo", result.getRepositoryId());
        assertEquals("test query", result.getQuery());
    }

    @Test
    void testBuildSymbolContext() {
        AIContextRequest request = new AIContextRequest();
        request.setRepositoryId("test-repo");
        request.setSymbolName("TestClass");
        request.setSymbolType("CLASS");
        request.setIncludeVectorSearch(false);
        request.setIncludeKnowledgeGraph(false);
        request.setIncludeSpringComponents(false);
        request.setIncludeRestApis(false);
        request.setIncludeConfiguration(false);
        request.setIncludeDocumentation(false);
        request.setIncludeRepositorySummary(false);

        AIContextResult result = aiContextService.buildSymbolContext(request);
        assertNotNull(result);
    }

    @Test
    void testBuildFileContext() {
        AIContextRequest request = new AIContextRequest();
        request.setRepositoryId("test-repo");
        request.setFilePath("/path/to/TestFile.java");
        request.setIncludeVectorSearch(false);
        request.setIncludeKnowledgeGraph(false);
        request.setIncludeSpringComponents(false);
        request.setIncludeRestApis(false);
        request.setIncludeConfiguration(false);
        request.setIncludeDocumentation(false);
        request.setIncludeRepositorySummary(false);

        AIContextResult result = aiContextService.buildFileContext(request);
        assertNotNull(result);
    }

    @Test
    void testBuildPackageContext() {
        AIContextRequest request = new AIContextRequest();
        request.setRepositoryId("test-repo");
        request.setPackageName("com.test");
        request.setIncludeVectorSearch(false);
        request.setIncludeKnowledgeGraph(false);
        request.setIncludeSpringComponents(false);
        request.setIncludeRestApis(false);
        request.setIncludeConfiguration(false);
        request.setIncludeDocumentation(false);
        request.setIncludeRepositorySummary(false);

        AIContextResult result = aiContextService.buildPackageContext(request);
        assertNotNull(result);
    }

    @Test
    void testBuildModuleContext() {
        AIContextRequest request = new AIContextRequest();
        request.setRepositoryId("test-repo");
        request.setModuleName("test-module");
        request.setIncludeVectorSearch(false);
        request.setIncludeKnowledgeGraph(false);
        request.setIncludeSpringComponents(false);
        request.setIncludeRestApis(false);
        request.setIncludeConfiguration(false);
        request.setIncludeDocumentation(false);
        request.setIncludeRepositorySummary(false);

        AIContextResult result = aiContextService.buildModuleContext(request);
        assertNotNull(result);
    }

    @Test
    void testBuildRepositoryContext() {
        AIContextRequest request = new AIContextRequest();
        request.setRepositoryId("test-repo");
        request.setIncludeVectorSearch(false);
        request.setIncludeKnowledgeGraph(false);
        request.setIncludeSpringComponents(false);
        request.setIncludeRestApis(false);
        request.setIncludeConfiguration(false);
        request.setIncludeDocumentation(false);
        request.setIncludeRepositorySummary(false);

        AIContextResult result = aiContextService.buildRepositoryContext(request);
        assertNotNull(result);
    }

    @Test
    void testAIContextResult() {
        AIContextResult result = new AIContextResult();
        result.setQuery("test");
        result.setRepositoryId("test-repo");
        result.setRepositorySummary("Test summary");
        result.getRelevantFiles().add("/path/to/file.java");
        result.getRelatedClasses().add("TestClass");
        result.getRelatedMethods().add("testMethod");
        result.getDependencies().add("com.test.Dependency");
        result.getSpringComponents().add("TestController");
        result.getRestApis().add("GET /api/test");
        result.getConfigurations().add("AppConfig");
        result.getDocumentation().add("README.md");

        assertEquals("test", result.getQuery());
        assertEquals("test-repo", result.getRepositoryId());
        assertEquals("Test summary", result.getRepositorySummary());
        assertEquals(1, result.getRelevantFiles().size());
        assertEquals(1, result.getRelatedClasses().size());
        assertEquals(1, result.getRelatedMethods().size());
        assertEquals(1, result.getDependencies().size());
        assertEquals(1, result.getSpringComponents().size());
        assertEquals(1, result.getRestApis().size());
        assertEquals(1, result.getConfigurations().size());
        assertEquals(1, result.getDocumentation().size());
        assertNotNull(result.getGeneratedAt());
    }

    @Test
    void testAIContextRequest() {
        AIContextRequest request = new AIContextRequest();
        request.setRepositoryId("test-repo");
        request.setQuery("test query");
        request.setTopK(20);
        request.setMaxGraphDepth(3);
        request.setMaxTokens(8192);
        request.setMinSimilarityScore(0.5);
        request.setIncludeVectorSearch(true);
        request.setIncludeKnowledgeGraph(true);
        request.setOptimizePrompt(true);

        assertEquals("test-repo", request.getRepositoryId());
        assertEquals("test query", request.getQuery());
        assertEquals(20, request.getTopK());
        assertEquals(3, request.getMaxGraphDepth());
        assertEquals(8192, request.getMaxTokens());
        assertEquals(0.5, request.getMinSimilarityScore());
        assertTrue(request.isIncludeVectorSearch());
        assertTrue(request.isIncludeKnowledgeGraph());
        assertTrue(request.isOptimizePrompt());
    }
}