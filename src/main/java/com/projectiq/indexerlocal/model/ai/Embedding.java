package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Represents an embedding vector for a semantic chunk.
 * Stores the vector data with versioning and timestamps.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Embedding {

    private String id;
    private String chunkId;
    private double[] vector;
    private int dimensions;
    private int version;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Embedding() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        this.version = 1;
    }

    public Embedding(String id, String chunkId, double[] vector) {
        this.id = id;
        this.chunkId = chunkId;
        this.vector = vector;
        this.dimensions = vector != null ? vector.length : 0;
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        this.version = 1;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }

    public double[] getVector() { return vector; }
    public void setVector(double[] vector) {
        this.vector = vector;
        this.dimensions = vector != null ? vector.length : 0;
    }

    public int getDimensions() { return dimensions; }
    public void setDimensions(int dimensions) { this.dimensions = dimensions; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    @Override
    public String toString() {
        return "Embedding{" +
                "id='" + id + '\'' +
                ", chunkId='" + chunkId + '\'' +
                ", dimensions=" + dimensions +
                ", version=" + version +
                '}';
    }
}