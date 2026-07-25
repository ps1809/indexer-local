package com.projectiq.indexerlocal.model.context;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request model for building development context.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextRequest {

    private String repositoryId;
    private String query;
    private String symbolName;
    private String symbolType;
    private String packageName;
    private String moduleName;
    private String filePath;
    private String contextType;

    private int expansionDepth = 1;
    private int maxSymbols = 100;
    private int maxFiles = 50;
    private int maxRelationships = 200;
    private int maxTokens = 4096;

    private boolean includeDependencies = true;
    private boolean includeDocumentation = true;
    private boolean includeConfiguration = true;
    private boolean includeRelationships = true;
    private boolean includeSpringBeans = true;
    private boolean includeCallGraph = true;

    public ContextRequest() {
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    public String getSymbolType() {
        return symbolType;
    }

    public void setSymbolType(String symbolType) {
        this.symbolType = symbolType;
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

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public int getExpansionDepth() {
        return expansionDepth;
    }

    public void setExpansionDepth(int expansionDepth) {
        this.expansionDepth = expansionDepth;
    }

    public int getMaxSymbols() {
        return maxSymbols;
    }

    public void setMaxSymbols(int maxSymbols) {
        this.maxSymbols = maxSymbols;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public int getMaxRelationships() {
        return maxRelationships;
    }

    public void setMaxRelationships(int maxRelationships) {
        this.maxRelationships = maxRelationships;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    public boolean isIncludeDocumentation() {
        return includeDocumentation;
    }

    public void setIncludeDocumentation(boolean includeDocumentation) {
        this.includeDocumentation = includeDocumentation;
    }

    public boolean isIncludeConfiguration() {
        return includeConfiguration;
    }

    public void setIncludeConfiguration(boolean includeConfiguration) {
        this.includeConfiguration = includeConfiguration;
    }

    public boolean isIncludeRelationships() {
        return includeRelationships;
    }

    public void setIncludeRelationships(boolean includeRelationships) {
        this.includeRelationships = includeRelationships;
    }

    public boolean isIncludeSpringBeans() {
        return includeSpringBeans;
    }

    public void setIncludeSpringBeans(boolean includeSpringBeans) {
        this.includeSpringBeans = includeSpringBeans;
    }

    public boolean isIncludeCallGraph() {
        return includeCallGraph;
    }

    public void setIncludeCallGraph(boolean includeCallGraph) {
        this.includeCallGraph = includeCallGraph;
    }
}