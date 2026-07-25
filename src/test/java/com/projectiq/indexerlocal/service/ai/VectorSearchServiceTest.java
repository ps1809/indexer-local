package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.VectorSearchResult;
import com.projectiq.indexerlocal.service.ai.impl.LocalEmbeddingProvider;
import com.projectiq.indexerlocal.service.ai.impl.VectorSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VectorSearchServiceTest {

    private VectorSearchService vectorSearchService;
    private EmbeddingService embeddingService;
    private ChunkGenerationService chunkGenerationService;
    private EmbeddingProvider embeddingProvider;

    @BeforeEach
    void setUp() {
        embeddingProvider = new LocalEmbeddingProvider();
        chunkGenerationService = new ChunkGenerationService() {
            private final java.util.Map<String, com.projectiq.indexerlocal.model.ai.Chunk> store = new java.util.concurrent.ConcurrentHashMap<>();
            private final java.util.Map<String, java.util.Set<String>> repoChunks = new java.util.concurrent.ConcurrentHashMap<>();

            @Override
            public List<com.projectiq.indexerlocal.model.ai.Chunk> generateAllChunks(String repositoryId) {
                com.projectiq.indexerlocal.model.ai.Chunk c1 = new com.projectiq.indexerlocal.model.ai.Chunk(
                        "chunk-1", repositoryId, "CLASS", "public class UserService { public void findUser() {} }");
                c1.setSymbol("UserService");
                c1.setLanguage("java");
                store.put(c1.getId(), c1);
                repoChunks.computeIfAbsent(repositoryId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(c1.getId());

                com.projectiq.indexerlocal.model.ai.Chunk c2 = new com.projectiq.indexerlocal.model.ai.Chunk(
                        "chunk-2", repositoryId, "METHOD", "public void processOrder() { }");
                c2.setSymbol("processOrder");
                c2.setLanguage("java");
                store.put(c2.getId(), c2);
                repoChunks.get(repositoryId).add(c2.getId());

                com.projectiq.indexerlocal.model.ai.Chunk c3 = new com.projectiq.indexerlocal.model.ai.Chunk(
                        "chunk-3", repositoryId, "DOCUMENTATION", "README: This is a sample project");
                c3.setSymbol("README");
                c3.setLanguage("markdown");
                store.put(c3.getId(), c3);
                repoChunks.get(repositoryId).add(c3.getId());

                return List.of(c1, c2, c3);
            }

            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateModuleChunks(String repositoryId, String module) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateClassChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateInterfaceChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateMethodChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generatePackageChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateModuleMetadataChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateConfigurationChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateDocumentationChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateSpringComponentChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateRestApiChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> generateBuildMetadataChunks(String repositoryId) { return List.of(); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> incrementalUpdate(String repositoryId) { return generateAllChunks(repositoryId); }
            @Override public List<com.projectiq.indexerlocal.model.ai.Chunk> getChunks(String repositoryId) {
                java.util.Set<String> ids = repoChunks.getOrDefault(repositoryId, java.util.Collections.emptySet());
                return ids.stream().map(store::get).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
            }
            @Override public com.projectiq.indexerlocal.model.ai.Chunk getChunk(String chunkId) { return store.get(chunkId); }
            @Override public void deleteChunks(String repositoryId) {
                java.util.Set<String> ids = repoChunks.remove(repositoryId);
                if (ids != null) ids.forEach(store::remove);
            }
            @Override public void deleteChunk(String chunkId) { store.remove(chunkId); }
        };

        embeddingService = new com.projectiq.indexerlocal.service.ai.impl.EmbeddingServiceImpl(embeddingProvider, chunkGenerationService);
        vectorSearchService = new VectorSearchServiceImpl(embeddingService, embeddingProvider, chunkGenerationService);
    }

    @Test
    void testSemanticSearch() {
        embeddingService.generateAllEmbeddings("test-repo");
        List<VectorSearchResult> results = vectorSearchService.semanticSearch("test-repo", "user service", 5, 0.0);
        assertNotNull(results);
    }

    @Test
    void testCosineSimilarity() {
        double[] vecA = {1.0, 0.0, 0.0};
        double[] vecB = {1.0, 0.0, 0.0};
        double similarity = vectorSearchService.cosineSimilarity(vecA, vecB);
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void testCosineSimilarityOrthogonal() {
        double[] vecA = {1.0, 0.0};
        double[] vecB = {0.0, 1.0};
        double similarity = vectorSearchService.cosineSimilarity(vecA, vecB);
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void testCosineSimilarityNull() {
        double similarity = vectorSearchService.cosineSimilarity(null, new double[]{1.0});
        assertEquals(0.0, similarity);
    }

    @Test
    void testSearchWithFilters() {
        embeddingService.generateAllEmbeddings("test-repo");
        List<VectorSearchResult> results = vectorSearchService.searchWithFilters(
                "test-repo", "user", null, null, "java", null, 5, 0.0);
        assertNotNull(results);
    }

    @Test
    void testEmptyRepositorySearch() {
        List<VectorSearchResult> results = vectorSearchService.semanticSearch("empty-repo", "test", 5, 0.0);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}