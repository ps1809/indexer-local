package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request model for triggering embedding operations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIEmbeddingRequest {

    private String repositoryId;
    private String chunkId;
    private boolean incremental;
    private boolean forceRegenerate;
    private boolean background;

    public AIEmbeddingRequest() {
        this.incremental = true;
        this.forceRegenerate = false;
        this.background = false;
    }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }

    public boolean isIncremental() { return incremental; }
    public void setIncremental(boolean incremental) { this.incremental = incremental; }

    public boolean isForceRegenerate() { return forceRegenerate; }
    public void setForceRegenerate(boolean forceRegenerate) { this.forceRegenerate = forceRegenerate; }

    public boolean isBackground() { return background; }
    public void setBackground(boolean background) { this.background = background; }
}