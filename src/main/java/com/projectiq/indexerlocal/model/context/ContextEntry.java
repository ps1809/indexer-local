package com.projectiq.indexerlocal.model.context;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single entry in the development context.
 * Each entry contains information about a symbol, file, relationship, or other artifact.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextEntry {

    private String type;
    private String name;
    private String fullyQualifiedName;
    private String packageName;
    private String moduleName;
    private String filePath;
    private String symbolType;
    private String visibility;
    private String description;
    private int priority;
    private int estimatedTokens;
    private String source;

    public ContextEntry() {
    }

    public ContextEntry(String type, String name, String fullyQualifiedName, int priority) {
        this.type = type;
        this.name = name;
        this.fullyQualifiedName = fullyQualifiedName;
        this.priority = priority;
        this.estimatedTokens = estimateTokens(name);
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        return (int) Math.ceil(text.length() / 4.0);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getSymbolType() {
        return symbolType;
    }

    public void setSymbolType(String symbolType) {
        this.symbolType = symbolType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getEstimatedTokens() {
        return estimatedTokens;
    }

    public void setEstimatedTokens(int estimatedTokens) {
        this.estimatedTokens = estimatedTokens;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}