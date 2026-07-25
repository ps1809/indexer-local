package com.projectiq.indexerlocal.service.ai.impl;

import com.projectiq.indexerlocal.model.ai.Chunk;
import com.projectiq.indexerlocal.model.ai.VectorSearchResult;
import com.projectiq.indexerlocal.service.ai.ChunkGenerationService;
import com.projectiq.indexerlocal.service.ai.EmbeddingProvider;
import com.projectiq.indexerlocal.service.ai.EmbeddingService;
import com.projectiq.indexerlocal.service.ai.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of VectorSearchService.
 * Provides deterministic, ranked semantic retrieval using cosine similarity.
 */
@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchServiceImpl.class);

    private final EmbeddingService embeddingService;
    private final EmbeddingProvider embeddingProvider;
    private final ChunkGenerationService chunkGenerationService;

    public VectorSearchServiceImpl(EmbeddingService embeddingService,
                                    EmbeddingProvider embeddingProvider,
                                    ChunkGenerationService chunkGenerationService) {
        this.embeddingService = embeddingService;
        this.embeddingProvider = embeddingProvider;
        this.chunkGenerationService = chunkGenerationService;
    }

    @Override
    public List<VectorSearchResult> semanticSearch(String repositoryId, String query, int topK, double minScore) {
        log.debug("Semantic search: query='{}', topK={}, repositoryId={}", query, topK, repositoryId);

        double[] queryVector = embeddingProvider.generateEmbedding(query);
        var embeddings = embeddingService.getEmbeddings(repositoryId);

        List<VectorSearchResult> results = new ArrayList<>();

        for (var embedding : embeddings) {
            double score = cosineSimilarity(queryVector, embedding.getVector());
            if (score >= minScore) {
                Chunk chunk = chunkGenerationService.getChunk(embedding.getChunkId());
                if (chunk != null) {
                    results.add(new VectorSearchResult(chunk, score, 0));
                }
            }
        }

        // Sort by similarity score descending, then by chunk ID for deterministic ordering
        results.sort((a, b) -> {
            int scoreCmp = Double.compare(b.getSimilarityScore(), a.getSimilarityScore());
            if (scoreCmp != 0) return scoreCmp;
            if (a.getChunk() != null && b.getChunk() != null) {
                return a.getChunk().getId().compareTo(b.getChunk().getId());
            }
            return 0;
        });

        // Assign ranks and limit to topK
        List<VectorSearchResult> rankedResults = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, results.size()); i++) {
            VectorSearchResult result = results.get(i);
            result.setRank(i + 1);
            rankedResults.add(result);
        }

        log.debug("Semantic search returned {} results", rankedResults.size());
        return rankedResults;
    }

    @Override
    public List<VectorSearchResult> similarCodeSearch(String repositoryId, String codeSnippet, int topK, double minScore) {
        // Code search - use the same approach but with code-focused query
        return semanticSearch(repositoryId, codeSnippet, topK, minScore);
    }

    @Override
    public List<VectorSearchResult> similarDocumentationSearch(String repositoryId, String query, int topK, double minScore) {
        var results = semanticSearch(repositoryId, query, Integer.MAX_VALUE, minScore);

        // Filter for documentation chunks only
        results = results.stream()
                .filter(r -> r.getChunk() != null && "DOCUMENTATION".equals(r.getChunk().getChunkType()))
                .limit(topK)
                .collect(Collectors.toList());

        // Re-rank
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        return results;
    }

    @Override
    public List<VectorSearchResult> relatedSymbols(String repositoryId, String symbolName, int topK, double minScore) {
        return semanticSearch(repositoryId, "symbol:" + symbolName, topK, minScore);
    }

    @Override
    public List<VectorSearchResult> relatedClasses(String repositoryId, String className, int topK, double minScore) {
        return semanticSearch(repositoryId, "class:" + className, topK, minScore);
    }

    @Override
    public List<VectorSearchResult> relatedMethods(String repositoryId, String methodName, int topK, double minScore) {
        return semanticSearch(repositoryId, "method:" + methodName, topK, minScore);
    }

    @Override
    public List<VectorSearchResult> searchWithFilters(String repositoryId, String query, String module,
                                                       String packageName, String language, String chunkType,
                                                       int topK, double minScore) {
        log.debug("Filtered search: query='{}', module={}, package={}, language={}, type={}",
                query, module, packageName, language, chunkType);

        var results = semanticSearch(repositoryId, query, Integer.MAX_VALUE, minScore);

        // Apply filters
        results = results.stream()
                .filter(r -> r.getChunk() != null)
                .filter(r -> module == null || module.equals(r.getChunk().getModule()))
                .filter(r -> packageName == null || (r.getChunk().getPackageName() != null &&
                        r.getChunk().getPackageName().contains(packageName)))
                .filter(r -> language == null || language.equals(r.getChunk().getLanguage()))
                .filter(r -> chunkType == null || chunkType.equals(r.getChunk().getChunkType()))
                .limit(topK)
                .collect(Collectors.toList());

        // Re-rank
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        return results;
    }

    @Override
    public double cosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }
}