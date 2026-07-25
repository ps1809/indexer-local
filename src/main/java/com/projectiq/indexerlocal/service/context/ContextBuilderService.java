package com.projectiq.indexerlocal.service.context;

import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;

/**
 * Service interface for building complete development context from search results.
 * Transforms raw repository search results into structured, relevant context
 * using only the indexed database without filesystem scanning.
 */
public interface ContextBuilderService {

    /**
     * Build complete context from a general search query.
     */
    ContextResponse buildContext(ContextRequest request);

    /**
     * Build context focused on a specific symbol.
     */
    ContextResponse buildSymbolContext(ContextRequest request);

    /**
     * Build context focused on a specific file.
     */
    ContextResponse buildFileContext(ContextRequest request);

    /**
     * Build context focused on a specific package.
     */
    ContextResponse buildPackageContext(ContextRequest request);

    /**
     * Build context focused on a specific module.
     */
    ContextResponse buildModuleContext(ContextRequest request);

    /**
     * Build context for an entire repository.
     */
    ContextResponse buildRepositoryContext(ContextRequest request);
}