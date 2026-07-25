package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request model for knowledge graph operations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeGraphRequest {

    private String repositoryId;
    private String nodeId;
    private String nodeType;
    private String relationshipType;
    private boolean incremental;
    private int maxDepth = 3;

    public KnowledgeGraphRequest() {
        this.incremental = true;
    }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

    public boolean isIncremental() { return incremental; }
    public void setIncremental(boolean incremental) { this.incremental = incremental; }

    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
}