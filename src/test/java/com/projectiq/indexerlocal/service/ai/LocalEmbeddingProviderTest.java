package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.service.ai.impl.LocalEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalEmbeddingProviderTest {

    private EmbeddingProvider embeddingProvider;

    @BeforeEach
    void setUp() {
        embeddingProvider = new LocalEmbeddingProvider();
    }

    @Test
    void testGenerateEmbedding() {
        double[] embedding = embeddingProvider.generateEmbedding("public class TestClass { }");
        assertNotNull(embedding);
        assertTrue(embedding.length > 0);
    }

    @Test
    void testGenerateEmbeddingEmpty() {
        double[] embedding = embeddingProvider.generateEmbedding("");
        assertNotNull(embedding);
        assertTrue(embedding.length > 0);
    }

    @Test
    void testGenerateEmbeddingNull() {
        double[] embedding = embeddingProvider.generateEmbedding(null);
        assertNotNull(embedding);
        assertTrue(embedding.length > 0);
    }

    @Test
    void testDeterministicEmbedding() {
        double[] emb1 = embeddingProvider.generateEmbedding("test text");
        double[] emb2 = embeddingProvider.generateEmbedding("test text");
        assertArrayEquals(emb1, emb2, 0.0001);
    }

    @Test
    void testDifferentInputsDifferentEmbeddings() {
        double[] emb1 = embeddingProvider.generateEmbedding("class A");
        double[] emb2 = embeddingProvider.generateEmbedding("class B");
        boolean allSame = true;
        for (int i = 0; i < emb1.length; i++) {
            if (Math.abs(emb1[i] - emb2[i]) > 0.0001) {
                allSame = false;
                break;
            }
        }
        assertFalse(allSame, "Different inputs should produce different embeddings");
    }

    @Test
    void testDimensionSize() {
        int dims = embeddingProvider.getDimensionSize();
        assertTrue(dims > 0);
        assertEquals(128, dims);
    }

    @Test
    void testProviderName() {
        String name = embeddingProvider.getProviderName();
        assertNotNull(name);
        assertFalse(name.isEmpty());
    }

    @Test
    void testNormalizedEmbedding() {
        double[] embedding = embeddingProvider.generateEmbedding("test");
        double sumSquares = 0;
        for (double v : embedding) {
            sumSquares += v * v;
        }
        double norm = Math.sqrt(sumSquares);
        assertEquals(1.0, norm, 0.01, "Embedding should be normalized to unit length");
    }
}