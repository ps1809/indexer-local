package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a semantic chunk of repository content for AI processing.
 * Each chunk contains a piece of code or documentation with rich metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Chunk {

    private String id;
    private String repositoryId;
    private String module;
    private String packageName;
    private String symbol;
    private String language;
    private String chunkType;
    private String content;
    private String contentHash;
    private Map<String, String> relationships = new HashMap<>();
    private LocalDateTime lastUpdated;
    private int version;

    public Chunk() {
        this.lastUpdated = LocalDateTime.now();
        this.version = 1;
    }

    public Chunk(String id, String repositoryId, String chunkType, String content) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.chunkType = chunkType;
        this.content = content;
        this.lastUpdated = LocalDateTime.now();
        this.version = 1;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public Map<String, String> getRelationships() { return relationships; }
    public void setRelationships(Map<String, String> relationships) { this.relationships = relationships; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public void addRelationship(String type, String target) {
        this.relationships.put(type, target);
    }
}