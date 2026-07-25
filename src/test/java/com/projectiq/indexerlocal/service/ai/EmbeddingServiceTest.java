package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.Embedding;
import com.projectiq.indexerlocal.service.ai.impl.EmbeddingServiceImpl;
import com.projectiq.indexerlocal.service.ai.impl.LocalEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    private EmbeddingProvider embeddingProvider;
    private ChunkGenerationService chunkGenerationService;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingProvider = new LocalEmbeddingProvider();
        chunkGenerationService = new ChunkGenerationService() {
            private final java.util.Map<String, com.projectiq.indexerlocal.model.ai.Chunk> store = new java.util.concurrent.ConcurrentHashMap<>();
            private final java.util.Map<String, java.util.Set<String>> repoChunks = new java.util.concurrent.ConcurrentHashMap<>();

            @Override
            public List<com.projectiq.indexerlocal.model.ai.Chunk> generateAllChunks(String repositoryId) {
                com.projectiq.indexerlocal.model.ai.Chunk chunk = new com.projectiq.indexerlocal.model.ai.Chunk(
                        "test-chunk-1", repositoryId, "CLASS", "public class TestClass { }");
                chunk.setSymbol("TestClass");
                chunk.setLanguage("java");
                store.put(chunk.getId(), chunk);
                repoChunks.computeIfAbsent(repositoryId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(chunk.getId());

                com.projectiq.indexerlocal.model.ai.Chunk chunk2 = new com.projectiq.indexerlocal.model.ai.Chunk(
                        "test-chunk-2", repositoryId, "METHOD", "public void testMethod() { }");
                chunk2.setSymbol("testMethod");
                chunk2.setLanguage("java");
                store.put(chunk2.getId(), chunk2);
                repoChunks.get(repositoryId).add(chunk2.getId());
                return List.of(chunk, chunk2);
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

        embeddingService = new EmbeddingServiceImpl(embeddingProvider, chunkGenerationService);
    }

    @Test
    void testGenerateAllEmbeddings() {
        List<Embedding> embeddings = embeddingService.generateAllEmbeddings("test-repo");
        assertNotNull(embeddings);
        assertEquals(2, embeddings.size());
    }

    @Test
    void testGenerateEmbedding() {
        chunkGenerationService.generateAllChunks("test-repo");
        Embedding embedding = embeddingService.generateEmbedding("test-chunk-1");
        assertNotNull(embedding);
        assertEquals("test-chunk-1", embedding.getChunkId());
        assertNotNull(embedding.getVector());
        assertTrue(embedding.getDimensions() > 0);
    }

    @Test
    void testGetEmbeddings() {
        embeddingService.generateAllEmbeddings("test-repo");
        List<Embedding> embeddings = embeddingService.getEmbeddings("test-repo");
        assertNotNull(embeddings);
        assertFalse(embeddings.isEmpty());
    }

    @Test
    void testGetEmbedding() {
        embeddingService.generateAllEmbeddings("test-repo");
        Embedding embedding = embeddingService.getEmbedding("test-chunk-1");
        assertNotNull(embedding);
    }

    @Test
    void testDeleteEmbeddings() {
        embeddingService.generateAllEmbeddings("test-repo");
        embeddingService.deleteEmbeddings("test-repo");
        List<Embedding> embeddings = embeddingService.getEmbeddings("test-repo");
        assertTrue(embeddings.isEmpty());
    }

    @Test
    void testDimensionSize() {
        int dims = embeddingService.getDimensionSize();
        assertTrue(dims > 0);
    }

    @Test
    void testEmbeddingVectorProperties() {
        Embedding embedding = new Embedding("test-emb", "test-chunk", new double[]{0.1, 0.2, 0.3});
        assertEquals(3, embedding.getDimensions());
        assertEquals(1, embedding.getVersion());
        assertNotNull(embedding.getCreatedTime());
        assertNotNull(embedding.getUpdatedTime());
    }
}