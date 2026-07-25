package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single result from a vector search operation.
 * Contains the matched chunk and its similarity score.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VectorSearchResult {

    private Chunk chunk;
    private double similarityScore;
    private int rank;

    public VectorSearchResult() {
    }

    public VectorSearchResult(Chunk chunk, double similarityScore, int rank) {
        this.chunk = chunk;
        this.similarityScore = similarityScore;
        this.rank = rank;
    }

    public Chunk getChunk() { return chunk; }
    public void setChunk(Chunk chunk) { this.chunk = chunk; }

    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}