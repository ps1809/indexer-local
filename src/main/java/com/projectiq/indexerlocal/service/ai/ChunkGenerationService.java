package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.Chunk;

import java.util.List;

/**
 * Service interface for generating AI-ready repository chunks.
 * Transforms indexed repository data into semantic chunks for AI processing.
 */
public interface ChunkGenerationService {

    /**
     * Generate all chunk types for a repository.
     */
    List<Chunk> generateAllChunks(String repositoryId);

    /**
     * Generate chunks for a specific module.
     */
    List<Chunk> generateModuleChunks(String repositoryId, String module);

    /**
     * Generate class chunks from indexed class data.
     */
    List<Chunk> generateClassChunks(String repositoryId);

    /**
     * Generate interface chunks from indexed interface data.
     */
    List<Chunk> generateInterfaceChunks(String repositoryId);

    /**
     * Generate method chunks from indexed method data.
     */
    List<Chunk> generateMethodChunks(String repositoryId);

    /**
     * Generate package chunks from indexed package data.
     */
    List<Chunk> generatePackageChunks(String repositoryId);

    /**
     * Generate module chunks from build metadata.
     */
    List<Chunk> generateModuleMetadataChunks(String repositoryId);

    /**
     * Generate configuration chunks from indexed configuration files.
     */
    List<Chunk> generateConfigurationChunks(String repositoryId);

    /**
     * Generate documentation chunks from indexed documentation.
     */
    List<Chunk> generateDocumentationChunks(String repositoryId);

    /**
     * Generate Spring component chunks from indexed Spring components.
     */
    List<Chunk> generateSpringComponentChunks(String repositoryId);

    /**
     * Generate REST API chunks from indexed REST endpoints.
     */
    List<Chunk> generateRestApiChunks(String repositoryId);

    /**
     * Generate build metadata chunks from indexed build system data.
     */
    List<Chunk> generateBuildMetadataChunks(String repositoryId);

    /**
     * Incrementally update chunks for a repository.
     * Only regenerates chunks for changed or new data.
     */
    List<Chunk> incrementalUpdate(String repositoryId);

    /**
     * Get all chunks for a repository.
     */
    List<Chunk> getChunks(String repositoryId);

    /**
     * Get a specific chunk by ID.
     */
    Chunk getChunk(String chunkId);

    /**
     * Delete chunks for a repository.
     */
    void deleteChunks(String repositoryId);

    /**
     * Delete a specific chunk.
     */
    void deleteChunk(String chunkId);
}