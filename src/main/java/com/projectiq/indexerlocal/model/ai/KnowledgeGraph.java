package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the complete knowledge graph for a repository.
 * Contains all nodes, relationships, and graph statistics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeGraph {

    private String repositoryId;
    private List<KnowledgeGraphNode> nodes = new ArrayList<>();
    private List<KnowledgeGraphRelationship> relationships = new ArrayList<>();
    private Map<String, Long> nodeTypeCounts = new HashMap<>();
    private Map<String, Long> relationshipTypeCounts = new HashMap<>();
    private long totalNodes;
    private long totalRelationships;
    private LocalDateTime generatedAt;

    public KnowledgeGraph() {
        this.generatedAt = LocalDateTime.now();
    }

    public KnowledgeGraph(String repositoryId) {
        this.repositoryId = repositoryId;
        this.generatedAt = LocalDateTime.now();
    }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public List<KnowledgeGraphNode> getNodes() { return nodes; }
    public void setNodes(List<KnowledgeGraphNode> nodes) { this.nodes = nodes; }

    public List<KnowledgeGraphRelationship> getRelationships() { return relationships; }
    public void setRelationships(List<KnowledgeGraphRelationship> relationships) { this.relationships = relationships; }

    public Map<String, Long> getNodeTypeCounts() { return nodeTypeCounts; }
    public void setNodeTypeCounts(Map<String, Long> nodeTypeCounts) { this.nodeTypeCounts = nodeTypeCounts; }

    public Map<String, Long> getRelationshipTypeCounts() { return relationshipTypeCounts; }
    public void setRelationshipTypeCounts(Map<String, Long> relationshipTypeCounts) { this.relationshipTypeCounts = relationshipTypeCounts; }

    public long getTotalNodes() { return totalNodes; }
    public void setTotalNodes(long totalNodes) { this.totalNodes = totalNodes; }

    public long getTotalRelationships() { return totalRelationships; }
    public void setTotalRelationships(long totalRelationships) { this.totalRelationships = totalRelationships; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public void addNode(KnowledgeGraphNode node) {
        this.nodes.add(node);
        this.nodeTypeCounts.merge(node.getType(), 1L, Long::sum);
        this.totalNodes = this.nodes.size();
    }

    public void addRelationship(KnowledgeGraphRelationship relationship) {
        this.relationships.add(relationship);
        this.relationshipTypeCounts.merge(relationship.getType(), 1L, Long::sum);
        this.totalRelationships = this.relationships.size();
    }

    public void calculateStatistics() {
        this.totalNodes = nodes.size();
        this.totalRelationships = relationships.size();

        nodeTypeCounts.clear();
        for (KnowledgeGraphNode node : nodes) {
            nodeTypeCounts.merge(node.getType(), 1L, Long::sum);
        }

        relationshipTypeCounts.clear();
        for (KnowledgeGraphRelationship rel : relationships) {
            relationshipTypeCounts.merge(rel.getType(), 1L, Long::sum);
        }
    }
}