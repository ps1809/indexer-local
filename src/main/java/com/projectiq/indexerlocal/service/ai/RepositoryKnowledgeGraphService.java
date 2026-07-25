package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.KnowledgeGraph;
import com.projectiq.indexerlocal.model.ai.KnowledgeGraphNode;
import com.projectiq.indexerlocal.model.ai.KnowledgeGraphRelationship;

import java.util.List;

/**
 * Service interface for building and traversing the repository knowledge graph.
 * Constructs a graph of all repository artifacts and their relationships.
 */
public interface RepositoryKnowledgeGraphService {

    /**
     * Build the complete knowledge graph for a repository.
     */
    KnowledgeGraph buildGraph(String repositoryId);

    /**
     * Incrementally update the knowledge graph for a repository.
     */
    KnowledgeGraph incrementalUpdate(String repositoryId);

    /**
     * Get the knowledge graph for a repository.
     */
    KnowledgeGraph getGraph(String repositoryId);

    /**
     * Get a specific node by ID.
     */
    KnowledgeGraphNode getNode(String nodeId);

    /**
     * Get all relationships for a specific node.
     */
    List<KnowledgeGraphRelationship> getNodeRelationships(String nodeId);

    /**
     * Traverse the graph from a starting node to a given depth.
     */
    KnowledgeGraph traverseGraph(String repositoryId, String startNodeId, int maxDepth);

    /**
     * Get graph statistics for a repository.
     */
    KnowledgeGraph getGraphStatistics(String repositoryId);

    /**
     * Find all nodes of a specific type.
     */
    List<KnowledgeGraphNode> findNodesByType(String repositoryId, String nodeType);

    /**
     * Find all relationships of a specific type.
     */
    List<KnowledgeGraphRelationship> findRelationshipsByType(String repositoryId, String relationshipType);

    /**
     * Delete the knowledge graph for a repository.
     */
    void deleteGraph(String repositoryId);
}