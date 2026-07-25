package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request model for the AI context building pipeline.
 * Supports full retrieval pipeline: symbol search -> vector search -> knowledge graph -> context engine -> prompt optimization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIContextRequest {

    private String repositoryId;
    private String query;
    private String symbolName;
    private String symbolType;
    private String packageName;
    private String moduleName;
    private String filePath;

    private int topK = 10;
    private int maxGraphDepth = 2;
    private int maxTokens = 4096;
    private double minSimilarityScore = 0.0;

    private boolean includeVectorSearch = true;
    private boolean includeKnowledgeGraph = true;
    private boolean includeSymbolSearch = true;
    private boolean includeSpringComponents = true;
    private boolean includeRestApis = true;
    private boolean includeConfiguration = true;
    private boolean includeDocumentation = true;
    private boolean includeRepositorySummary = true;
    private boolean optimizePrompt = true;

    public AIContextRequest() {
    }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getSymbolName() { return symbolName; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }

    public String getSymbolType() { return symbolType; }
    public void setSymbolType(String symbolType) { this.symbolType = symbolType; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public int getMaxGraphDepth() { return maxGraphDepth; }
    public void setMaxGraphDepth(int maxGraphDepth) { this.maxGraphDepth = maxGraphDepth; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public double getMinSimilarityScore() { return minSimilarityScore; }
    public void setMinSimilarityScore(double minSimilarityScore) { this.minSimilarityScore = minSimilarityScore; }

    public boolean isIncludeVectorSearch() { return includeVectorSearch; }
    public void setIncludeVectorSearch(boolean includeVectorSearch) { this.includeVectorSearch = includeVectorSearch; }

    public boolean isIncludeKnowledgeGraph() { return includeKnowledgeGraph; }
    public void setIncludeKnowledgeGraph(boolean includeKnowledgeGraph) { this.includeKnowledgeGraph = includeKnowledgeGraph; }

    public boolean isIncludeSymbolSearch() { return includeSymbolSearch; }
    public void setIncludeSymbolSearch(boolean includeSymbolSearch) { this.includeSymbolSearch = includeSymbolSearch; }

    public boolean isIncludeSpringComponents() { return includeSpringComponents; }
    public void setIncludeSpringComponents(boolean includeSpringComponents) { this.includeSpringComponents = includeSpringComponents; }

    public boolean isIncludeRestApis() { return includeRestApis; }
    public void setIncludeRestApis(boolean includeRestApis) { this.includeRestApis = includeRestApis; }

    public boolean isIncludeConfiguration() { return includeConfiguration; }
    public void setIncludeConfiguration(boolean includeConfiguration) { this.includeConfiguration = includeConfiguration; }

    public boolean isIncludeDocumentation() { return includeDocumentation; }
    public void setIncludeDocumentation(boolean includeDocumentation) { this.includeDocumentation = includeDocumentation; }

    public boolean isIncludeRepositorySummary() { return includeRepositorySummary; }
    public void setIncludeRepositorySummary(boolean includeRepositorySummary) { this.includeRepositorySummary = includeRepositorySummary; }

    public boolean isOptimizePrompt() { return optimizePrompt; }
    public void setOptimizePrompt(boolean optimizePrompt) { this.optimizePrompt = optimizePrompt; }
}