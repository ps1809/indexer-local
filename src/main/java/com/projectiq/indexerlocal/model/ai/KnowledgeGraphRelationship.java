package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Represents a relationship between two nodes in the repository knowledge graph.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeGraphRelationship {

    private String id;
    private String sourceNodeId;
    private String targetNodeId;
    private String type;
    private int weight;
    private String description;
    private LocalDateTime created;

    public KnowledgeGraphRelationship() {
        this.created = LocalDateTime.now();
        this.weight = 1;
    }

    public KnowledgeGraphRelationship(String id, String sourceNodeId, String targetNodeId, String type) {
        this.id = id;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.type = type;
        this.weight = 1;
        this.created = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }
}