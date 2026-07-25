package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request model for triggering chunk generation operations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIChunkRequest {

    private String repositoryId;
    private String module;
    private String chunkType;
    private boolean incremental;
    private boolean forceRegenerate;

    public AIChunkRequest() {
        this.incremental = true;
        this.forceRegenerate = false;
    }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }

    public boolean isIncremental() { return incremental; }
    public void setIncremental(boolean incremental) { this.incremental = incremental; }

    public boolean isForceRegenerate() { return forceRegenerate; }
    public void setForceRegenerate(boolean forceRegenerate) { this.forceRegenerate = forceRegenerate; }
}