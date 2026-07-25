package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request model for AI-powered semantic search across indexed repositories.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AISearchRequest {

    private String repositoryId;
    private String query;
    private String module;
    private String packageName;
    private String language;
    private String chunkType;
    private String searchType = "semantic";
    private int topK = 10;
    private double minSimilarityScore = 0.0;

    public AISearchRequest() {
    }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public double getMinSimilarityScore() { return minSimilarityScore; }
    public void setMinSimilarityScore(double minSimilarityScore) { this.minSimilarityScore = minSimilarityScore; }
}