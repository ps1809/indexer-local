package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.Chunk;
import com.projectiq.indexerlocal.service.*;
import com.projectiq.indexerlocal.service.ai.impl.ChunkGenerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ChunkGenerationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
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

    private ChunkGenerationService chunkGenerationService;

    @BeforeEach
    void setUp() {
        chunkGenerationService = new ChunkGenerationServiceImpl(
                jdbcTemplate, symbolSearchService, springSearchService,
                relationshipSearchService, buildSearchService, repositorySearchService);
    }

    @Test
    void testGenerateAndGetChunks() {
        // Generate chunks for a test repository
        List<Chunk> chunks = chunkGenerationService.generateAllChunks("test-repo");
        assertNotNull(chunks);

        // Get chunks
        List<Chunk> retrieved = chunkGenerationService.getChunks("test-repo");
        assertNotNull(retrieved);
    }

    @Test
    void testGetChunkById() {
        chunkGenerationService.generateAllChunks("test-repo");
        Chunk chunk = chunkGenerationService.getChunk("class:test-repo:null");
        // May be null if no class data, but should not throw
        assertNotNull(chunkGenerationService.getChunks("test-repo"));
    }

    @Test
    void testDeleteChunks() {
        chunkGenerationService.generateAllChunks("test-repo");
        chunkGenerationService.deleteChunks("test-repo");
        List<Chunk> chunks = chunkGenerationService.getChunks("test-repo");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testDeleteChunk() {
        chunkGenerationService.generateAllChunks("test-repo");
        chunkGenerationService.deleteChunk("nonexistent-chunk");
        // Should not throw
        assertNotNull(chunkGenerationService.getChunks("test-repo"));
    }

    @Test
    void testIncrementalUpdate() {
        List<Chunk> chunks = chunkGenerationService.incrementalUpdate("test-repo");
        assertNotNull(chunks);
    }

    @Test
    void testEmptyRepository() {
        List<Chunk> chunks = chunkGenerationService.getChunks("nonexistent-repo");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testChunkMetadata() {
        Chunk chunk = new Chunk("test-id", "test-repo", "CLASS", "Test content");
        chunk.setSymbol("TestClass");
        chunk.setPackageName("com.test");
        chunk.setModule("test-module");
        chunk.setLanguage("java");

        assertEquals("test-id", chunk.getId());
        assertEquals("test-repo", chunk.getRepositoryId());
        assertEquals("CLASS", chunk.getChunkType());
        assertEquals("Test content", chunk.getContent());
        assertEquals("TestClass", chunk.getSymbol());
        assertEquals("com.test", chunk.getPackageName());
        assertEquals("test-module", chunk.getModule());
        assertEquals("java", chunk.getLanguage());
        assertNotNull(chunk.getLastUpdated());
        assertEquals(1, chunk.getVersion());
    }

    @Test
    void testChunkRelationships() {
        Chunk chunk = new Chunk("test-id", "test-repo", "CLASS", "content");
        chunk.addRelationship("package", "com.test");
        chunk.addRelationship("file", "/path/to/File.java");

        assertTrue(chunk.getRelationships().containsKey("package"));
        assertTrue(chunk.getRelationships().containsKey("file"));
        assertEquals("com.test", chunk.getRelationships().get("package"));
    }
}