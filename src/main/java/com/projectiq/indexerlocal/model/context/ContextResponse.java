package com.projectiq.indexerlocal.model.context;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response model containing the complete development context.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextResponse {

    private String contextType;
    private String repositoryId;
    private String query;
    private String symbolName;
    private String packageName;
    private String moduleName;

    private List<ContextEntry> symbols = new ArrayList<>();
    private List<ContextEntry> files = new ArrayList<>();
    private List<ContextEntry> relationships = new ArrayList<>();
    private List<ContextEntry> dependencies = new ArrayList<>();
    private List<ContextEntry> springComponents = new ArrayList<>();
    private List<ContextEntry> configurations = new ArrayList<>();
    private List<ContextEntry> documentation = new ArrayList<>();
    private List<ContextEntry> modules = new ArrayList<>();

    private int totalEntries;
    private int totalEstimatedTokens;
    private int expansionDepth;
    private boolean optimized;

    private LocalDateTime generatedAt;

    public ContextResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
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

    public List<ContextEntry> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<ContextEntry> symbols) {
        this.symbols = symbols;
    }

    public List<ContextEntry> getFiles() {
        return files;
    }

    public void setFiles(List<ContextEntry> files) {
        this.files = files;
    }

    public List<ContextEntry> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<ContextEntry> relationships) {
        this.relationships = relationships;
    }

    public List<ContextEntry> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ContextEntry> dependencies) {
        this.dependencies = dependencies;
    }

    public List<ContextEntry> getSpringComponents() {
        return springComponents;
    }

    public void setSpringComponents(List<ContextEntry> springComponents) {
        this.springComponents = springComponents;
    }

    public List<ContextEntry> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<ContextEntry> configurations) {
        this.configurations = configurations;
    }

    public List<ContextEntry> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(List<ContextEntry> documentation) {
        this.documentation = documentation;
    }

    public List<ContextEntry> getModules() {
        return modules;
    }

    public void setModules(List<ContextEntry> modules) {
        this.modules = modules;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public int getTotalEstimatedTokens() {
        return totalEstimatedTokens;
    }

    public void setTotalEstimatedTokens(int totalEstimatedTokens) {
        this.totalEstimatedTokens = totalEstimatedTokens;
    }

    public int getExpansionDepth() {
        return expansionDepth;
    }

    public void setExpansionDepth(int expansionDepth) {
        this.expansionDepth = expansionDepth;
    }

    public boolean isOptimized() {
        return optimized;
    }

    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public void calculateTotals() {
        int total = 0;
        int tokens = 0;

        for (ContextEntry e : symbols) { total++; tokens += e.getEstimatedTokens(); }
        for (ContextEntry e : files) { total++; tokens += e.getEstimatedTokens(); }
        for (ContextEntry e : relationships) { total++; tokens += e.getEstimatedTokens(); }
        for (ContextEntry e : dependencies) { total++; tokens += e.getEstimatedTokens(); }
        for (ContextEntry e : springComponents) { total++; tokens += e.getEstimatedTokens(); }
        for (ContextEntry e : configurations) { total++; tokens += e.getEstimatedTokens(); }
        for (ContextEntry e : documentation) { total++; tokens += e.getEstimatedTokens(); }
        for (ContextEntry e : modules) { total++; tokens += e.getEstimatedTokens(); }

        this.totalEntries = total;
        this.totalEstimatedTokens = tokens;
    }
}