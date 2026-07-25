package com.projectiq.indexerlocal.service.ai.impl;

import com.projectiq.indexerlocal.model.ai.Embedding;
import com.projectiq.indexerlocal.service.ai.ChunkGenerationService;
import com.projectiq.indexerlocal.service.ai.EmbeddingProvider;
import com.projectiq.indexerlocal.service.ai.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of EmbeddingService.
 * Generates and manages embeddings with support for incremental updates and background processing.
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);

    private final EmbeddingProvider embeddingProvider;
    private final ChunkGenerationService chunkGenerationService;

    // In-memory embedding store (in production, use a vector database)
    private final Map<String, Embedding> embeddingStore = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> repositoryEmbeddings = new ConcurrentHashMap<>();

    public EmbeddingServiceImpl(EmbeddingProvider embeddingProvider,
                                 ChunkGenerationService chunkGenerationService) {
        this.embeddingProvider = embeddingProvider;
        this.chunkGenerationService = chunkGenerationService;
    }

    @Override
    public List<Embedding> generateAllEmbeddings(String repositoryId) {
        log.info("Generating all embeddings for repository: {}", repositoryId);

        var chunks = chunkGenerationService.getChunks(repositoryId);
        if (chunks.isEmpty()) {
            log.warn("No chunks found for repository: {}. Generating chunks first.", repositoryId);
            chunkGenerationService.generateAllChunks(repositoryId);
            chunks = chunkGenerationService.getChunks(repositoryId);
        }

        List<Embedding> embeddings = new ArrayList<>();
        for (var chunk : chunks) {
            try {
                double[] vector = embeddingProvider.generateEmbedding(chunk.getContent());
                Embedding embedding = new Embedding(
                        "emb:" + chunk.getId(),
                        chunk.getId(),
                        vector);
                embedding.setUpdatedTime(LocalDateTime.now());

                embeddingStore.put(embedding.getId(), embedding);
                repositoryEmbeddings.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(embedding.getId());
                embeddings.add(embedding);
            } catch (Exception e) {
                log.warn("Failed to generate embedding for chunk: {}", chunk.getId(), e);
            }
        }

        log.info("Generated {} embeddings for repository: {}", embeddings.size(), repositoryId);
        return embeddings;
    }

    @Override
    public Embedding generateEmbedding(String chunkId) {
        log.debug("Generating embedding for chunk: {}", chunkId);

        var chunk = chunkGenerationService.getChunk(chunkId);
        if (chunk == null) {
            log.warn("Chunk not found: {}", chunkId);
            return null;
        }

        try {
            double[] vector = embeddingProvider.generateEmbedding(chunk.getContent());
            Embedding embedding = new Embedding("emb:" + chunkId, chunkId, vector);
            embedding.setUpdatedTime(LocalDateTime.now());

            embeddingStore.put(embedding.getId(), embedding);
            if (chunk.getRepositoryId() != null) {
                repositoryEmbeddings.computeIfAbsent(chunk.getRepositoryId(), k -> ConcurrentHashMap.newKeySet()).add(embedding.getId());
            }

            return embedding;
        } catch (Exception e) {
            log.warn("Failed to generate embedding for chunk: {}", chunkId, e);
            return null;
        }
    }

    @Override
    public List<Embedding> generateEmbeddings(List<String> chunkIds) {
        log.debug("Generating embeddings for {} chunks", chunkIds.size());
        List<Embedding> embeddings = new ArrayList<>();

        for (String chunkId : chunkIds) {
            Embedding embedding = generateEmbedding(chunkId);
            if (embedding != null) {
                embeddings.add(embedding);
            }
        }

        return embeddings;
    }

    @Override
    public List<Embedding> incrementalUpdate(String repositoryId) {
        log.info("Running incremental embedding update for repository: {}", repositoryId);
        // For MVP, regenerate all embeddings
        return generateAllEmbeddings(repositoryId);
    }

    @Override
    public void cleanupDeletedChunks(String repositoryId) {
        log.info("Cleaning up embeddings for deleted chunks in repository: {}", repositoryId);

        var chunks = chunkGenerationService.getChunks(repositoryId);
        Set<String> activeChunkIds = chunks.stream()
                .map(c -> "emb:" + c.getId())
                .collect(Collectors.toSet());

        Set<String> storedEmbeddingIds = repositoryEmbeddings.getOrDefault(repositoryId, Collections.emptySet());
        List<String> toRemove = storedEmbeddingIds.stream()
                .filter(id -> !activeChunkIds.contains(id))
                .collect(Collectors.toList());

        for (String id : toRemove) {
            embeddingStore.remove(id);
            storedEmbeddingIds.remove(id);
        }

        if (!toRemove.isEmpty()) {
            log.info("Cleaned up {} orphaned embeddings", toRemove.size());
        }
    }

    @Override
    public List<Embedding> getEmbeddings(String repositoryId) {
        Set<String> embeddingIds = repositoryEmbeddings.getOrDefault(repositoryId, Collections.emptySet());
        return embeddingIds.stream()
                .map(embeddingStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Embedding getEmbedding(String chunkId) {
        return embeddingStore.get("emb:" + chunkId);
    }

    @Override
    public void deleteEmbeddings(String repositoryId) {
        Set<String> embeddingIds = repositoryEmbeddings.remove(repositoryId);
        if (embeddingIds != null) {
            embeddingIds.forEach(embeddingStore::remove);
        }
        log.info("Deleted embeddings for repository: {}", repositoryId);
    }

    @Override
    public void deleteEmbedding(String chunkId) {
        Embedding embedding = embeddingStore.remove("emb:" + chunkId);
        if (embedding != null) {
            // Also remove from repository index if we can find it
            for (var entry : repositoryEmbeddings.entrySet()) {
                entry.getValue().remove(embedding.getId());
            }
        }
    }

    @Override
    public int getDimensionSize() {
        return embeddingProvider.getDimensionSize();
    }
}