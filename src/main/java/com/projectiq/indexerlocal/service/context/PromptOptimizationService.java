package com.projectiq.indexerlocal.service.context;

import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;

/**
 * Service interface for prompt optimization.
 * Produces compact, deterministic AI-ready context by removing duplicates,
 * reducing token count, ranking by relevance, and removing noise.
 */
public interface PromptOptimizationService {

    /**
     * Remove duplicate symbols from the context.
     */
    void removeDuplicateSymbols(ContextResponse context);

    /**
     * Remove duplicate dependencies.
     */
    void removeDuplicateDependencies(ContextResponse context);

    /**
     * Remove duplicate relationships.
     */
    void removeDuplicateRelationships(ContextResponse context);

    /**
     * Remove all duplicate entries across all sections.
     */
    void removeAllDuplicates(ContextResponse context);

    /**
     * Reduce token count by removing lower-priority entries.
     */
    void reduceTokens(ContextResponse context, ContextRequest request);

    /**
     * Compress context by merging related entries.
     */
    void compressContext(ContextResponse context);

    /**
     * Rank entries by priority and remove low-priority noise.
     */
    void rankByPriority(ContextResponse context, ContextRequest request);

    /**
     * Remove noise (entries with very low priority or no clear relevance).
     */
    void removeNoise(ContextResponse context, ContextRequest request);

    /**
     * Apply all optimization strategies in sequence.
     */
    ContextResponse optimize(ContextResponse context, ContextRequest request);
}