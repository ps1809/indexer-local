package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.VectorSearchResult;

import java.util.List;

/**
 * Service interface for semantic vector search across indexed repository chunks.
 * Provides deterministic, ranked semantic retrieval.
 */
public interface VectorSearchService {

    /**
     * Perform semantic search using a text query.
     */
    List<VectorSearchResult> semanticSearch(String repositoryId, String query, int topK, double minScore);

    /**
     * Search for similar code chunks.
     */
    List<VectorSearchResult> similarCodeSearch(String repositoryId, String codeSnippet, int topK, double minScore);

    /**
     * Search for similar documentation chunks.
     */
    List<VectorSearchResult> similarDocumentationSearch(String repositoryId, String query, int topK, double minScore);

    /**
     * Find related symbols for a given symbol.
     */
    List<VectorSearchResult> relatedSymbols(String repositoryId, String symbolName, int topK, double minScore);

    /**
     * Find related classes for a given class.
     */
    List<VectorSearchResult> relatedClasses(String repositoryId, String className, int topK, double minScore);

    /**
     * Find related methods for a given method.
     */
    List<VectorSearchResult> relatedMethods(String repositoryId, String methodName, int topK, double minScore);

    /**
     * Search with filters.
     */
    List<VectorSearchResult> searchWithFilters(String repositoryId, String query, String module,
                                                String packageName, String language, String chunkType,
                                                int topK, double minScore);

    /**
     * Compute cosine similarity between two vectors.
     */
    double cosineSimilarity(double[] vectorA, double[] vectorB);
}