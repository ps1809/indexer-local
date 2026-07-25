package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified response model for all AI Knowledge Layer operations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIResponse {

    private String operation;
    private String repositoryId;
    private String status;
    private String message;

    private List<Chunk> chunks = new ArrayList<>();
    private List<Embedding> embeddings = new ArrayList<>();
    private List<VectorSearchResult> vectorResults = new ArrayList<>();
    private KnowledgeGraph knowledgeGraph;
    private AIContextResult contextResult;

    private int totalCount;
    private LocalDateTime generatedAt;

    public AIResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    public AIResponse(String operation, String status) {
        this.operation = operation;
        this.status = status;
        this.generatedAt = LocalDateTime.now();
    }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Chunk> getChunks() { return chunks; }
    public void setChunks(List<Chunk> chunks) { this.chunks = chunks; }

    public List<Embedding> getEmbeddings() { return embeddings; }
    public void setEmbeddings(List<Embedding> embeddings) { this.embeddings = embeddings; }

    public List<VectorSearchResult> getVectorResults() { return vectorResults; }
    public void setVectorResults(List<VectorSearchResult> vectorResults) { this.vectorResults = vectorResults; }

    public KnowledgeGraph getKnowledgeGraph() { return knowledgeGraph; }
    public void setKnowledgeGraph(KnowledgeGraph knowledgeGraph) { this.knowledgeGraph = knowledgeGraph; }

    public AIContextResult getContextResult() { return contextResult; }
    public void setContextResult(AIContextResult contextResult) { this.contextResult = contextResult; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}