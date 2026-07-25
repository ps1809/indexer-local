package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the final AI retrieval context built by the pipeline.
 * Contains all information needed for AI agents to understand the repository context.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIContextResult {

    private String query;
    private String repositoryId;
    private String repositorySummary;
    private List<String> relevantFiles = new ArrayList<>();
    private List<Chunk> semanticMatches = new ArrayList<>();
    private List<String> relatedClasses = new ArrayList<>();
    private List<String> relatedMethods = new ArrayList<>();
    private List<String> dependencies = new ArrayList<>();
    private List<String> springComponents = new ArrayList<>();
    private List<String> restApis = new ArrayList<>();
    private List<String> configurations = new ArrayList<>();
    private List<String> documentation = new ArrayList<>();
    private List<KnowledgeGraphNode> graphNodes = new ArrayList<>();
    private List<KnowledgeGraphRelationship> graphRelationships = new ArrayList<>();
    private int totalEntries;
    private int estimatedTokens;
    private LocalDateTime generatedAt;

    public AIContextResult() {
        this.generatedAt = LocalDateTime.now();
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getRepositorySummary() { return repositorySummary; }
    public void setRepositorySummary(String repositorySummary) { this.repositorySummary = repositorySummary; }

    public List<String> getRelevantFiles() { return relevantFiles; }
    public void setRelevantFiles(List<String> relevantFiles) { this.relevantFiles = relevantFiles; }

    public List<Chunk> getSemanticMatches() { return semanticMatches; }
    public void setSemanticMatches(List<Chunk> semanticMatches) { this.semanticMatches = semanticMatches; }

    public List<String> getRelatedClasses() { return relatedClasses; }
    public void setRelatedClasses(List<String> relatedClasses) { this.relatedClasses = relatedClasses; }

    public List<String> getRelatedMethods() { return relatedMethods; }
    public void setRelatedMethods(List<String> relatedMethods) { this.relatedMethods = relatedMethods; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

    public List<String> getSpringComponents() { return springComponents; }
    public void setSpringComponents(List<String> springComponents) { this.springComponents = springComponents; }

    public List<String> getRestApis() { return restApis; }
    public void setRestApis(List<String> restApis) { this.restApis = restApis; }

    public List<String> getConfigurations() { return configurations; }
    public void setConfigurations(List<String> configurations) { this.configurations = configurations; }

    public List<String> getDocumentation() { return documentation; }
    public void setDocumentation(List<String> documentation) { this.documentation = documentation; }

    public List<KnowledgeGraphNode> getGraphNodes() { return graphNodes; }
    public void setGraphNodes(List<KnowledgeGraphNode> graphNodes) { this.graphNodes = graphNodes; }

    public List<KnowledgeGraphRelationship> getGraphRelationships() { return graphRelationships; }
    public void setGraphRelationships(List<KnowledgeGraphRelationship> graphRelationships) { this.graphRelationships = graphRelationships; }

    public int getTotalEntries() { return totalEntries; }
    public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }

    public int getEstimatedTokens() { return estimatedTokens; }
    public void setEstimatedTokens(int estimatedTokens) { this.estimatedTokens = estimatedTokens; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}