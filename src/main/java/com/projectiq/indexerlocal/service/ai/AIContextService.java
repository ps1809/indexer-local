package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.AIContextRequest;
import com.projectiq.indexerlocal.model.ai.AIContextResult;

/**
 * Service interface for building the final AI retrieval context.
 * Implements the full pipeline: symbol search -> vector search -> knowledge graph -> context engine -> prompt optimization.
 */
public interface AIContextService {

    /**
     * Build complete AI context from a user query.
     * Runs the full pipeline: symbol search -> vector search -> knowledge graph -> context engine -> prompt optimization.
     */
    AIContextResult buildContext(AIContextRequest request);

    /**
     * Build AI context focused on a specific symbol.
     */
    AIContextResult buildSymbolContext(AIContextRequest request);

    /**
     * Build AI context focused on a specific file.
     */
    AIContextResult buildFileContext(AIContextRequest request);

    /**
     * Build AI context focused on a specific package.
     */
    AIContextResult buildPackageContext(AIContextRequest request);

    /**
     * Build AI context focused on a specific module.
     */
    AIContextResult buildModuleContext(AIContextRequest request);

    /**
     * Build AI context for an entire repository.
     */
    AIContextResult buildRepositoryContext(AIContextRequest request);
}