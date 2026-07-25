package com.projectiq.indexerlocal.service.context;

import com.projectiq.indexerlocal.model.context.ContextEntry;
import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;

import java.util.List;

/**
 * Service interface for smart context expansion.
 * Automatically expands context using indexed relationships
 * with configurable limits and cycle detection.
 */
public interface SmartContextService {

    /**
     * Expand context by following imports.
     */
    void expandImports(ContextResponse context, ContextRequest request);

    /**
     * Expand context by following dependencies.
     */
    void expandDependencies(ContextResponse context, ContextRequest request);

    /**
     * Expand context by traversing the call graph.
     */
    void expandCallGraph(ContextResponse context, ContextRequest request);

    /**
     * Expand context by following inheritance hierarchy.
     */
    void expandInheritance(ContextResponse context, ContextRequest request);

    /**
     * Expand context by resolving interface implementations.
     */
    void expandInterfaces(ContextResponse context, ContextRequest request);

    /**
     * Expand context by finding related Spring beans.
     */
    void expandSpringBeans(ContextResponse context, ContextRequest request);

    /**
     * Expand context by finding related REST endpoints.
     */
    void expandRestEndpoints(ContextResponse context, ContextRequest request);

    /**
     * Expand context by finding related configuration.
     */
    void expandConfiguration(ContextResponse context, ContextRequest request);

    /**
     * Expand context to include related modules.
     */
    void expandModules(ContextResponse context, ContextRequest request);

    /**
     * Apply all expansion rules based on request configuration.
     */
    void expandAll(ContextResponse context, ContextRequest request);
}