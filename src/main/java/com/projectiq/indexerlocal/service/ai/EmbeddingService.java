package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.Embedding;

import java.util.List;

/**
 * Service interface for generating and managing embeddings.
 * Supports pluggable embedding providers and incremental updates.
 */
public interface EmbeddingService {

    /**
     * Generate embeddings for all chunks in a repository.
     */
    List<Embedding> generateAllEmbeddings(String repositoryId);

    /**
     * Generate embedding for a single chunk.
     */
    Embedding generateEmbedding(String chunkId);

    /**
     * Generate embeddings for a list of chunks.
     */
    List<Embedding> generateEmbeddings(List<String> chunkIds);

    /**
     * Incrementally update embeddings for a repository.
     * Only generates embeddings for new or changed chunks.
     */
    List<Embedding> incrementalUpdate(String repositoryId);

    /**
     * Clean up embeddings for deleted chunks.
     */
    void cleanupDeletedChunks(String repositoryId);

    /**
     * Get all embeddings for a repository.
     */
    List<Embedding> getEmbeddings(String repositoryId);

    /**
     * Get embedding for a specific chunk.
     */
    Embedding getEmbedding(String chunkId);

    /**
     * Delete embeddings for a repository.
     */
    void deleteEmbeddings(String repositoryId);

    /**
     * Delete embedding for a specific chunk.
     */
    void deleteEmbedding(String chunkId);

    /**
     * Get the embedding vector dimension size.
     */
    int getDimensionSize();
}