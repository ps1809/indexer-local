package com.projectiq.indexerlocal.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a node in the repository knowledge graph.
 * Each node corresponds to a repository artifact like class, method, module, etc.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeGraphNode {

    private String id;
    private String type;
    private String name;
    private String fullyQualifiedName;
    private String repositoryId;
    private String module;
    private String packageName;
    private String filePath;
    private String language;
    private Map<String, Object> properties = new HashMap<>();
    private LocalDateTime created;

    public KnowledgeGraphNode() {
        this.created = LocalDateTime.now();
    }

    public KnowledgeGraphNode(String id, String type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.created = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullyQualifiedName() { return fullyQualifiedName; }
    public void setFullyQualifiedName(String fullyQualifiedName) { this.fullyQualifiedName = fullyQualifiedName; }

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }

    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }

    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }
}